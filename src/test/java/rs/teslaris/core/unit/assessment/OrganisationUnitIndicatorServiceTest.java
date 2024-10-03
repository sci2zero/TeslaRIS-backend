package rs.teslaris.core.unit.assessment;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.mockito.Mockito.when;

import java.util.List;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.springframework.boot.test.context.SpringBootTest;
import rs.teslaris.core.assessment.model.Indicator;
import rs.teslaris.core.assessment.model.IndicatorAccessLevel;
import rs.teslaris.core.assessment.model.OrganisationUnitIndicator;
import rs.teslaris.core.assessment.repository.OrganisationUnitIndicatorRepository;
import rs.teslaris.core.assessment.service.impl.OrganisationUnitIndicatorServiceImpl;

@SpringBootTest
public class OrganisationUnitIndicatorServiceTest {

    @Mock
    private OrganisationUnitIndicatorRepository organisationUnitIndicatorRepository;

    @InjectMocks
    private OrganisationUnitIndicatorServiceImpl organisationUnitIndicatorService;


    @ParameterizedTest
    @EnumSource(value = IndicatorAccessLevel.class, names = {"OPEN", "CLOSED", "ADMIN_ONLY"})
    void shouldReadAllOrganisationUnitIndicatorsForOrganisationUnit(
        IndicatorAccessLevel accessLevel) {
        // Given
        var organisationUnitId = 1;

        var indicator = new Indicator();
        indicator.setAccessLevel(IndicatorAccessLevel.OPEN);

        var organisationUnitIndicator1 = new OrganisationUnitIndicator();
        organisationUnitIndicator1.setNumericValue(12d);
        organisationUnitIndicator1.setIndicator(indicator);

        var organisationUnitIndicator2 = new OrganisationUnitIndicator();
        organisationUnitIndicator2.setNumericValue(11d);
        organisationUnitIndicator2.setIndicator(indicator);

        when(
            organisationUnitIndicatorRepository.findIndicatorsForOrganisationUnitAndIndicatorAccessLevel(
                organisationUnitId,
                accessLevel)).thenReturn(
            List.of(organisationUnitIndicator1, organisationUnitIndicator2));

        // When
        var response =
            organisationUnitIndicatorService.getIndicatorsForOrganisationUnit(organisationUnitId,
                accessLevel);

        // Then
        assertNotNull(response);
        assertEquals(2, response.size());
    }
}
