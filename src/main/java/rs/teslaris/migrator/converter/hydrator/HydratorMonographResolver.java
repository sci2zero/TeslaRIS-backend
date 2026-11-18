package rs.teslaris.migrator.converter.hydrator;

import java.time.Instant;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;
import java.util.regex.Pattern;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import rs.teslaris.core.dto.document.MonographDTO;
import rs.teslaris.core.model.document.Monograph;
import rs.teslaris.core.model.document.MonographType;
import rs.teslaris.core.service.interfaces.document.MonographService;
import rs.teslaris.migrator.model.MigrationItemStatus;
import rs.teslaris.migrator.model.MigrationRecordLog;
import rs.teslaris.migrator.model.hydrator.HydratorCVModel;
import rs.teslaris.migrator.pipeline.RunScopedCache;
import rs.teslaris.migrator.repository.MigrationRecordLogRepository;
import rs.teslaris.migrator.util.MigrationEntityType;
import rs.teslaris.migrator.util.MigrationLog;

/**
 * The book of a chapter is only named in the curriculum, so it is created on demand as an edited
 * book stub (CW-001-MAP-000147…149, 000155…157, 000162), the only monograph type core accepts for a
 * chapter.
 * <p>
 * Books are matched by their ISBN (CW-001-MAP-000160), otherwise by title and year. A chapter
 * without either gets a "Not informed" book of its own.
 */
@Component
@RequiredArgsConstructor
public class HydratorMonographResolver implements RunScopedCache {

    private static final String NOT_INFORMED = "Not informed";

    private static final Pattern ISBN =
        Pattern.compile("^(?:(?:\\d[\\ |-]?){9}[\\dX]|(?:\\d[\\ |-]?){13})$");

    private static final Pattern TRAILING_PUNCTUATION = Pattern.compile("[\\p{Punct}\\s]+$");

    private static final int IDENTIFIERS_RULE = 160;

    private final MonographService monographService;

    private final HydratorPublisherResolver publisherResolver;

    private final HydratorOutputConverters converters;

    private final HydratorConversionUtil conversionUtil;

    private final MigrationLog migrationLog;

    private final MigrationRecordLogRepository recordLogRepository;

    private final Map<String, Integer> monographIds = new ConcurrentHashMap<>();


    public Integer resolveOrCreate(HydratorCVModel.Curriculum record,
                                   HydratorCVModel.Output output, boolean performIndex) {
        var isbns = isbns(record, output);

        if (!isbns.isEmpty()) {
            var existing = monographIds.computeIfAbsent("isbn:" + digits(isbns.getFirst()),
                key -> existingByIsbn(record, output, isbns)
                    .or(() -> storedStub(key))
                    .orElseGet(() -> create(key, record, output, isbns, performIndex)));

            if (Objects.nonNull(existing)) {
                return existing;
            }
        }

        var chapter = output.bookChapter();
        var key = isBlank(chapter.bookTitle()) ?
            "output:" + converters.outputKey(record, output) :
            "title:" + TRAILING_PUNCTUATION.matcher(conversionUtil.normalise(chapter.bookTitle()))
                .replaceFirst("") + "|" + Objects.toString(chapter.publicationYear(), "").trim();

        return monographIds.computeIfAbsent(key, k -> storedStub(k)
            .orElseGet(() -> create(k, record, output, List.of(), performIndex)));
    }

    @Override
    public void clearCache() {
        monographIds.clear();
    }

    private List<String> isbns(HydratorCVModel.Curriculum record, HydratorCVModel.Output output) {
        var valid = new ArrayList<String>();

        converters.bookIsbns(output.bookChapter().identifiers()).forEach(isbn -> {
            if (ISBN.matcher(isbn).matches()) {
                valid.add(isbn);
            } else {
                dropped(record, output, "invalid isbn format: '" + isbn + "'");
            }
        });

        return valid;
    }

