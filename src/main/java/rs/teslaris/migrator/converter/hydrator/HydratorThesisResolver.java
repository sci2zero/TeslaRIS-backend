package rs.teslaris.migrator.converter.hydrator;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import rs.teslaris.migrator.pipeline.RunScopedCache;
import rs.teslaris.migrator.repository.ThesisLookupRepository;

/**
 * Finds an existing thesis by title and author. The title alone is not enough: one curriculum
 * lists many internship reports of the same title, each by a different student. The author
 * matches on shared name parts, since the same student is often entered with fewer names.
 */
@Component
@RequiredArgsConstructor
public class HydratorThesisResolver implements RunScopedCache {

    private final ThesisLookupRepository thesisLookupRepository;

    private final HydratorConversionUtil conversionUtil;

    // title key -> theses carrying it, with their authors' name parts
    private Map<String, List<Candidate>> thesesByTitle;


    public synchronized Optional<Integer> find(String title, String authorName) {
        var authorTokens = conversionUtil.nameTokens(authorName);

        return index().getOrDefault(conversionUtil.titleKey(title), List.of()).stream()
            .filter(candidate -> conversionUtil.namesMatch(candidate.authorTokens(),
                authorTokens))
            .map(Candidate::thesisId)
            .findFirst();
    }

    // A thesis created in this run, so a later item of the same run finds it
    public synchronized void register(Integer thesisId, String title, String authorName) {
        index().computeIfAbsent(conversionUtil.titleKey(title), key -> new ArrayList<>())
            .add(new Candidate(thesisId, conversionUtil.nameTokens(authorName)));
    }

    @Override
    public synchronized void clearCache() {
        thesesByTitle = null;
    }

    private Map<String, List<Candidate>> index() {
        if (Objects.nonNull(thesesByTitle)) {
            return thesesByTitle;
        }

        var authorTokens = new HashMap<Integer, Set<String>>();
        thesisLookupRepository.findAuthorNames().forEach(row ->
            authorTokens.computeIfAbsent((Integer) row[0], id -> new HashSet<>())
                .addAll(conversionUtil.nameTokens(
                    Objects.toString(row[1], "") + " " + Objects.toString(row[2], ""))));

        thesesByTitle = new HashMap<>();
        thesisLookupRepository.findTitles().forEach(row -> {
            var thesisId = (Integer) row[0];
            thesesByTitle.computeIfAbsent(conversionUtil.titleKey((String) row[1]),
                    key -> new ArrayList<>())
                .add(new Candidate(thesisId, authorTokens.getOrDefault(thesisId, Set.of())));
        });

        return thesesByTitle;
    }

    private record Candidate(Integer thesisId, Set<String> authorTokens) {
    }
}
