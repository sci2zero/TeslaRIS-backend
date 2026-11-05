package rs.teslaris.migrator.converter.hydrator;

import java.util.Objects;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import rs.teslaris.core.dto.person.involvement.EmploymentDTO;
import rs.teslaris.core.model.person.Employment;
import rs.teslaris.core.service.interfaces.person.InvolvementService;
import rs.teslaris.migrator.model.MigrationRun;
import rs.teslaris.migrator.pipeline.EntityCreator;
import rs.teslaris.migrator.pipeline.EntityUpdater;
import rs.teslaris.migrator.pipeline.merge.MergeContext;
import rs.teslaris.migrator.pipeline.merge.MigrationMergePolicy;
import rs.teslaris.migrator.service.impl.MigrationIdResolver;
import rs.teslaris.migrator.util.MigrationEntityType;
import rs.teslaris.migrator.util.MigrationLog;

/**
 * Resolves the person and the institution's organisation unit, then adds the employment.
 */
@Component
@RequiredArgsConstructor
public class EmploymentEntityCreator
    implements EntityCreator<EmploymentMigrationDTO>, EntityUpdater<EmploymentMigrationDTO> {

    private final InvolvementService involvementService;

    private final MigrationIdResolver idResolver;

    private final MigrationMergePolicy mergePolicy;

    private final HydratorInstitutionResolver institutionResolver;

    private final MigrationLog migrationLog;


    @Override
    public Integer create(EmploymentMigrationDTO dto, MigrationRun run) {
        dto.throwIfRejected();

        var personId = idResolver.require(HydratorSource.NAME, MigrationEntityType.PERSON,
            dto.personSourceKey());

        setOrganisationUnit(dto);

        var created = involvementService.addEmployment(personId, dto.employment());

        return Objects.isNull(created) ? null : created.getId();
    }

    @Override
    public void update(Integer targetId, EmploymentMigrationDTO dto, MigrationRun run) {
        dto.throwIfRejected();

        setOrganisationUnit(dto);

        var merged = mergePolicy.merge(dto.employment(),
            involvementService.<Employment, EmploymentDTO>getInvolvement(targetId,
                Employment.class),
            new MergeContext(HydratorSource.NAME, MigrationEntityType.PERSON_EMPLOYMENT.name(),
                dto.sourceKey()));

        involvementService.updateEmployment(targetId, merged);
    }

    private void setOrganisationUnit(EmploymentMigrationDTO dto) {
        var institution = institutionResolver.resolve(dto.institution());

        if (institution.found()) {
            dto.employment().setOrganisationUnitId(institution.organisationUnitId());
            return;
        }

        migrationLog.valueDropped(HydratorSource.NAME,
            MigrationEntityType.PERSON_EMPLOYMENT.name(),
            dto.sourceKey(), HydratorMappingRule.cv(39),
            "organisation unit not linked, institution kept as display name: " +
                institution.reason());
    }
}
