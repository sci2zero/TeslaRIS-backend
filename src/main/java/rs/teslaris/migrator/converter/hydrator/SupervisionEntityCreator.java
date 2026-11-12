package rs.teslaris.migrator.converter.hydrator;

import java.util.ArrayList;
import java.util.Objects;
import java.util.Set;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import rs.teslaris.core.dto.document.PersonDocumentContributionDTO;
import rs.teslaris.core.dto.person.PersonNameDTO;
import rs.teslaris.core.model.document.DocumentContributionType;
import rs.teslaris.core.service.interfaces.document.ThesisService;
import rs.teslaris.migrator.model.MigrationRun;
import rs.teslaris.migrator.pipeline.EntityCreator;
import rs.teslaris.migrator.service.impl.MigrationIdResolver;
import rs.teslaris.migrator.util.MigrationEntityType;
import rs.teslaris.migrator.util.MigrationException;
import rs.teslaris.migrator.util.ResolvedToExistingException;

/**
 * Creates the supervised thesis with the curriculum owner as advisor. When a thesis of the same
 * title and student already exists, the owner is added to it as advisor instead - or linked, when
 * it already names them - and the item is reported as resolved.
 */
@Component
@RequiredArgsConstructor
public class SupervisionEntityCreator implements EntityCreator<SupervisionMigrationDTO> {

    private final ThesisService thesisService;

    private final HydratorThesisResolver thesisResolver;

    private final HydratorConversionUtil conversionUtil;

    private final MigrationIdResolver idResolver;


    @Override
    public Integer create(SupervisionMigrationDTO dto, MigrationRun run) {
        dto.throwIfRejected();

        var ownerId = idResolver.require(HydratorSource.NAME, MigrationEntityType.PERSON,
            dto.personSourceKey());
        var advisor = dto.thesis().getContributions().stream()
            .filter(this::isAdvisor)
            .findFirst()
            .orElseThrow(() -> new MigrationException("curriculum owner has no name"));
        advisor.setPersonId(ownerId);

        var title = dto.thesis().getTitle().getFirst().getContent();
        var existing = thesisResolver.find(title, dto.studentName());

        if (existing.isPresent()) {
            throw new ResolvedToExistingException(existing.get(),
                addAdvisor(existing.get(), advisor));
        }

        var created = thesisService.createThesis(dto.thesis(), run.isPerformIndex());
        if (Objects.isNull(created)) {
            return null;
        }

        thesisResolver.register(created.getId(), title, dto.studentName());
        return created.getId();
    }

    private String addAdvisor(Integer thesisId, PersonDocumentContributionDTO advisor) {
        var thesis = thesisService.readThesisSnapshot(thesisId);
        var contributions = new ArrayList<>(thesis.getContributions());

        if (contributions.stream().anyMatch(contribution -> isAdvisor(contribution) &&
            advisor.getPersonId().equals(contribution.getPersonId()))) {
            return "thesis " + thesisId + " already has the owner as advisor";
        }

        var unlinked = contributions.stream()
            .filter(contribution -> isAdvisor(contribution) &&
                Objects.isNull(contribution.getPersonId()) &&
                sameName(contribution.getPersonName(), advisor.getPersonName()))
            .findFirst();

        String message;
        if (unlinked.isPresent()) {
            unlinked.get().setPersonId(advisor.getPersonId());
            message = "owner linked to the advisor of thesis " + thesisId;
        } else {
            advisor.setOrderNumber(contributions.size() + 1);
            contributions.add(advisor);
            message = "owner added as advisor to thesis " + thesisId;
        }

        thesis.setContributions(contributions);
        thesisService.editThesis(thesisId, thesis);

        return message;
    }

    private boolean sameName(PersonNameDTO first, PersonNameDTO second) {
        return conversionUtil.namesMatch(tokens(first), tokens(second));
    }

    private Set<String> tokens(PersonNameDTO name) {
        return Objects.isNull(name) ? Set.of() : conversionUtil.nameTokens(
            Objects.toString(name.getFirstname(), "") + " " +
                Objects.toString(name.getLastname(), ""));
    }

    private boolean isAdvisor(PersonDocumentContributionDTO contribution) {
        return DocumentContributionType.ADVISOR.equals(contribution.getContributionType());
    }
}
