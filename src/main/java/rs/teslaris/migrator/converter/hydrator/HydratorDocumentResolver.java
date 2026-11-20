package rs.teslaris.migrator.converter.hydrator;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import rs.teslaris.core.dto.document.DocumentDTO;
import rs.teslaris.core.model.document.DocumentContributionType;
import rs.teslaris.core.model.document.MonographPublicationType;
import rs.teslaris.core.model.document.PerformanceRelatedOutputType;
import rs.teslaris.core.model.document.ProceedingsPublicationType;
import rs.teslaris.migrator.pipeline.RunScopedCache;
import rs.teslaris.migrator.repository.DocumentLookupRepository;
import rs.teslaris.migrator.util.MigrationEntityType;

/**
 * Finds the same work listed in a co-author's curriculum, where no identifier ties the two
 * together: same kind, title and year, and authors in common. A document the curriculum owner is
 * already linked on is their own repeated entry, which is kept as the source has it.
 */
@Component
@RequiredArgsConstructor
public class HydratorDocumentResolver implements RunScopedCache {

    private static final Map<MigrationEntityType, Kind> KINDS = Map.of(
        MigrationEntityType.JOURNAL_PUBLICATION, new Kind("JournalPublication", null, null),
        MigrationEntityType.PROCEEDINGS_PUBLICATION,
        new Kind("ProceedingsPublication", "proceedingsPublicationType",
            ProceedingsPublicationType.REGULAR_FULL_ARTICLE),
        MigrationEntityType.CONFERENCE_ABSTRACT,
        new Kind("ProceedingsPublication", "proceedingsPublicationType",
            ProceedingsPublicationType.REGULAR_ABSTRACT_ARTICLE),
        MigrationEntityType.CONFERENCE_POSTER,
        new Kind("PerformanceRelatedOutput", "type", PerformanceRelatedOutputType.POSTER),
        MigrationEntityType.PERFORMANCE_RELATED_OUTPUT,
        new Kind("PerformanceRelatedOutput", "type",
            PerformanceRelatedOutputType.NON_RESEARCH_PRESENTATION),
        MigrationEntityType.MONOGRAPH_PUBLICATION,
        new Kind("MonographPublication", "monographPublicationType",
            MonographPublicationType.CHAPTER),
        // Chapter stubs have no authors, so only books with authors of their own match
        MigrationEntityType.MONOGRAPH, new Kind("Monograph", null, null)
    );

    private final DocumentLookupRepository lookupRepository;

    private final HydratorConversionUtil conversionUtil;

    // kind -> title key -> documents carrying it
    private final Map<MigrationEntityType, Map<String, List<Candidate>>> indexes = new HashMap<>();


    public synchronized Optional<Integer> find(MigrationEntityType type, DocumentDTO dto,
                                               Integer ownerId) {
        if (!KINDS.containsKey(type)) {
            return Optional.empty();
        }

        var authors = authors(dto);
        if (authors.isEmpty()) {
            return Optional.empty();
        }

        var year = year(dto);
        var index = index(type);

        return titleKeys(dto).stream()
            .flatMap(key -> index.getOrDefault(key, List.of()).stream())
            .distinct()
            .filter(candidate -> Objects.equals(candidate.year(), year))
            .filter(candidate -> Objects.isNull(ownerId) ||
                candidate.authors().stream().noneMatch(author ->
                    ownerId.equals(author.personId())))
            .filter(candidate -> authorsMatch(candidate.authors(), authors))
            .map(Candidate::documentId)
            .findFirst();
    }

    // A document created in this run, so a later item of the same run finds it
    public synchronized void register(MigrationEntityType type, Integer documentId,
                                      DocumentDTO dto) {
        if (!KINDS.containsKey(type) || Objects.isNull(documentId)) {
            return;
        }

        var candidate = new Candidate(documentId, year(dto), authors(dto));
        var index = index(type);
        titleKeys(dto).forEach(key ->
            index.computeIfAbsent(key, k -> new ArrayList<>()).add(candidate));
    }

    @Override
    public synchronized void clearCache() {
        indexes.clear();
    }

    // Two authors in common, or every author when either side has fewer
    private boolean authorsMatch(List<Author> existing, List<Author> incoming) {
        var common = incoming.stream()
            .filter(author -> existing.stream()
                .anyMatch(other -> conversionUtil.namesMatch(author.tokens(), other.tokens())))
            .count();

        return common > 0 && common >= Math.min(2, Math.min(existing.size(), incoming.size()));
    }

    private Map<String, List<Candidate>> index(MigrationEntityType type) {
        return indexes.computeIfAbsent(type, this::load);
    }

    private Map<String, List<Candidate>> load(MigrationEntityType type) {
        var kind = KINDS.get(type);

        var authors = new HashMap<Integer, List<Author>>();
        lookupRepository.findAuthors(kind.entity(), kind.subtypeAttribute(), kind.subtype())
            .forEach(row -> authors.computeIfAbsent((Integer) row[0], id -> new ArrayList<>())
                .add(new Author(conversionUtil.nameTokens(
                    Objects.toString(row[1], "") + " " + Objects.toString(row[2], "")),
                    (Integer) row[3])));

        var candidates = new LinkedHashMap<Integer, Candidate>();
        var index = new HashMap<String, List<Candidate>>();
        lookupRepository.findTitles(kind.entity(), kind.subtypeAttribute(), kind.subtype())
            .forEach(row -> {
                var documentId = (Integer) row[0];
                var candidate = candidates.computeIfAbsent(documentId,
                    id -> new Candidate(id, (Integer) row[2], authors.getOrDefault(id, List.of())));
                index.computeIfAbsent(conversionUtil.titleKey((String) row[1]),
                    key -> new ArrayList<>()).add(candidate);
            });

        return index;
    }

    private Set<String> titleKeys(DocumentDTO dto) {
        var keys = new LinkedHashSet<String>();

        if (Objects.nonNull(dto.getTitle())) {
            dto.getTitle().stream()
                .map(title -> conversionUtil.titleKey(title.getContent()))
                .filter(key -> !key.isEmpty())
                .forEach(keys::add);
        }

        return keys;
    }

    private Integer year(DocumentDTO dto) {
        return Objects.isNull(dto.getDocumentDate()) ? null : dto.getDocumentDate().year();
    }

    private List<Author> authors(DocumentDTO dto) {
        if (Objects.isNull(dto.getContributions())) {
            return List.of();
        }

        return dto.getContributions().stream()
            .filter(contribution ->
                DocumentContributionType.AUTHOR.equals(contribution.getContributionType()))
            .map(contribution -> new Author(conversionUtil.nameTokens(contribution.getPersonName()),
                contribution.getPersonId()))
            .filter(author -> !author.tokens().isEmpty())
            .toList();
    }

    private record Kind(String entity, String subtypeAttribute, Enum<?> subtype) {
    }

    private record Author(Set<String> tokens, Integer personId) {
    }

    private record Candidate(Integer documentId, Integer year, List<Author> authors) {
    }
}
