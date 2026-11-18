package rs.teslaris.migrator.converter.hydrator;

import java.time.Instant;
import java.time.LocalDate;
import java.time.YearMonth;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;
import java.util.regex.Pattern;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import rs.teslaris.core.dto.commontypes.FlexibleDateDTO;
import rs.teslaris.core.dto.document.ConferenceBasicAdditionDTO;
import rs.teslaris.core.dto.document.ConferenceDTO;
import rs.teslaris.core.dto.document.EventDTO;
import rs.teslaris.core.dto.document.OtherEventDTO;
import rs.teslaris.core.dto.document.ProceedingsDTO;
import rs.teslaris.core.model.document.Conference;
import rs.teslaris.core.model.document.OtherEventType;
import rs.teslaris.core.service.interfaces.commontypes.CountryService;
import rs.teslaris.core.service.interfaces.document.ConferenceService;
import rs.teslaris.core.service.interfaces.document.OtherEventService;
import rs.teslaris.core.service.interfaces.document.ProceedingsService;
import rs.teslaris.migrator.model.MigrationItemStatus;
import rs.teslaris.migrator.model.MigrationRecordLog;
import rs.teslaris.migrator.model.hydrator.HydratorCVModel;
import rs.teslaris.migrator.pipeline.RunScopedCache;
import rs.teslaris.migrator.repository.MigrationRecordLogRepository;
import rs.teslaris.migrator.util.MigrationEntityType;
import rs.teslaris.migrator.util.MigrationLog;

/**
 * The proceedings of a conference paper or abstract, its conference and the event a paper was
 * presented at are only named in the curriculum, so all are created on demand as stubs
 * (CW-001-MAP-000073…083, 000087, 000090, 000372…376, 000380).
 * <p>
 * Proceedings are matched by their ISBN (CW-001-MAP-000085), otherwise by conference and title;
 * conferences by name and year. Missing names fall back to "Not informed" per crosswalk.
 */
@Component
@RequiredArgsConstructor
public class HydratorProceedingsResolver implements RunScopedCache {

    private static final String NOT_INFORMED = "Not informed";

    // Mirrors ProceedingsServiceImpl#setCommonIdentifiers; core rejects anything else
    private static final Pattern ISBN =
        Pattern.compile("^(?:(?:\\d[\\ |-]?){9}[\\dX]|(?:\\d[\\ |-]?){13})$");

    private final ConferenceService conferenceService;

    private final ProceedingsService proceedingsService;

    private final OtherEventService otherEventService;

    private final CountryService countryService;

    private final HydratorOutputConverters converters;

    private final HydratorConversionUtil conversionUtil;

    private final MigrationLog migrationLog;

    private final MigrationRecordLogRepository recordLogRepository;

    private final Map<String, Integer> conferenceIds = new ConcurrentHashMap<>();

    private final Map<String, Container> containers = new ConcurrentHashMap<>();


    public Container resolveOrCreate(HydratorCVModel.Curriculum record,
                                     HydratorCVModel.Output output, boolean performIndex) {
        var source = source(output);
        var language = converters.languageOf(record);
        var isbn = isbn(record, output, source);

        if (Objects.nonNull(isbn)) {
            return containers.computeIfAbsent("isbn:" + isbn,
                key -> Optional.ofNullable(proceedingsService.findProceedingsByIsbn(isbn, isbn))
                    .map(proceedings -> new Container(proceedings.getId(),
                        proceedings.getEvent().getId()))
                    .orElseGet(() -> create(key, record, output, language, isbn, performIndex)));
        }

        var conferenceId = resolveConference(record, output, language, performIndex);

        return containers.computeIfAbsent(
            "conference:" + conferenceId + "|" + conversionUtil.normalise(title(source)),
            key -> storedStub(MigrationEntityType.PROCEEDINGS, key)
                .map(this::container)
                .orElseGet(() -> create(key, record, output, language, null, performIndex)));
    }

    @Override
    public void clearCache() {
        conferenceIds.clear();
        containers.clear();
    }

