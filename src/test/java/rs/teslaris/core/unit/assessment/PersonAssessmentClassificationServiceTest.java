package rs.teslaris.core.unit.assessment;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.atLeastOnce;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import org.junit.jupiter.api.Test;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import rs.teslaris.assessment.model.AssessmentMeasure;
import rs.teslaris.assessment.model.AssessmentResearchArea;
import rs.teslaris.assessment.model.AssessmentRulebook;
import rs.teslaris.assessment.model.classification.AssessmentClassification;
import rs.teslaris.assessment.model.classification.PersonAssessmentClassification;
import rs.teslaris.assessment.repository.AssessmentResearchAreaRepository;
import rs.teslaris.assessment.repository.AssessmentRulebookRepository;
import rs.teslaris.assessment.repository.classification.PersonAssessmentClassificationRepository;
import rs.teslaris.assessment.repository.indicator.DocumentIndicatorRepository;
import rs.teslaris.assessment.service.impl.classification.PersonAssessmentClassificationServiceImpl;
import rs.teslaris.core.indexmodel.DocumentPublicationIndex;
import rs.teslaris.core.indexmodel.PersonIndex;
import rs.teslaris.core.indexrepository.DocumentPublicationIndexRepository;
import rs.teslaris.core.indexrepository.PersonIndexRepository;
import rs.teslaris.core.model.institution.Commission;
import rs.teslaris.core.repository.commontypes.ResearchAreaRepository;
import rs.teslaris.core.repository.user.UserRepository;
import rs.teslaris.core.util.functional.Triple;

@SpringBootTest
public class PersonAssessmentClassificationServiceTest {

    @Mock
    private PersonAssessmentClassificationRepository personAssessmentClassificationRepository;

    @Mock
    private AssessmentRulebookRepository assessmentRulebookRepository;

    @Mock
    private DocumentIndicatorRepository documentIndicatorRepository;

    @Mock
    private DocumentPublicationIndexRepository documentPublicationIndexRepository;

    @Mock
    private PersonIndexRepository personIndexRepository;

    @Mock
    private UserRepository userRepository;

    @Mock
    private ResearchAreaRepository researchAreaRepository;

    @Mock
    private AssessmentResearchAreaRepository assessmentResearchAreaRepository;

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
        personAssessmentClassification1.setClassificationYear(2025);

        var personAssessmentClassification2 = new PersonAssessmentClassification();
        personAssessmentClassification2.setAssessmentClassification(assessmentClassification);
        personAssessmentClassification2.setClassificationYear(2025);

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

    @Test
    void shouldReindexAllResearchersInChunks() {
        // Given
        var researcher1 = new PersonIndex();
        researcher1.setDatabaseId(1);
        var researcher2 = new PersonIndex();
        researcher2.setDatabaseId(2);

        when(personIndexRepository.findAll(PageRequest.of(0, 1000)))
            .thenReturn(
                new PageImpl<>(List.of(researcher1, researcher2), PageRequest.of(0, 1000), 2));

        when(assessmentRulebookRepository.readAssessmentMeasuresForRulebook(any(), any()))
            .thenReturn(new PageImpl<>(Collections.emptyList()));
        when(userRepository.findUserCommissionForOrganisationUnits(any()))
            .thenReturn(Collections.emptyList());
        when(documentPublicationIndexRepository.findAssessedByAuthorIds(anyInt(), any()))
            .thenReturn(new PageImpl<>(Collections.emptyList()));
        when(assessmentRulebookRepository.findDefaultRulebook()).thenReturn(
            Optional.of(new AssessmentRulebook()));

        // When
        personAssessmentClassificationService.reindexPublicationPointsForAllResearchers();

        // Then
        verify(personIndexRepository, atLeastOnce()).findAll(any(PageRequest.class));
        verify(personIndexRepository).findAll(PageRequest.of(0, 1000));
    }

