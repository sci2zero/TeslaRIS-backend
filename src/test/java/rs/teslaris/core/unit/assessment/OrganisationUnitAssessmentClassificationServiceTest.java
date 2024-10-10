package rs.teslaris.core.unit.assessment;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.mockito.Mockito.when;

import java.util.List;
import org.junit.jupiter.api.Test;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.springframework.boot.test.context.SpringBootTest;
import rs.teslaris.core.assessment.model.AssessmentClassification;
import rs.teslaris.core.assessment.model.OrganisationUnitAssessmentClassification;
import rs.teslaris.core.assessment.repository.OrganisationUnitAssessmentClassificationRepository;
import rs.teslaris.core.assessment.service.impl.OrganisationUnitAssessmentClassificationServiceImpl;


@SpringBootTest
public class OrganisationUnitAssessmentClassificationServiceTest {

    @Mock
    private OrganisationUnitAssessmentClassificationRepository
        organisationUnitAssessmentClassificationRepository;

    @InjectMocks
    private OrganisationUnitAssessmentClassificationServiceImpl
        organisationUnitAssessmentClassificationService;

    @Test
    void shouldReadAllOrganisationUnitAssessmentClassificationsForOrganisationUnit() {
        // Given
        var organisationUnitId = 1;

        var assessmentClassification = new AssessmentClassification();
        assessmentClassification.setCode("code");

        var organisationUnitAssessmentClassification1 =
            new OrganisationUnitAssessmentClassification();
        organisationUnitAssessmentClassification1.setAssessmentClassification(
            assessmentClassification);

        var organisationUnitAssessmentClassification2 =
            new OrganisationUnitAssessmentClassification();
        organisationUnitAssessmentClassification2.setAssessmentClassification(
            assessmentClassification);

        when(
            organisationUnitAssessmentClassificationRepository.findAssessmentClassificationsForOrganisationUnit(
                organisationUnitId)).thenReturn(
            List.of(organisationUnitAssessmentClassification1,
                organisationUnitAssessmentClassification2));

        // When
        var response =
            organisationUnitAssessmentClassificationService.getAssessmentClassificationsForOrganisationUnit(
                organisationUnitId);

        // Then
        assertNotNull(response);
        assertEquals(2, response.size());
    }
}