    private Container create(String key, HydratorCVModel.Curriculum record,
                             HydratorCVModel.Output output, String language, String isbn,
                             boolean performIndex) {
        var source = source(output);
        var dto = new ProceedingsDTO();
        dto.setTitle(conversionUtil.multilingualContent(title(source), language));
        dto.setSubTitle(List.of());
        dto.setDescription(List.of());
        dto.setKeywords(List.of());
        dto.setUris(new HashSet<>());
        dto.setDocumentDate(conversionUtil.flexibleDate(source.proceedingsDate()));
        dto.setContributions(converters.editorContributions(record, source.editors()));
        dto.setEventId(resolveConference(record, output, language, performIndex));
        dto.setPrintISBN(isbn);
        dto.setPublicationSeriesVolume(source.volume());
        dto.setPublicationSeriesIssue(source.issue());

        var languageId = conversionUtil.localeLanguageId(language);
        dto.setLanguageIds(Objects.isNull(languageId) ? new ArrayList<>() :
            new ArrayList<>(List.of(languageId)));

        converters.applyLocation(dto, language, source.publicationLocation());

        var created = proceedingsService.createProceedings(dto, performIndex);
        migrationLog.stubCreated(HydratorSource.NAME, "PROCEEDINGS", key, created.getId(),
            title(source));
        storeStub(MigrationEntityType.PROCEEDINGS, key, created.getId());

        return new Container(created.getId(), dto.getEventId());
    }

    private Container container(Integer proceedingsId) {
        var proceedings = proceedingsService.findProceedingsById(proceedingsId);

        return new Container(proceedings.getId(), proceedings.getEvent().getId());
    }

    private Optional<Integer> storedStub(MigrationEntityType entityType, String key) {
        return recordLogRepository.findTargetEntityId(HydratorSource.NAME, entityType, key);
    }

    private void storeStub(MigrationEntityType entityType, String key, Integer id) {
        recordLogRepository.record(MigrationRecordLog.builder()
            .source(HydratorSource.NAME)
            .entityType(entityType)
            .sourceKey(key)
            .status(MigrationItemStatus.CREATED)
            .targetEntityId(id)
            .processedAt(Instant.now())
            .build());
    }

    // An unnamed conference is the paper's own, so unrelated papers are not merged into one
    private Integer resolveConference(HydratorCVModel.Curriculum record,
                                      HydratorCVModel.Output output, String language,
                                      boolean performIndex) {
        var source = source(output);
        var isNamed = !isBlank(source.conferenceName());
        var name = isNamed ? source.conferenceName().trim() : NOT_INFORMED;
        var date = conversionUtil.flexibleDate(source.conferenceDate());
        var key = isNamed ? "name:" + conversionUtil.normalise(name) + "|" +
            (Objects.isNull(date) ? "" : date.year()) :
            "output:" + converters.outputKey(record, output);

        return conferenceIds.computeIfAbsent(key, k -> storedStub(MigrationEntityType.CONFERENCE, k)
            .orElseGet(() -> {
                var conference = Objects.isNull(date) ?
                    createUndated(name, language) :
                    createDated(name, date, language, source.conferenceLocation(), performIndex);
                migrationLog.stubCreated(HydratorSource.NAME, "CONFERENCE", k,
                    conference.getId(), name);
                storeStub(MigrationEntityType.CONFERENCE, k, conference.getId());

                return conference.getId();
            }));
    }

    private Conference createDated(String name, FlexibleDateDTO date, String language,
                                   HydratorCVModel.PublicationLocation location,
                                   boolean performIndex) {
        var dto = new ConferenceDTO();
        applyEventFields(dto, name, date, language, location);

        return conferenceService.createConference(dto, performIndex);
    }

    /**
     * CW-001-MAP-000090: the event a paper was presented at, one per presentation, named after the
     * paper and held where its conference was. Dated by the presentation, or by the conference when
     * only the presenters are known.
     *
     * @return the event's id, or {@code null} when there is no date to hold it on
     */
    public Integer createPresentationEvent(HydratorCVModel.Curriculum record,
                                           HydratorCVModel.Output output, boolean performIndex) {
        var paper = output.conferencePaper();
        var date = conversionUtil.flexibleDate(converters.presentationDate(paper));

        if (Objects.isNull(date)) {
            return null;
        }

        var dto = new OtherEventDTO();
        dto.setType(OtherEventType.PERFORMANCE);
        applyEventFields(dto, paper.paperTitle().trim(), date, converters.languageOf(record),
            paper.conferenceLocation());

        var created = otherEventService.createOtherEvent(dto, performIndex);
        migrationLog.stubCreated(HydratorSource.NAME, "PERFORMANCE",
            "output:" + converters.outputKey(record, output), created.getId(),
            paper.paperTitle().trim());

        return created.getId();
    }

