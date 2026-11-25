package rs.teslaris.migrator.converter.hydrator;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Objects;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import rs.teslaris.core.dto.document.PersonDocumentContributionDTO;
import rs.teslaris.core.dto.document.ThesisDTO;
import rs.teslaris.core.dto.document.ThesisResponseDTO;
import rs.teslaris.core.model.document.DocumentContributionType;
import rs.teslaris.core.service.interfaces.document.ThesisService;
import rs.teslaris.migrator.model.MigrationRun;
import rs.teslaris.migrator.pipeline.EntityCreator;
import rs.teslaris.migrator.util.ResolvedToExistingException;

/**
 * Creates a thesis from its author's own curriculum, a dissertation or a degree. When a thesis of
 * the same title and author already exists, made from an advisor's or examiner's curriculum, the
 * author's version replaces its fields, the roles already on it are kept, and the item is reported
 * as resolved.
 */
@Component
@RequiredArgsConstructor
public class StudentThesisEntityCreator implements EntityCreator<ThesisDTO> {

    private final ThesisService thesisService;

    private final HydratorThesisResolver thesisResolver;

    private final HydratorConversionUtil conversionUtil;


    @Override
    public Integer create(ThesisDTO dto, MigrationRun run) {
        var title = dto.getTitle().getFirst().getContent();
        var authorName = authorName(dto);

        if (Objects.nonNull(authorName)) {
            var existing = thesisResolver.find(title, authorName);

            if (existing.isPresent()) {
                throw new ResolvedToExistingException(existing.get(),
                    replace(existing.get(), dto));
            }
        }

        var created = thesisService.createThesis(dto, run.isPerformIndex());
        if (Objects.isNull(created)) {
            return null;
        }

        if (Objects.nonNull(authorName)) {
            thesisResolver.register(created.getId(), title, authorName);
        }

        return created.getId();
    }

    private String replace(Integer thesisId, ThesisDTO dto) {
        var existing = thesisService.readThesisSnapshot(thesisId);
        var kept = keepRoles(dto, existing);

        if (Objects.isNull(dto.getOrganisationUnitId())) {
            dto.setOrganisationUnitId(existing.getOrganisationUnitId());
        }

        if (Objects.isNull(dto.getThesisDefenceDate())) {
            dto.setThesisDefenceDate(existing.getThesisDefenceDate());
        }

        if (Objects.isNull(dto.getDocumentDate())) {
            dto.setDocumentDate(existing.getDocumentDate());
        }

        if (Objects.isNull(dto.getScientificArea()) || dto.getScientificArea().isEmpty()) {
            dto.setScientificArea(existing.getScientificArea());
        }

        var uris = new HashSet<>(Objects.requireNonNullElse(dto.getUris(), new HashSet<>()));
        uris.addAll(Objects.requireNonNullElse(existing.getUris(), new HashSet<>()));
        dto.setUris(uris);

        thesisService.editThesis(thesisId, dto);

        return "author's thesis replaced thesis " + thesisId + ", kept " + kept + " roles";
    }

    // The existing advisors and examiners join the author's; one naming the same person as an
    // author's contribution of the same role only lends it the person
    private int keepRoles(ThesisDTO dto, ThesisResponseDTO existing) {
        var contributions = new ArrayList<>(dto.getContributions());
        var kept = 0;

        for (var other : existing.getContributions()) {
            if (isAuthor(other)) {
                continue;
            }

            var same = contributions.stream()
                .filter(contribution -> !isAuthor(contribution) &&
                    other.getContributionType().equals(contribution.getContributionType()) &&
                    samePerson(contribution, other))
                .findFirst();

            if (same.isPresent()) {
                if (Objects.isNull(same.get().getPersonId())) {
                    same.get().setPersonId(other.getPersonId());
                }
                continue;
            }

            other.setOrderNumber(contributions.size() + 1);
            contributions.add(other);
            kept++;
        }

        dto.setContributions(contributions);

        return kept;
    }

    private boolean samePerson(PersonDocumentContributionDTO first,
                               PersonDocumentContributionDTO second) {
        if (Objects.nonNull(first.getPersonId()) && Objects.nonNull(second.getPersonId())) {
            return first.getPersonId().equals(second.getPersonId());
        }

        return conversionUtil.namesMatch(first.getPersonName(), second.getPersonName());
    }

    private String authorName(ThesisDTO dto) {
        return authors(dto.getContributions()).stream()
            .map(PersonDocumentContributionDTO::getPersonName)
            .filter(Objects::nonNull)
            .map(name -> Objects.toString(name.getFirstname(), "") + " " +
                Objects.toString(name.getLastname(), ""))
            .filter(name -> !name.isBlank())
            .findFirst()
            .orElse(null);
    }

    private List<PersonDocumentContributionDTO> authors(
        List<PersonDocumentContributionDTO> contributions) {
        return contributions.stream().filter(this::isAuthor).toList();
    }

    private boolean isAuthor(PersonDocumentContributionDTO contribution) {
        return DocumentContributionType.AUTHOR.equals(contribution.getContributionType());
    }
}
