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
import rs.teslaris.core.assessment.model.PersonAssessmentClassification;
import rs.teslaris.core.assessment.repository.PersonAssessmentClassificationRepository;
import rs.teslaris.core.assessment.service.impl.PersonAssessmentClassificationServiceImpl;

@SpringBootTest
public class PersonAssessmentClassificationServiceTest {

    @Mock
    private PersonAssessmentClassificationRepository personAssessmentClassificationRepository;

    @InjectMocks
    private PersonAssessmentClassificationServiceImpl personAssessmentClassificationService;

    @Test
    void shouldReadAllPersonAssessmentClassificationsForPerson() {
        // Given
        var personId = 1;

        var assessmentClassification = new AssessmentClassification();
        assessmentClassification.setCode("code");

        var personAssessmentClassification1 = new PersonAssessmentClassification();
        personAssessmentClassification1.setAssessmentClassification(assessmentClassification);

        var personAssessmentClassification2 = new PersonAssessmentClassification();
        personAssessmentClassification2.setAssessmentClassification(assessmentClassification);

        when(
            personAssessmentClassificationRepository.findAssessmentClassificationsForPerson(
                personId)).thenReturn(
            List.of(personAssessmentClassification1, personAssessmentClassification2));

        // When
        var response =
            personAssessmentClassificationService.getAssessmentClassificationsForPerson(personId);

        // Then
        assertNotNull(response);
        assertEquals(2, response.size());
    }
}
