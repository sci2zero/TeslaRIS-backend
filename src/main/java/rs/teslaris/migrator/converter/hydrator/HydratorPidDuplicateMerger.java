package rs.teslaris.migrator.converter.hydrator;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.function.BiConsumer;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.stream.Collectors;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import rs.teslaris.core.dto.commontypes.MultilingualContentDTO;
import rs.teslaris.core.dto.document.DocumentDTO;
import rs.teslaris.core.service.interfaces.document.DocumentPublicationService;
import rs.teslaris.migrator.util.MigrationEntityType;
import rs.teslaris.migrator.util.MigrationLog;
import rs.teslaris.migrator.util.ResolvedToExistingException;

/**
 * Merges obvious duplicates sharing a PID (Dragan, 2026-11-19). A document holding one of the
 * incoming DOI, Scopus, WoS or OpenAlex ids is the same work when every identifier both carry is
 * equal, a title matches once case, accents, apostrophes and punctuation are ignored, the years and
 * the containers (journal, conference, book) agree. Then empty fields of the existing document are
 * filled from the incoming one, lists are united (authors included) and differing values stay as
 * they are; title, year, container, DOI and handle never change.
 * <p>
 * Otherwise the incoming document keeps its own record, without the ids another document holds.
 */
@Component
@RequiredArgsConstructor
public class HydratorPidDuplicateMerger {

    private final DocumentPublicationService documentPublicationService;

    private final HydratorConversionUtil conversionUtil;

    private final HydratorContributionLinker contributionLinker;

    private final MigrationLog migrationLog;


    /**
     * @return what the PID lookup found, so the caller can skip a second lookup when nothing holds
     * the incoming ids
     * @throws ResolvedToExistingException when the incoming document was merged into another
     */
    public <D extends DocumentDTO> PidOutcome resolve(MigrationEntityType type, String key,
                                                      D incoming,
                                                      Function<Integer, ? extends D> reader,
                                                      BiConsumer<Integer, D> updater,
                                                      TypeRule<D> rule) {
        var owner = documentPublicationService.findDocumentByCommonIdentifier(incoming.getDoi(),
            incoming.getOpenAlexId(), incoming.getScopusId(), incoming.getWebOfScienceId());

        if (owner.isEmpty()) {
            return PidOutcome.UNUSED;
        }

        var documentId = owner.get().getId();
        D existing;

        try {
            existing = reader.apply(documentId);
        } catch (Exception e) {
            // A document of another type; the duplicate failure handler decides
            return PidOutcome.HELD;
        }

        var differences = differences(existing, incoming);
        rule.containerDifference(existing).ifPresent(differences::add);

        if (!differences.isEmpty()) {
            dropHeldIdentifiers(existing, incoming);
            migrationLog.valueDropped(HydratorSource.NAME, type.name(), key,
                HydratorMappingRule.cvWithoutRule(), "identifier held by document " + documentId +
                    ", not the same work (different " + String.join(", ", differences) +
                    "), created without that identifier");
            return PidOutcome.HELD;
        }

        var filler = new Filler();
        mergeCommon(existing, incoming, filler);
        rule.fill(existing, incoming, filler);
        updater.accept(documentId, existing);

        throw new ResolvedToExistingException(documentId,
            "same work by PID, merged into document " + documentId + ": " + filler.summary());
    }

    /**
     * Compares a container the existing document is in with the one the source names. Not
     * comparable (nothing named on either side) counts as the same.
     */
    public Optional<String> containerDifference(String label,
                                                Collection<MultilingualContentDTO> existingNames,
                                                Collection<String> existingCodes,
                                                String sourceName,
                                                Collection<String> sourceCodes) {
        var codes = codes(existingCodes);
        var incomingCodes = codes(sourceCodes);

        if (!codes.isEmpty() && !incomingCodes.isEmpty() &&
            codes.stream().anyMatch(incomingCodes::contains)) {
            return Optional.empty();
        }

        var names = Objects.requireNonNullElse(existingNames, List.<MultilingualContentDTO>of())
            .stream()
            .map(MultilingualContentDTO::getContent)
            .map(this::containerKey)
            .filter(name -> !name.isEmpty())
            .collect(Collectors.toSet());
        var name = containerKey(sourceName);

        if (names.isEmpty() || name.isEmpty()) {
            return codes.isEmpty() || incomingCodes.isEmpty() ? Optional.empty() :
                Optional.of(label);
        }

        return names.contains(name) ? Optional.empty() : Optional.of(label);
    }

