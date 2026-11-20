package rs.teslaris.migrator.unit;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.Optional;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import rs.teslaris.core.dto.person.involvement.EmploymentDTO;
import rs.teslaris.core.model.person.Employment;
import rs.teslaris.core.service.interfaces.person.EmploymentPositionService;
import rs.teslaris.core.service.interfaces.person.InvolvementService;
import rs.teslaris.migrator.converter.hydrator.EmploymentEntityCreator;
import rs.teslaris.migrator.converter.hydrator.EmploymentMigrationDTO;
import rs.teslaris.migrator.converter.hydrator.HydratorInstitutionResolver;
import rs.teslaris.migrator.converter.hydrator.HydratorSource;
import rs.teslaris.migrator.model.MigrationRun;
import rs.teslaris.migrator.model.hydrator.HydratorCVModel;
import rs.teslaris.migrator.pipeline.merge.MigrationMergePolicy;
import rs.teslaris.migrator.service.impl.MigrationIdResolver;
import rs.teslaris.migrator.util.InvalidSourceValueException;
import rs.teslaris.migrator.util.MigrationEntityType;
import rs.teslaris.migrator.util.MigrationException;
import rs.teslaris.migrator.util.MigrationLog;

@ExtendWith(MockitoExtension.class)
public class EmploymentEntityCreatorTest {

    private static final MigrationRun RUN = MigrationRun.builder().id("run-1").build();

    private static final HydratorCVModel.Institution INSTITUTION =
        new HydratorCVModel.Institution("Test University", null,
            new HydratorCVModel.InstitutionIdentifier("12345", "RINGGOLD"), null);
    @Mock
    private InvolvementService involvementService;
    @Mock
    private MigrationIdResolver idResolver;
    @Mock
    private HydratorInstitutionResolver institutionResolver;
    @Mock
    private MigrationLog migrationLog;
    @Mock
    private MigrationMergePolicy mergePolicy;
    @Mock
    private EmploymentPositionService employmentPositionService;

    @InjectMocks
    private EmploymentEntityCreator creator;


    @Test
    void shouldAddEmploymentToResolvedPersonAndInstitution() {
        var employment = new EmploymentDTO();
        var created = new Employment();
        created.setId(42);
        when(idResolver.require(HydratorSource.NAME, MigrationEntityType.PERSON, "cv-1"))
            .thenReturn(5);
        when(institutionResolver.resolve(INSTITUTION))
            .thenReturn(new HydratorInstitutionResolver.Match(7, null));
        when(involvementService.addEmployment(5, employment)).thenReturn(created);

        var id = creator.create(
            new EmploymentMigrationDTO("cv-1", "1", INSTITUTION, null, employment, null), RUN);

        assertEquals(42, id);
        assertEquals(7, employment.getOrganisationUnitId());
    }

    @Test
    void shouldFailWhenPersonIsNotMigrated() {
        when(idResolver.require(HydratorSource.NAME, MigrationEntityType.PERSON, "cv-1"))
            .thenThrow(new MigrationException("not migrated"));

        assertThrows(MigrationException.class, () -> creator.create(
            new EmploymentMigrationDTO("cv-1", "1", INSTITUTION, null, new EmploymentDTO(),
                null), RUN));
        verify(involvementService, never()).addEmployment(anyInt(), any());
    }

    @Test
    void shouldFailItemWithInvalidSourceValue() {
        var dto = new EmploymentMigrationDTO("cv-1", "1", INSTITUTION, null, new EmploymentDTO(),
            "invalid date (year='2015', month='2', day='30')");

        assertThrows(InvalidSourceValueException.class, () -> creator.create(dto, RUN));
        verify(involvementService, never()).addEmployment(anyInt(), any());
    }

    @Test
    void shouldKeepDisplayNameAndLogWhenOrganisationUnitIsNotFound() {
        var employment = new EmploymentDTO();
        var created = new Employment();
        created.setId(43);
        when(idResolver.require(HydratorSource.NAME, MigrationEntityType.PERSON, "cv-1"))
            .thenReturn(5);
        when(institutionResolver.resolve(INSTITUTION))
            .thenReturn(new HydratorInstitutionResolver.Match(null, "identifier not found"));
        when(involvementService.addEmployment(5, employment)).thenReturn(created);

        var id = creator.create(
            new EmploymentMigrationDTO("cv-1", "1", INSTITUTION, null, employment, null), RUN);

        assertEquals(43, id);
        assertNull(employment.getOrganisationUnitId());
        verify(migrationLog).valueDropped(HydratorSource.NAME, "PERSON_EMPLOYMENT",
            "cv-1#employment#1", "CW-001-MAP-000039",
            "organisation unit not linked, institution kept as display name: " +
                "identifier not found");
    }

    @Test
    void shouldLinkEmploymentPositionByCode() {
        var employment = new EmploymentDTO();
        when(idResolver.require(HydratorSource.NAME, MigrationEntityType.PERSON, "cv-1"))
            .thenReturn(5);
        when(institutionResolver.resolve(INSTITUTION))
            .thenReturn(new HydratorInstitutionResolver.Match(7, null));
        when(employmentPositionService.findIdBySchemeNameAndCode("ptcris", "T01V01C11"))
            .thenReturn(Optional.of(9));

        creator.create(
            new EmploymentMigrationDTO("cv-1", "1", INSTITUTION, "T01V01C11", employment, null),
            RUN);

        assertEquals(9, employment.getEmploymentPositionId());
    }

    @Test
    void shouldLogWhenEmploymentPositionIsNotFound() {
        var employment = new EmploymentDTO();
        when(idResolver.require(HydratorSource.NAME, MigrationEntityType.PERSON, "cv-1"))
            .thenReturn(5);
        when(institutionResolver.resolve(INSTITUTION))
            .thenReturn(new HydratorInstitutionResolver.Match(7, null));
        when(employmentPositionService.findIdBySchemeNameAndCode("ptcris", "T99"))
            .thenReturn(Optional.empty());

        creator.create(
            new EmploymentMigrationDTO("cv-1", "1", INSTITUTION, "T99", employment, null), RUN);

        assertNull(employment.getEmploymentPositionId());
        verify(migrationLog).valueDropped(HydratorSource.NAME, "PERSON_EMPLOYMENT",
            "cv-1#employment#1", "CW-001-MAP-000039", "employment position not linked: T99");
    }
}
