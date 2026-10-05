package rs.teslaris.core.unit.migrator;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.List;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;
import rs.teslaris.core.indexmodel.OrganisationUnitIndex;
import rs.teslaris.core.service.interfaces.institution.OrganisationUnitService;
import rs.teslaris.migrator.converter.hydrator.HydratorInstitutionResolver;
import rs.teslaris.migrator.model.hydrator.HydratorCVModel;

@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
public class HydratorInstitutionResolverTest {

    @Mock
    private OrganisationUnitService organisationUnitService;

    @InjectMocks
    private HydratorInstitutionResolver resolver;


    private static HydratorCVModel.InstitutionIdentifier id(String value, String type) {
        return new HydratorCVModel.InstitutionIdentifier(value, type);
    }

    private static HydratorCVModel.Institution institution(
        HydratorCVModel.InstitutionIdentifier main,
        HydratorCVModel.InstitutionIdentifier... others) {
        return new HydratorCVModel.Institution("Test University", null, main,
            new HydratorCVModel.OtherIdentifiers(others.length, List.of(others)));
    }

    private static OrganisationUnitIndex index(int databaseId) {
        var index = new OrganisationUnitIndex();
        index.setDatabaseId(databaseId);
        return index;
    }

    @Test
    void shouldMatchByMainIdentifier() {
        when(organisationUnitService.findOrganisationUnitByImportId("12345"))
            .thenReturn(index(7));

        var match = resolver.resolve(institution(id("12345", "RINGGOLD")));

        assertTrue(match.found());
        assertEquals(7, match.organisationUnitId());
    }

    @Test
    void shouldFallBackToRorAndSkipUnsupportedTypes() {
        when(organisationUnitService.findOrganisationUnitByImportId("https://ror.org/00example"))
            .thenReturn(index(9));

        var match = resolver.resolve(institution(id("12345", "RINGGOLD"),
            id("0000000000000001", "ISNI"), id("X1", "DGEEC"),
            id("https://ror.org/00example", "ROR")));

        assertEquals(9, match.organisationUnitId());
        verify(organisationUnitService, never()).findOrganisationUnitByImportId("X1");
        verify(organisationUnitService, never())
            .findOrganisationUnitByImportId("0000000000000001");
    }

    @Test
    void shouldReportNotFound() {
        var match = resolver.resolve(institution(id("12345", "RINGGOLD")));

        assertFalse(match.found());
        assertEquals("identifier not found", match.reason());
    }

    @Test
    void shouldReportMissingSupportedIdentifier() {
        var match = resolver.resolve(institution(id(" ", "RINGGOLD"), id("X1", "DGEEC")));

        assertEquals("institution has no supported identifier", match.reason());
        verify(organisationUnitService, never()).findOrganisationUnitByImportId(any());
    }

    @Test
    void shouldLookUpEachIdentifierOnlyOnce() {
        when(organisationUnitService.findOrganisationUnitByImportId("12345"))
            .thenReturn(index(7));
        when(organisationUnitService.findOrganisationUnitByImportId("67890"))
            .thenReturn(null);

        resolver.resolve(institution(id("12345", "RINGGOLD")));
        resolver.resolve(institution(id("12345", "RINGGOLD")));
        resolver.resolve(institution(id("67890", "RINGGOLD")));
        resolver.resolve(institution(id("67890", "RINGGOLD")));

        verify(organisationUnitService, times(1)).findOrganisationUnitByImportId("12345");
        verify(organisationUnitService, times(1)).findOrganisationUnitByImportId("67890");
    }

    @Test
    void shouldLookUpAgainAfterCacheIsCleared() {
        resolver.resolve(institution(id("12345", "RINGGOLD")));
        resolver.clearCache();
        resolver.resolve(institution(id("12345", "RINGGOLD")));

        verify(organisationUnitService, times(2)).findOrganisationUnitByImportId("12345");
    }
}