    /**
     * A monograph already holding the ISBN. One of another type cannot take the chapter, and its
     * ISBN cannot be reused, so the chapter falls back to a book matched by title.
     */
    private Optional<Integer> existingByIsbn(HydratorCVModel.Curriculum record,
                                             HydratorCVModel.Output output, List<String> isbns) {
        for (var isbn : isbns) {
            var monograph = monographService.findMonographByIsbn(isbn, isbn);

            if (Objects.isNull(monograph)) {
                continue;
            }

            if (MonographType.EDITED_BOOK.equals(monograph.getMonographType())) {
                return Optional.of(monograph.getId());
            }

            dropped(record, output, "isbn '" + isbn + "' belongs to monograph " +
                monograph.getId() + " of type " + monograph.getMonographType() +
                ", not an edited book");
            return Optional.empty();
        }

        return Optional.empty();
    }

    private Optional<Integer> storedStub(String key) {
        return recordLogRepository.findTargetEntityId(HydratorSource.NAME,
            MigrationEntityType.MONOGRAPH, key);
    }

    private Integer create(String key, HydratorCVModel.Curriculum record,
                           HydratorCVModel.Output output, List<String> isbns,
                           boolean performIndex) {
        if (!isbns.isEmpty() && existsWithOtherType(isbns)) {
            return null;
        }

        var chapter = output.bookChapter();
        var language = converters.languageOf(record);
        var title = isBlank(chapter.bookTitle()) ? NOT_INFORMED : chapter.bookTitle().trim();

        var dto = new MonographDTO();
        dto.setMonographType(MonographType.EDITED_BOOK);
        dto.setTitle(conversionUtil.multilingualContent(title, language));
        dto.setSubTitle(List.of());
        dto.setDescription(List.of());
        dto.setKeywords(List.of());
        dto.setUris(new HashSet<>());
        dto.setDocumentDate(
            conversionUtil.flexibleDate(converters.yearDate(chapter.publicationYear())));
        dto.setContributions(converters.editorContributions(record, chapter.editors()));
        dto.setPrintISBN(isbns.isEmpty() ? null : isbns.get(0));
        dto.setEisbn(isbns.size() > 1 ? isbns.get(1) : null);
        dto.setVolume(isBlank(chapter.bookVolume()) ? null : chapter.bookVolume().trim());

        if (!isBlank(chapter.bookEdition())) {
            dto.setEdition(conversionUtil.multilingualContent(chapter.bookEdition(), language));
        }

        var languageId = conversionUtil.localeLanguageId(language);
        dto.setLanguageIds(Objects.isNull(languageId) ? new ArrayList<>() :
            new ArrayList<>(List.of(languageId)));

        converters.applyLocation(dto, language, chapter.publicationLocation());
        dto.setPublisherId(publisherResolver.resolveOrCreate(chapter.bookPublisher(), language,
            chapter.publicationLocation()));

        var created = monographService.createMonograph(dto, performIndex);
        migrationLog.stubCreated(HydratorSource.NAME, MigrationEntityType.MONOGRAPH.name(), key,
            created.getId(), title);
        recordLogRepository.record(MigrationRecordLog.builder()
            .source(HydratorSource.NAME)
            .entityType(MigrationEntityType.MONOGRAPH)
            .sourceKey(key)
            .status(MigrationItemStatus.CREATED)
            .targetEntityId(created.getId())
            .processedAt(Instant.now())
            .build());

        return created.getId();
    }

    // The second ISBN may already be on a monograph of another type, which would reject the stub
    private boolean existsWithOtherType(List<String> isbns) {
        return isbns.stream()
            .map(isbn -> monographService.findMonographByIsbn(isbn, isbn))
            .filter(Objects::nonNull)
            .map(Monograph::getMonographType)
            .anyMatch(type -> !MonographType.EDITED_BOOK.equals(type));
    }

    private void dropped(HydratorCVModel.Curriculum record, HydratorCVModel.Output output,
                         String reason) {
        migrationLog.valueDropped(HydratorSource.NAME,
            MigrationEntityType.MONOGRAPH_PUBLICATION.name(), converters.outputKey(record, output),
            HydratorMappingRule.cv(IDENTIFIERS_RULE), reason);
    }

    private String digits(String isbn) {
        return isbn.replaceAll("[^0-9X]", "");
    }

    private boolean isBlank(String value) {
        return Objects.isNull(value) || value.isBlank();
    }
}