    @Test
    void shouldReindexPublicationPointsForResearcher() {
        // Given
        var researcher = new PersonIndex();
        researcher.setDatabaseId(42);
        researcher.setEmploymentInstitutionsIdHierarchy(List.of(1, 2, 3));

        var assessmentMeasure = new AssessmentMeasure();
        assessmentMeasure.setCode("M1");

        when(assessmentRulebookRepository.readAssessmentMeasuresForRulebook(any(), any()))
            .thenReturn(new PageImpl<>(List.of(assessmentMeasure)));

        var commission = new Commission();
        commission.setId(10);
        commission.setRecognisedResearchAreas(Set.of("CS"));
        when(userRepository.findUserCommissionForOrganisationUnits(any()))
            .thenReturn(List.of(commission));

        var publication = new DocumentPublicationIndex();
        publication.setDatabaseId(100);
        publication.setCommissionAssessments(List.of(new Triple<>(10, "CS", false)));
        publication.setAssessmentPoints(new ArrayList<>());

        when(documentPublicationIndexRepository.findAssessedByAuthorIds(eq(42), any()))
            .thenReturn(new PageImpl<>(List.of(publication), PageRequest.of(0, 1000), 1))
            .thenReturn(new PageImpl<>(Collections.emptyList(), PageRequest.of(1, 1000), 1));

        when(documentIndicatorRepository.findIndicatorsForDocumentAndIndicatorAccessLevel(anyInt(),
            any()))
            .thenReturn(Collections.emptyList());
        when(assessmentRulebookRepository.findDefaultRulebook()).thenReturn(
            Optional.of(new AssessmentRulebook()));

        // Mock research area resolution
        when(assessmentResearchAreaRepository.findForPersonIdAndCommissionId(eq(42),
            any(Integer.class)))
            .thenReturn(Optional.of(new AssessmentResearchArea() {{
                setResearchAreaCode("CS");
            }}));

        // When
        personAssessmentClassificationService.reindexPublicationPointsForResearcher(researcher);

        // Then
        verify(assessmentRulebookRepository).readAssessmentMeasuresForRulebook(any(), any());
        verify(userRepository).findUserCommissionForOrganisationUnits(List.of(1, 2, 3));
        verify(documentPublicationIndexRepository, times(1)).findAssessedByAuthorIds(eq(42), any());
        verify(documentPublicationIndexRepository).save(publication);
    }

    @Test
    void shouldSkipCommissionWhenResearchAreaNotRecognized() {
        // Given
        var researcher = new PersonIndex();
        researcher.setDatabaseId(42);
        researcher.setEmploymentInstitutionsIdHierarchy(List.of(1));

        var assessmentMeasure = new AssessmentMeasure();
        assessmentMeasure.setCode("M1");

        when(assessmentRulebookRepository.readAssessmentMeasuresForRulebook(any(), any()))
            .thenReturn(new PageImpl<>(List.of(assessmentMeasure)));

        var commission = new Commission();
        commission.setId(10);
        commission.setRecognisedResearchAreas(Set.of("MATH")); // CS not recognized

        when(userRepository.findUserCommissionForOrganisationUnits(any()))
            .thenReturn(List.of(commission));

        when(documentPublicationIndexRepository.findAssessedByAuthorIds(anyInt(), any()))
            .thenReturn(new PageImpl<>(Collections.emptyList()));
        when(assessmentRulebookRepository.findDefaultRulebook()).thenReturn(
            Optional.of(new AssessmentRulebook()));

        when(assessmentResearchAreaRepository.findForPersonIdAndCommissionId(eq(42),
            any(Integer.class)))
            .thenReturn(Optional.of(new AssessmentResearchArea() {{
                setResearchAreaCode("CS");
            }}));

        // When
        personAssessmentClassificationService.reindexPublicationPointsForResearcher(researcher);

        // Then
        verify(documentPublicationIndexRepository, never()).save(any());
    }

    @Test
    void shouldRemoveExistingPointsBeforeAddingNewOnes() {
        // Given
        var researcher = new PersonIndex();
        researcher.setDatabaseId(42);
        researcher.setEmploymentInstitutionsIdHierarchy(List.of(1));

        var assessmentMeasure = new AssessmentMeasure();
        assessmentMeasure.setCode("M1");

        when(assessmentRulebookRepository.readAssessmentMeasuresForRulebook(any(), any()))
            .thenReturn(new PageImpl<>(List.of(assessmentMeasure)));

        var commission = new Commission();
        commission.setId(10);
        commission.setRecognisedResearchAreas(Set.of("CS"));
        when(userRepository.findUserCommissionForOrganisationUnits(any()))
            .thenReturn(List.of(commission));

        var publication = new DocumentPublicationIndex();
        publication.setDatabaseId(100);
        publication.setCommissionAssessments(List.of(new Triple<>(10, "CS", false)));

        // Existing points for same researcher and commission
        var existingPoints = new Triple<>(42, 10, 5.0);
        publication.setAssessmentPoints(new ArrayList<>(List.of(existingPoints)));

        when(documentPublicationIndexRepository.findAssessedByAuthorIds(eq(42), any()))
            .thenReturn(new PageImpl<>(List.of(publication)))
            .thenReturn(new PageImpl<>(Collections.emptyList()));

        when(documentIndicatorRepository.findIndicatorsForDocumentAndIndicatorAccessLevel(anyInt(),
            any()))
            .thenReturn(Collections.emptyList());
        when(assessmentRulebookRepository.findDefaultRulebook()).thenReturn(
            Optional.of(new AssessmentRulebook()));

        when(assessmentResearchAreaRepository.findForPersonIdAndCommissionId(eq(42),
            any(Integer.class)))
            .thenReturn(Optional.of(new AssessmentResearchArea() {{
                setResearchAreaCode("CS");
            }}));

        // When
        personAssessmentClassificationService.reindexPublicationPointsForResearcher(researcher);

        // Then
        verify(documentPublicationIndexRepository).save(publication);
    }

