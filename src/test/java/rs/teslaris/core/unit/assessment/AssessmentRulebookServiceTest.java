package rs.teslaris.core.unit.assessment;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import org.junit.jupiter.api.Test;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import rs.teslaris.core.assessment.dto.AssessmentRulebookDTO;
import rs.teslaris.core.assessment.model.AssessmentMeasure;
import rs.teslaris.core.assessment.model.AssessmentRulebook;
import rs.teslaris.core.assessment.repository.AssessmentRulebookRepository;
import rs.teslaris.core.assessment.service.impl.AssessmentRulebookServiceImpl;
import rs.teslaris.core.dto.commontypes.MultilingualContentDTO;
import rs.teslaris.core.dto.document.DocumentFileDTO;
import rs.teslaris.core.indexmodel.DocumentFileIndex;
import rs.teslaris.core.model.commontypes.LanguageTag;
import rs.teslaris.core.model.commontypes.MultiLingualContent;
import rs.teslaris.core.model.document.DocumentFile;
import rs.teslaris.core.service.interfaces.commontypes.MultilingualContentService;
import rs.teslaris.core.service.interfaces.document.DocumentFileService;
import rs.teslaris.core.util.language.LanguageAbbreviations;

@SpringBootTest
public class AssessmentRulebookServiceTest {

    @Mock
    private MultilingualContentService multilingualContentService;

    @Mock
    private AssessmentRulebookRepository assessmentRulebookRepository;

    @Mock
    private DocumentFileService documentFileService;

    @InjectMocks
    private AssessmentRulebookServiceImpl assessmentRulebookService;


    @Test
    void shouldReadAllAssessmentRulebooks() {
        var assessmentRulebook1 = new AssessmentRulebook();
        assessmentRulebook1.setIssueDate(LocalDate.of(2024, 3, 13));
        assessmentRulebook1.setName(
            Set.of(new MultiLingualContent(new LanguageTag(), "Name 1", 1)));

        var assessmentRulebook2 = new AssessmentRulebook();
        assessmentRulebook2.setIssueDate(LocalDate.of(2024, 7, 7));
        assessmentRulebook2.setName(
            Set.of(new MultiLingualContent(new LanguageTag(), "Name 2", 1)));

        when(assessmentRulebookRepository.readAll(eq(LanguageAbbreviations.SERBIAN),
            any(Pageable.class))).thenReturn(
            new PageImpl<>(List.of(assessmentRulebook1, assessmentRulebook2)));

        var response =
            assessmentRulebookService.readAllAssessmentRulebooks(PageRequest.of(0, 10),
                LanguageAbbreviations.SERBIAN);

        assertNotNull(response);
        assertEquals(2, response.getSize());
    }

    @Test
    void shouldReadAssessmentRulebook() {
        var assessmentRulebookId = 1;
        var assessmentRulebook = new AssessmentRulebook();
        assessmentRulebook.setName(
            Set.of(new MultiLingualContent(new LanguageTag(), "Title", 1)));
        assessmentRulebook.setDescription(
            Set.of(new MultiLingualContent(new LanguageTag(), "Description", 1)));
        assessmentRulebook.setIssueDate(LocalDate.of(2024, 10, 2));

        when(assessmentRulebookRepository.findById(assessmentRulebookId))
            .thenReturn(Optional.of(assessmentRulebook));

        var result = assessmentRulebookService.readAssessmentRulebookById(assessmentRulebookId);

        assertEquals(assessmentRulebook.getIssueDate(), result.issueDate());
        verify(assessmentRulebookRepository).findById(assessmentRulebookId);
    }

    @Test
    void shouldCreateAssessmentRulebook() {
        var assessmentRulebookDTO =
            new AssessmentRulebookDTO(List.of(new MultilingualContentDTO()),
                List.of(new MultilingualContentDTO()), LocalDate.of(2024, 10, 2), null);
        var newAssessmentRulebook = new AssessmentRulebook();

        when(assessmentRulebookRepository.save(any(AssessmentRulebook.class)))
            .thenReturn(newAssessmentRulebook);

        var result = assessmentRulebookService.createAssessmentRulebook(
            assessmentRulebookDTO);

        assertNotNull(result);
        verify(assessmentRulebookRepository).save(any(AssessmentRulebook.class));
    }

