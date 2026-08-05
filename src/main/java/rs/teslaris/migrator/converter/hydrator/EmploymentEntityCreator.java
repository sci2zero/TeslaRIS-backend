package rs.teslaris.migrator.converter.hydrator;

import java.util.Objects;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import rs.teslaris.core.service.interfaces.person.InvolvementService;
import rs.teslaris.migrator.pipeline.EntityCreator;
import rs.teslaris.migrator.service.impl.MigrationIdResolver;
import rs.teslaris.migrator.util.MigrationEntityType;
import rs.teslaris.migrator.util.MigrationException;

/**
 * Creator for dependent entities: resolves the person and organisation unit migrated earlier, then
 * delegates to the core service.
 */
@Component
@RequiredArgsConstructor
public class EmploymentEntityCreator implements EntityCreator<EmploymentMigrationDTO> {

    private final InvolvementService involvementService;

    private final MigrationIdResolver idResolver;


    @Override
    public Integer create(EmploymentMigrationDTO dto, boolean performIndex) {
        var personId = idResolver
            .resolve(HydratorSource.NAME, MigrationEntityType.PERSON, dto.personSourceKey())
            .orElseThrow(() -> new MigrationException(String.format(
                "Person '%s' has not been migrated yet - run the entities pass first.",
                dto.personSourceKey())));

        idResolver
            .resolve(HydratorSource.NAME, MigrationEntityType.ORGANISATION_UNIT,
                dto.institutionSourceKey())
            .ifPresent(organisationUnitId -> dto.employment()
                .setOrganisationUnitId(organisationUnitId));

        var created = involvementService.addEmployment(personId, dto.employment());

        return Objects.isNull(created) ? null : created.getId();
    }
}