    @Test
    void shouldHandleEmptyCommissionList() {
        // Given
        var researcher = new PersonIndex();
        researcher.setDatabaseId(42);
        researcher.setEmploymentInstitutionsIdHierarchy(List.of(1));

        when(documentPublicationIndexRepository.findAssessedByAuthorIds(any(), any()))
            .thenReturn(
                new PageImpl<>(List.of(), PageRequest.of(0, 1), 0));
        when(assessmentRulebookRepository.readAssessmentMeasuresForRulebook(any(), any()))
            .thenReturn(new PageImpl<>(Collections.emptyList()));
        when(userRepository.findUserCommissionForOrganisationUnits(any()))
            .thenReturn(Collections.emptyList());
        when(assessmentRulebookRepository.findDefaultRulebook()).thenReturn(
            Optional.of(new AssessmentRulebook()));

        // When
        personAssessmentClassificationService.reindexPublicationPointsForResearcher(researcher);

        // Then
        verify(documentPublicationIndexRepository, never()).save(any());
    }

    @Test
    void shouldProcessMultipleCommissionsForResearcher() {
        // Given
        var researcher = new PersonIndex();
        researcher.setDatabaseId(42);
        researcher.setEmploymentInstitutionsIdHierarchy(List.of(1));

        var assessmentMeasure = new AssessmentMeasure();
        assessmentMeasure.setCode("M1");

        when(assessmentRulebookRepository.readAssessmentMeasuresForRulebook(any(), any()))
            .thenReturn(new PageImpl<>(List.of(assessmentMeasure)));

        var commission1 = new Commission();
        commission1.setId(10);
        commission1.setRecognisedResearchAreas(Set.of("CS"));

        var commission2 = new Commission();
        commission2.setId(20);
        commission2.setRecognisedResearchAreas(Set.of("PHYS"));

        when(userRepository.findUserCommissionForOrganisationUnits(any()))
            .thenReturn(List.of(commission1, commission2));

        var publication = new DocumentPublicationIndex();
        publication.setDatabaseId(100);
        publication.setCommissionAssessments(List.of(
            new Triple<>(10, "CS", false),
            new Triple<>(20, "PHYS", false)
        ));
        publication.setAssessmentPoints(new ArrayList<>());

        when(documentPublicationIndexRepository.findAssessedByAuthorIds(eq(42), any()))
            .thenReturn(new PageImpl<>(List.of(publication)))
            .thenReturn(new PageImpl<>(Collections.emptyList()));

        when(documentIndicatorRepository.findIndicatorsForDocumentAndIndicatorAccessLevel(anyInt(),
            any()))
            .thenReturn(Collections.emptyList());
        when(assessmentRulebookRepository.findDefaultRulebook()).thenReturn(
            Optional.of(new AssessmentRulebook()));

        when(assessmentResearchAreaRepository.findForPersonIdAndCommissionId(eq(42),
            any(Integer.class)))
            .thenReturn(Optional.of(new AssessmentResearchArea() {{
                setResearchAreaCode("CS");
            }}))
            .thenReturn(Optional.of(new AssessmentResearchArea() {{
                setResearchAreaCode("PHYS");
            }}));

        // When
        personAssessmentClassificationService.reindexPublicationPointsForResearcher(researcher);

        // Then
        verify(documentPublicationIndexRepository).save(publication);
    }

    @Test
    void shouldStopWhenNoMoreResearchers() {
        // Given
        when(personIndexRepository.findAll(PageRequest.of(0, 1000)))
            .thenReturn(new PageImpl<>(Collections.emptyList()));
        when(assessmentRulebookRepository.findDefaultRulebook()).thenReturn(
            Optional.of(new AssessmentRulebook()));

        // When
        personAssessmentClassificationService.reindexPublicationPointsForAllResearchers();

        // Then
        verify(personIndexRepository, times(1)).findAll(any(PageRequest.class));
        verify(personIndexRepository, never()).findAll(PageRequest.of(1, 1000));
    }
}
