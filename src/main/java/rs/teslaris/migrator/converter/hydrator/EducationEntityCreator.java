package rs.teslaris.migrator.converter.hydrator;

import java.util.Objects;
import java.util.Optional;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import rs.teslaris.core.dto.person.involvement.EducationDTO;
import rs.teslaris.core.model.person.Education;
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
 * Resolves the person, the institution's organisation unit and the thesis, then adds the
 * education.
 */
@Component
@RequiredArgsConstructor
public class EducationEntityCreator
    implements EntityCreator<EducationMigrationDTO>, EntityUpdater<EducationMigrationDTO> {

    private final InvolvementService involvementService;

    private final MigrationIdResolver idResolver;

    private final MigrationMergePolicy mergePolicy;

    private final HydratorInstitutionResolver institutionResolver;

    private final MigrationLog migrationLog;


    @Override
    public Integer create(EducationMigrationDTO dto, MigrationRun run) {
        dto.throwIfRejected();

        var personId = idResolver.require(HydratorSource.NAME, MigrationEntityType.PERSON,
            dto.personSourceKey());

        setReferences(dto);

        var created = involvementService.addEducation(personId, dto.education());

        return Objects.isNull(created) ? null : created.getId();
    }

    @Override
    public void update(Integer targetId, EducationMigrationDTO dto, MigrationRun run) {
        dto.throwIfRejected();

        setReferences(dto);

        var merged = mergePolicy.merge(dto.education(),
            involvementService.<Education, EducationDTO>getInvolvement(targetId,
                Education.class),
            new MergeContext(HydratorSource.NAME, MigrationEntityType.PERSON_EDUCATION.name(),
                dto.sourceKey()));

        involvementService.updateEducation(targetId, merged);
    }

    private void setReferences(EducationMigrationDTO dto) {
        dto.thesisSourceKeys().stream()
            .map(key -> idResolver.resolve(HydratorSource.NAME, MigrationEntityType.THESIS, key))
            .flatMap(Optional::stream)
            .findFirst()
            .ifPresent(dto.education()::setThesisId);

        if (Objects.isNull(dto.institution())) {
            return;
        }

        var institution = institutionResolver.resolve(dto.institution());

        if (institution.found()) {
            dto.education().setOrganisationUnitId(institution.organisationUnitId());
            return;
        }

        migrationLog.valueDropped(HydratorSource.NAME,
            MigrationEntityType.PERSON_EDUCATION.name(),
            dto.sourceKey(), HydratorMappingRule.cv(38),
            "organisation unit not linked, institution kept as display name: " +
                institution.reason());
    }
}