    @Test
    void shouldUpdateAssessmentRulebook() {
        var assessmentRulebookId = 1;
        var assessmentRulebookDTO =
            new AssessmentRulebookDTO(List.of(new MultilingualContentDTO()),
                List.of(new MultilingualContentDTO()), LocalDate.of(2024, 10, 2), null);
        var existingAssessmentRulebook = new AssessmentRulebook();

        when(assessmentRulebookRepository.findById(assessmentRulebookId))
            .thenReturn(Optional.of(existingAssessmentRulebook));

        assessmentRulebookService.updateAssessmentRulebook(assessmentRulebookId,
            assessmentRulebookDTO);

        verify(assessmentRulebookRepository).findById(assessmentRulebookId);
        verify(assessmentRulebookRepository).save(existingAssessmentRulebook);
    }

    @Test
    void shouldDeleteAssessmentClassification() {
        // Given
        var assessmentRulebookId = 1;

        when(assessmentRulebookRepository.findById(assessmentRulebookId)).thenReturn(
            Optional.of(new AssessmentRulebook()));

        // When
        assessmentRulebookService.deleteAssessmentRulebook(assessmentRulebookId);

        // Then
        verify(assessmentRulebookRepository).save(any());
    }

    @Test
    void shouldSetDefaultRulebook() {
        // Given
        var assessmentRulebookId = 1;
        var rulebook = new AssessmentRulebook();
        rulebook.setIsDefault(false);

        when(assessmentRulebookRepository.findById(assessmentRulebookId)).thenReturn(
            Optional.of(rulebook));

        // When
        assessmentRulebookService.setDefaultRulebook(assessmentRulebookId);

        // Then
        assertTrue(rulebook.getIsDefault());
        verify(assessmentRulebookRepository).setAllOthersAsNonDefault(assessmentRulebookId);
        verify(assessmentRulebookRepository).save(rulebook);
    }

    @Test
    public void shouldAddPdfFile() {
        // Given
        var rulebookId = 1;
        var rulebook = new AssessmentRulebook();
        var documentFile = new DocumentFile();
        documentFile.setId(1);

        when(assessmentRulebookRepository.findById(rulebookId)).thenReturn(Optional.of(rulebook));
        when(documentFileService.saveNewDocument(any(DocumentFileDTO.class), eq(false))).thenReturn(
            documentFile);
        when(documentFileService.findDocumentFileIndexByDatabaseId(any())).thenReturn(
            new DocumentFileIndex());

        // When
        assessmentRulebookService.addPDFFile(rulebookId, new DocumentFileDTO());

        // Then
        verify(assessmentRulebookRepository, times(1)).save(rulebook);
    }

    @Test
    public void shouldDeleteDocumentFileWithProof() {
        // Given
        var rulebookId = 1;
        var documentFileId = 1;
        var rulebook = new AssessmentRulebook();
        var documentFile = new DocumentFile();

        when(assessmentRulebookRepository.findById(rulebookId)).thenReturn(Optional.of(rulebook));
        when(documentFileService.findOne(documentFileId)).thenReturn(documentFile);

        // When
        assessmentRulebookService.deletePDFFile(rulebookId, documentFileId);

        // Then
        verify(documentFileService, times(1)).deleteDocumentFile(any());
    }

    @Test
    void shouldReadAllAssessmentMeasuresForRulebook() {
        // given
        var assessmentMeasure1 = new AssessmentMeasure();
        assessmentMeasure1.setCode("code1");
        assessmentMeasure1.setPointRule("serbianPointsRulebook2025");
        assessmentMeasure1.setScalingRule("serbianScalingRulebook2025");
        assessmentMeasure1.setRulebook(new AssessmentRulebook());

        var assessmentMeasure2 = new AssessmentMeasure();
        assessmentMeasure2.setCode("code2");
        assessmentMeasure2.setPointRule("serbianPointsRulebook2025");
        assessmentMeasure2.setScalingRule("serbianScalingRulebook2025");
        assessmentMeasure2.setRulebook(new AssessmentRulebook());

        when(assessmentRulebookRepository.readAssessmentMeasuresForRulebook(any(Pageable.class),
            anyInt()))
            .thenReturn(new PageImpl<>(List.of(assessmentMeasure1, assessmentMeasure2)));

        // when
        var response =
            assessmentRulebookService.readAssessmentMeasuresForRulebook(PageRequest.of(0, 10), 1);

        // then
        assertNotNull(response);
        assertEquals(2, response.getSize());
    }

}