    // A non-serial event needs both dates; the source has one, so it spans what it leaves open
    private void applyEventFields(EventDTO dto, String name, FlexibleDateDTO date,
                                  String language, HydratorCVModel.PublicationLocation location) {
        dto.setName(conversionUtil.multilingualContent(name, language));
        dto.setNameAbbreviation(List.of());
        dto.setDescription(List.of());
        dto.setKeywords(List.of());
        dto.setPlace(List.of());
        dto.setDisplayOrganizer(new ArrayList<>());
        dto.setContributions(new ArrayList<>());
        dto.setUris(new HashSet<>());
        dto.setResearchAreasId(new HashSet<>());
        dto.setSerialEvent(false);

        if (Objects.isNull(date.month())) {
            dto.setDateFrom(LocalDate.of(date.year(), 1, 1));
            dto.setDateTo(LocalDate.of(date.year(), 12, 31));
        } else if (Objects.isNull(date.day())) {
            var month = YearMonth.of(date.year(), date.month());
            dto.setDateFrom(month.atDay(1));
            dto.setDateTo(month.atEndOfMonth());
        } else {
            dto.setDateFrom(LocalDate.of(date.year(), date.month(), date.day()));
            dto.setDateTo(dto.getDateFrom());
        }

        if (Objects.isNull(location)) {
            return;
        }

        if (!isBlank(location.city())) {
            dto.setPlace(conversionUtil.multilingualContent(location.city(), language));
        }

        if (Objects.nonNull(location.country()) && !isBlank(location.country().code())) {
            countryService.findCountryByCode(location.country().code().trim())
                .ifPresent(country -> dto.setCountryId(country.getId()));
        }
    }

    private Conference createUndated(String name, String language) {
        var dto = new ConferenceBasicAdditionDTO();
        dto.setName(conversionUtil.multilingualContent(name, language));

        return conferenceService.createConference(dto);
    }

    private String isbn(HydratorCVModel.Curriculum record, HydratorCVModel.Output output,
                        Source source) {
        var value = converters.proceedingsIsbn(source.identifiers());

        if (isBlank(value)) {
            return null;
        }

        var isbn = value.trim().toUpperCase(Locale.ROOT);

        if (!ISBN.matcher(isbn).matches()) {
            migrationLog.valueDropped(HydratorSource.NAME, source.entityType().name(),
                converters.outputKey(record, output), HydratorMappingRule.cv(source.isbnRule()),
                "invalid isbn format: '" + isbn + "'");
            return null;
        }

        return isbn;
    }

    private String title(Source source) {
        return isBlank(source.proceedingsTitle()) ? NOT_INFORMED :
            source.proceedingsTitle().trim();
    }

    // CW-001-MAP-000372…376, 000380
    private Source source(HydratorCVModel.Output output) {
        var paper = output.conferencePaper();

        if (Objects.nonNull(paper)) {
            return new Source(paper.conferenceName(), paper.conferenceDate(),
                paper.conferenceLocation(), paper.proceedingsTitle(), paper.conferenceDate(),
                paper.identifiers(), paper.proceedingsEditors(), paper.publicationLocation(), null,
                null, MigrationEntityType.PROCEEDINGS_PUBLICATION, 85);
        }

        var conferenceAbstract = output.conferenceAbstract();

        return new Source(conferenceAbstract.conferenceName(),
            conferenceAbstract.publicationDate(), conferenceAbstract.conferenceLocation(), null,
            conferenceAbstract.publicationDate(), conferenceAbstract.identifiers(), null, null,
            conferenceAbstract.volume(), conferenceAbstract.issue(),
            MigrationEntityType.CONFERENCE_ABSTRACT, 382);
    }

    private boolean isBlank(String value) {
        return Objects.isNull(value) || value.isBlank();
    }

    private record Source(String conferenceName, HydratorCVModel.DateInfo conferenceDate,
                          HydratorCVModel.PublicationLocation conferenceLocation,
                          String proceedingsTitle, HydratorCVModel.DateInfo proceedingsDate,
                          HydratorCVModel.OutputIdentifiers identifiers,
                          HydratorCVModel.OutputAuthors editors,
                          HydratorCVModel.PublicationLocation publicationLocation, String volume,
                          String issue, MigrationEntityType entityType, int isbnRule) {
    }

    // Core takes the paper's event from the DTO, not from its proceedings
    public record Container(Integer proceedingsId, Integer eventId) {
    }
}