    private List<String> differences(DocumentDTO existing, DocumentDTO incoming) {
        var differences = new ArrayList<String>();

        identifierDifference(differences, "doi", existing.getDoi(), incoming.getDoi());
        identifierDifference(differences, "scopus", existing.getScopusId(),
            incoming.getScopusId());
        identifierDifference(differences, "openAlex", existing.getOpenAlexId(),
            incoming.getOpenAlexId());
        identifierDifference(differences, "wos", existing.getWebOfScienceId(),
            incoming.getWebOfScienceId());
        identifierDifference(differences, "handle", existing.getHandleId(),
            incoming.getHandleId());
        identifierDifference(differences, "pubmed", existing.getPubmedId(),
            incoming.getPubmedId());
        identifierDifference(differences, "arxiv", existing.getArxivId(), incoming.getArxivId());
        identifierDifference(differences, "ssrn", existing.getSsrnId(), incoming.getSsrnId());

        var titles = titleKeys(existing);
        if (titleKeys(incoming).stream().noneMatch(titles::contains)) {
            differences.add("title");
        }

        var year = year(existing);
        var incomingYear = year(incoming);
        if (Objects.nonNull(year) && Objects.nonNull(incomingYear) && !year.equals(incomingYear)) {
            differences.add("year (" + year + " vs " + incomingYear + ")");
        }

        return differences;
    }

    private void identifierDifference(List<String> differences, String label, String existing,
                                      String incoming) {
        if (!isBlank(existing) && !isBlank(incoming) &&
            !identifierKey(existing).equals(identifierKey(incoming))) {
            differences.add(label);
        }
    }

    // The ids the existing document holds are left to it
    private void dropHeldIdentifiers(DocumentDTO existing, DocumentDTO incoming) {
        if (sameIdentifier(existing.getDoi(), incoming.getDoi())) {
            incoming.setDoi(null);
        }

        if (sameIdentifier(existing.getScopusId(), incoming.getScopusId())) {
            incoming.setScopusId(null);
        }

        if (sameIdentifier(existing.getOpenAlexId(), incoming.getOpenAlexId())) {
            incoming.setOpenAlexId(null);
        }

        if (sameIdentifier(existing.getWebOfScienceId(), incoming.getWebOfScienceId())) {
            incoming.setWebOfScienceId(null);
        }
    }

    private void mergeCommon(DocumentDTO existing, DocumentDTO incoming, Filler filler) {
        filler.value("scopus", existing.getScopusId(), incoming.getScopusId(),
            existing::setScopusId);
        filler.value("openAlex", existing.getOpenAlexId(), incoming.getOpenAlexId(),
            existing::setOpenAlexId);
        filler.value("wos", existing.getWebOfScienceId(), incoming.getWebOfScienceId(),
            existing::setWebOfScienceId);
        filler.value("pubmed", existing.getPubmedId(), incoming.getPubmedId(),
            existing::setPubmedId);
        filler.value("arxiv", existing.getArxivId(), incoming.getArxivId(),
            existing::setArxivId);
        filler.value("ssrn", existing.getSsrnId(), incoming.getSsrnId(), existing::setSsrnId);
        filler.value("nationalId", existing.getNationalId(), incoming.getNationalId(),
            existing::setNationalId);
        filler.value("documentDate", existing.getDocumentDate(), incoming.getDocumentDate(),
            existing::setDocumentDate);
        filler.value("publicationStatus", existing.getPublicationStatus(),
            incoming.getPublicationStatus(), existing::setPublicationStatus);
        filler.value("country", existing.getCountryId(), incoming.getCountryId(),
            existing::setCountryId);
        filler.value("note", existing.getNote(), incoming.getNote(), existing::setNote);

        filler.languages("subtitle", existing.getSubTitle(), incoming.getSubTitle(),
            existing::setSubTitle);
        filler.languages("description", existing.getDescription(), incoming.getDescription(),
            existing::setDescription);
        filler.languages("city", existing.getCity(), incoming.getCity(), existing::setCity);
        filler.languages("edition", existing.getEdition(), incoming.getEdition(),
            existing::setEdition);
        filler.languages("remark", existing.getRemark(), incoming.getRemark(),
            existing::setRemark);
        filler.keywords(existing, incoming);

        var uris = new LinkedHashSet<>(Objects.requireNonNullElse(existing.getUris(), Set.of()));
        if (uris.addAll(Objects.requireNonNullElse(incoming.getUris(), Set.<String>of()))) {
            existing.setUris(uris);
            filler.changed("uris");
        }

        var contributions = new ArrayList<>(existing.getContributions());
        filler.people(contributionLinker.unite(contributions, incoming.getContributions()));
        existing.setContributions(contributions);
    }

    private Set<String> titleKeys(DocumentDTO dto) {
        return Objects.requireNonNullElse(dto.getTitle(), List.<MultilingualContentDTO>of())
            .stream()
            .map(title -> conversionUtil.titleKey(title.getContent()))
            .filter(key -> !key.isEmpty())
            .collect(Collectors.toSet());
    }

    private Integer year(DocumentDTO dto) {
        return Objects.isNull(dto.getDocumentDate()) ? null : dto.getDocumentDate().year();
    }

    // "The Journal of X" and "Journal of X" name one journal
    private String containerKey(String name) {
        return conversionUtil.titleKey(name).replaceFirst("^the", "");
    }

