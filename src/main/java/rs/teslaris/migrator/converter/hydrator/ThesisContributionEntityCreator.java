package rs.teslaris.migrator.converter.hydrator;

import java.util.ArrayList;
import java.util.Objects;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import rs.teslaris.core.dto.document.PersonDocumentContributionDTO;
import rs.teslaris.core.model.document.DocumentContributionType;
import rs.teslaris.core.service.interfaces.document.ThesisService;
import rs.teslaris.migrator.model.MigrationRun;
import rs.teslaris.migrator.pipeline.EntityCreator;
import rs.teslaris.migrator.service.impl.MigrationIdResolver;
import rs.teslaris.migrator.util.MigrationEntityType;
import rs.teslaris.migrator.util.MigrationException;
import rs.teslaris.migrator.util.ResolvedToExistingException;

/**
 * Creates a supervised or examined thesis with the curriculum owner in their role. When a thesis
 * of the same title and student already exists, the owner is added to it instead - or linked,
 * when it already names them - and the item is reported as resolved. An owner already on the
 * thesis gets no second role.
 */
@Component
@RequiredArgsConstructor
public class ThesisContributionEntityCreator
    implements EntityCreator<ThesisContributionMigrationDTO> {

    private final ThesisService thesisService;

    private final HydratorThesisResolver thesisResolver;

    private final HydratorConversionUtil conversionUtil;

    private final MigrationIdResolver idResolver;


    @Override
    public Integer create(ThesisContributionMigrationDTO dto, MigrationRun run) {
        dto.throwIfRejected();

        var ownerId = idResolver.require(HydratorSource.NAME, MigrationEntityType.PERSON,
            dto.personSourceKey());
        var owner = dto.thesis().getContributions().stream()
            .filter(contribution -> !isAuthor(contribution))
            .findFirst()
            .orElseThrow(() -> new MigrationException("curriculum owner has no name"));
        owner.setPersonId(ownerId);

        var title = dto.thesis().getTitle().getFirst().getContent();
        var existing = thesisResolver.find(title, dto.studentName());

        if (existing.isPresent()) {
            throw new ResolvedToExistingException(existing.get(),
                addOwner(existing.get(), owner));
        }

        var created = thesisService.createThesis(dto.thesis(), run.isPerformIndex());
        if (Objects.isNull(created)) {
            return null;
        }

        thesisResolver.register(created.getId(), title, dto.studentName());
        return created.getId();
    }

    private String addOwner(Integer thesisId, PersonDocumentContributionDTO owner) {
        var thesis = thesisService.readThesisSnapshot(thesisId);
        var contributions = new ArrayList<>(thesis.getContributions());

        if (contributions.stream()
            .anyMatch(contribution -> owner.getPersonId().equals(contribution.getPersonId()))) {
            return "thesis " + thesisId + " already has the owner";
        }

        var unlinked = contributions.stream()
            .filter(contribution -> !isAuthor(contribution) &&
                Objects.isNull(contribution.getPersonId()) &&
                conversionUtil.namesMatch(contribution.getPersonName(), owner.getPersonName()))
            .findFirst();

        String message;
        if (unlinked.isPresent()) {
            unlinked.get().setPersonId(owner.getPersonId());
            message = "owner linked to their " + unlinked.get().getContributionType() +
                " contribution on thesis " + thesisId;
        } else {
            owner.setOrderNumber(contributions.size() + 1);
            contributions.add(owner);
            message = "owner added as " + owner.getContributionType() + " to thesis " + thesisId;
        }

        thesis.setContributions(contributions);
        thesisService.editThesis(thesisId, thesis);

        return message;
    }

    private boolean isAuthor(PersonDocumentContributionDTO contribution) {
        return DocumentContributionType.AUTHOR.equals(contribution.getContributionType());
    }
}
