package rs.teslaris.migrator.converter.hydrator;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.function.BiConsumer;
import java.util.function.Function;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import rs.teslaris.core.dto.document.DocumentDTO;
import rs.teslaris.core.dto.document.PersonDocumentContributionDTO;

/**
 * Brings the people a curriculum links on its copy of a work onto the document that already holds
 * the work: a name-only contribution of the same role and name gets the person, anyone else is
 * appended.
 */
@Component
@RequiredArgsConstructor
public class HydratorContributionLinker {

    private final HydratorConversionUtil conversionUtil;


    public <D extends DocumentDTO> String link(Integer documentId, D incoming,
                                               Function<Integer, ? extends D> reader,
                                               BiConsumer<Integer, D> updater) {
        D existing = reader.apply(documentId);
        var contributions = new ArrayList<>(existing.getContributions());
        var linked = 0;
        var added = 0;

        for (var contribution : incoming.getContributions()) {
            if (Objects.isNull(contribution.getPersonId()) || contributions.stream()
                .anyMatch(other -> contribution.getPersonId().equals(other.getPersonId()))) {
                continue;
            }

            var same = contributions.stream()
                .filter(other -> Objects.isNull(other.getPersonId()) &&
                    Objects.equals(other.getContributionType(),
                        contribution.getContributionType()) &&
                    conversionUtil.namesMatch(other.getPersonName(),
                        contribution.getPersonName()))
                .findFirst();

            if (same.isPresent()) {
                same.get().setPersonId(contribution.getPersonId());
                linked++;
                continue;
            }

            contributions.add(appended(contribution, contributions.size() + 1));
            added++;
        }

        if (linked + added > 0) {
            existing.setContributions(contributions);
            updater.accept(documentId, existing);
        }

        return String.format("linked %d and added %d people on document %d", linked, added,
            documentId);
    }

    /**
     * Unites two contribution lists of one work: a contribution of the same person, or of the same
     * role and name, is the same one (and lends its person when the existing has none); every
     * other incoming one is appended, name-only ones too.
     *
     * @return how many were linked and how many appended
     */
    public String unite(List<PersonDocumentContributionDTO> existing,
                        List<PersonDocumentContributionDTO> incoming) {
        var linked = 0;
        var added = 0;

        for (var contribution : incoming) {
            var same = existing.stream()
                .filter(other -> (Objects.nonNull(contribution.getPersonId()) &&
                    contribution.getPersonId().equals(other.getPersonId())) ||
                    (Objects.equals(other.getContributionType(),
                        contribution.getContributionType()) &&
                        conversionUtil.namesMatch(other.getPersonName(),
                            contribution.getPersonName())))
                .findFirst();

            if (same.isPresent()) {
                if (Objects.isNull(same.get().getPersonId()) &&
                    Objects.nonNull(contribution.getPersonId())) {
                    same.get().setPersonId(contribution.getPersonId());
                    linked++;
                }
                continue;
            }

            existing.add(appended(contribution, existing.size() + 1));
            added++;
        }

        return String.format("linked %d and added %d people", linked, added);
    }

    private PersonDocumentContributionDTO appended(PersonDocumentContributionDTO contribution,
                                                   int orderNumber) {
        contribution.setId(null);
        contribution.setOrderNumber(orderNumber);

        return contribution;
    }
}
