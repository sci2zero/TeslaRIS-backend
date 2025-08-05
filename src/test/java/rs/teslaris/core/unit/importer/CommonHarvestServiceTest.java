package rs.teslaris.core.unit.importer;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.when;

import org.junit.jupiter.api.Test;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.springframework.boot.test.context.SpringBootTest;
import rs.teslaris.core.model.institution.OrganisationUnit;
import rs.teslaris.core.model.person.Person;
import rs.teslaris.core.service.interfaces.institution.OrganisationUnitService;
import rs.teslaris.core.service.interfaces.person.PersonService;
import rs.teslaris.importer.dto.OrganisationUnitImportSourceConfigurationDTO;
import rs.teslaris.importer.service.impl.CommonHarvestServiceImpl;
import rs.teslaris.importer.service.interfaces.OrganisationUnitImportSourceConfigurationService;

@SpringBootTest
public class CommonHarvestServiceTest {

    @Mock
    private PersonService personService;

    @Mock
    private OrganisationUnitService organisationUnitService;

    @Mock
    private OrganisationUnitImportSourceConfigurationService
        organisationUnitImportSourceConfigurationService;

    @InjectMocks
    private CommonHarvestServiceImpl commonHarvestService;


    @Test
    void shouldReturnFalseWhenOrganisationUnitIdIsNull() {
        var result = commonHarvestService.canOUEmployeeScanDataSources(null);

        when(organisationUnitImportSourceConfigurationService.readConfigurationForInstitution(
            1)).thenReturn(new OrganisationUnitImportSourceConfigurationDTO(true, true, true));

        assertFalse(result);
    }

    @Test
    void shouldReturnFalseWhenOrganisationUnitHasNullScopusAfid() {
        var ou = new OrganisationUnit();
        ou.setScopusAfid(null);

        when(organisationUnitService.findOne(1)).thenReturn(ou);
        when(organisationUnitImportSourceConfigurationService.readConfigurationForInstitution(
            1)).thenReturn(new OrganisationUnitImportSourceConfigurationDTO(true, false, false));

        var result = commonHarvestService.canOUEmployeeScanDataSources(1);
        assertFalse(result);
    }

    @Test
    void shouldReturnFalseWhenOrganisationUnitHasEmptyScopusAfid() {
        var ou = new OrganisationUnit();
        ou.setScopusAfid("");

        when(organisationUnitService.findOne(2)).thenReturn(ou);
        when(organisationUnitImportSourceConfigurationService.readConfigurationForInstitution(
            2)).thenReturn(new OrganisationUnitImportSourceConfigurationDTO(true, false, false));

        boolean result = commonHarvestService.canOUEmployeeScanDataSources(2);
        assertFalse(result);
    }

    @Test
    void shouldReturnTrueWhenOrganisationUnitHasValidScopusAfid() {
        var ou = new OrganisationUnit();
        ou.setScopusAfid("123456");

        when(organisationUnitService.findOne(3)).thenReturn(ou);
        when(organisationUnitImportSourceConfigurationService.readConfigurationForInstitution(
            3)).thenReturn(new OrganisationUnitImportSourceConfigurationDTO(true, false, false));

        var result = commonHarvestService.canOUEmployeeScanDataSources(3);
        assertTrue(result);
    }

    @Test
    void shouldScanDataSourcesWhenScopusAuthorIdIsNonEmpty() {
        var userId = 1;
        var person = new Person();
        person.setScopusAuthorId("1234");
        person.setId(1);

        when(personService.getPersonIdForUserId(userId)).thenReturn(1);
        when(personService.findOne(1)).thenReturn(person);
        when(organisationUnitImportSourceConfigurationService.readConfigurationForPerson(
            1)).thenReturn(new OrganisationUnitImportSourceConfigurationDTO(true, false, false));

        var response = commonHarvestService.canPersonScanDataSources(userId);

        assertTrue(response);
    }

    @Test
    void shouldReturnFalseWhenScopusAuthorIdIsEmpty() {
        var userId = 1;

        when(personService.getPersonIdForUserId(userId)).thenReturn(1);
        when(personService.findOne(1)).thenReturn(new Person() {{
            setId(1);
        }});
        when(organisationUnitImportSourceConfigurationService.readConfigurationForPerson(
            1)).thenReturn(new OrganisationUnitImportSourceConfigurationDTO(false, true, true));

        var response = commonHarvestService.canPersonScanDataSources(userId);

        assertFalse(response);
    }
}