    private Set<String> codes(Collection<String> values) {
        return Objects.requireNonNullElse(values, List.<String>of()).stream()
            .filter(value -> !isBlank(value))
            .map(value -> value.toUpperCase().replaceAll("[^0-9X]", ""))
            .filter(value -> !value.isEmpty())
            .collect(Collectors.toSet());
    }

    private boolean sameIdentifier(String existing, String incoming) {
        return !isBlank(existing) && !isBlank(incoming) &&
            identifierKey(existing).equals(identifierKey(incoming));
    }

    private String identifierKey(String value) {
        return value.trim().toLowerCase().replaceFirst("^https?://(dx\\.)?doi\\.org/", "");
    }

    private boolean isBlank(String value) {
        return Objects.isNull(value) || value.isBlank();
    }

    /**
     * UNUSED: no document holds the incoming ids. HELD: one does and was not merged into (another
     * type, or not the same work), so the ids still need the usual collision handling.
     */
    public enum PidOutcome {
        UNUSED,
        HELD
    }

    /**
     * What a document type adds to the shared checks: its container and its own fields.
     */
    public interface TypeRule<D extends DocumentDTO> {

        static <D extends DocumentDTO> TypeRule<D> none() {
            return new TypeRule<>() {
                @Override
                public Optional<String> containerDifference(D existing) {
                    return Optional.empty();
                }

                @Override
                public void fill(D existing, D incoming, Filler filler) {
                }
            };
        }

        Optional<String> containerDifference(D existing);

        void fill(D existing, D incoming, Filler filler);
    }

    /**
     * Fills what the existing document lacks and records what it filled.
     */
    public static final class Filler {

        private final List<String> changes = new ArrayList<>();

        private String people = "";

        public <T> void value(String field, T current, T incoming, Consumer<T> setter) {
            if (isEmpty(current) && !isEmpty(incoming)) {
                setter.accept(incoming);
                changes.add(field);
            }
        }

        // Languages the existing value lacks; a language both have keeps the existing content
        public void languages(String field, List<MultilingualContentDTO> current,
                              List<MultilingualContentDTO> incoming,
                              Consumer<List<MultilingualContentDTO>> setter) {
            var merged = new ArrayList<>(Objects.requireNonNullElse(current, List.of()));
            var languages = merged.stream()
                .map(MultilingualContentDTO::getLanguageTagId)
                .collect(Collectors.toCollection(HashSet::new));
            var added = false;

            for (var content : Objects.requireNonNullElse(incoming,
                List.<MultilingualContentDTO>of())) {
                if (Objects.nonNull(content.getContent()) && !content.getContent().isBlank() &&
                    languages.add(content.getLanguageTagId())) {
                    merged.add(content);
                    added = true;
                }
            }

            if (added) {
                setter.accept(merged);
                changes.add(field);
            }
        }

        public <T> void union(String field, Collection<T> current, Collection<T> incoming,
                              Consumer<List<T>> setter) {
            var merged = new LinkedHashSet<>(Objects.requireNonNullElse(current, List.of()));

            if (merged.addAll(Objects.requireNonNullElse(incoming, List.of()))) {
                setter.accept(new ArrayList<>(merged));
                changes.add(field);
            }
        }

        public void changed(String field) {
            changes.add(field);
        }

        // Keywords are one line per keyword in a content per language: the lines are united
        void keywords(DocumentDTO existing, DocumentDTO incoming) {
            var merged = new ArrayList<>(Objects.requireNonNullElse(existing.getKeywords(),
                List.<MultilingualContentDTO>of()));
            var changed = false;

            for (var content : Objects.requireNonNullElse(incoming.getKeywords(),
                List.<MultilingualContentDTO>of())) {
                if (Objects.isNull(content.getContent()) || content.getContent().isBlank()) {
                    continue;
                }

                var same = merged.stream()
                    .filter(other -> Objects.equals(other.getLanguageTagId(),
                        content.getLanguageTagId()))
                    .findFirst();

                if (same.isEmpty()) {
                    merged.add(content);
                    changed = true;
                    continue;
                }

                var lines = new LinkedHashSet<>(lines(same.get().getContent()));
                if (lines.addAll(lines(content.getContent()))) {
                    same.get().setContent(String.join("\n", lines));
                    changed = true;
                }
            }

            if (changed) {
                existing.setKeywords(merged);
                changes.add("keywords");
            }
        }

        void people(String summary) {
            people = summary;
        }

        String summary() {
            return (changes.isEmpty() ? "no empty fields filled" :
                "filled " + String.join(", ", changes)) + "; " + people;
        }

        private List<String> lines(String content) {
            return Objects.isNull(content) ? List.of() : content.lines()
                .map(String::trim)
                .filter(line -> !line.isEmpty())
                .toList();
        }

        private boolean isEmpty(Object value) {
            return Objects.isNull(value) ||
                (value instanceof String text && text.isBlank()) ||
                (value instanceof Collection<?> collection && collection.isEmpty());
        }
    }
}
