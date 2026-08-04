package rs.teslaris.core.unit.revisioner;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.mockStatic;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.time.Instant;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import rs.teslaris.core.applicationevent.DataQualityAssessmentReindexEvent;
import rs.teslaris.core.indexmodel.DocumentPublicationType;
import rs.teslaris.revisioner.indexrepository.DataQualityAssessmentIndexRepository;
import rs.teslaris.revisioner.model.EntityRevision;
import rs.teslaris.revisioner.model.qualityassessment.DataQualityAssessment;
import rs.teslaris.revisioner.repository.DataQualityAssessmentRepository;
import rs.teslaris.revisioner.service.impl.DataQualityReindexServiceImpl;
import rs.teslaris.revisioner.util.CompressionUtil;
import rs.teslaris.revisioner.util.RevisionHydratorRegistry;
import rs.teslaris.revisioner.util.dataquality.DataQualityAssessmentIndexer;
import rs.teslaris.revisioner.util.dataquality.DataQualityAssessmentListener;

@SpringBootTest
public class DataQualityReindexServiceTest {

    private static final String ENTITY_TYPE = DocumentPublicationType.INTANGIBLE_PRODUCT.name();

    @Mock
    private DataQualityAssessmentRepository dataQualityAssessmentRepository;

    @Mock
    private DataQualityAssessmentIndexRepository dataQualityAssessmentIndexRepository;

    @Mock
    private DataQualityAssessmentIndexer dataQualityAssessmentIndexer;

    @Mock
    private RevisionHydratorRegistry revisionHydratorRegistry;

    @InjectMocks
    private DataQualityReindexServiceImpl dataQualityReindexService;

    private DataQualityAssessment assessmentWithContent(String json) {
        var revision = EntityRevision.builder()
            .entityType(ENTITY_TYPE)
            .entityId(1)
            .revisionTimestamp(Instant.now())
            .contentHash("hash")
            .compressedContent(CompressionUtil.compress(json))
            .build();

        var assessment = DataQualityAssessment.builder()
            .revision(revision)
            .profileName("PTCRIS")
            .profileVersion("1.3")
            .finishedAt(Instant.now())
            .build();
        assessment.setId(1);

        return assessment;
    }

    private void stubSinglePage(DataQualityAssessment assessment) {
        Page<DataQualityAssessment> page =
            new PageImpl<>(List.of(assessment), PageRequest.of(0, 100), 1);

        when(dataQualityAssessmentRepository.findAll(any(PageRequest.class))).thenReturn(page);
    }

    @Test
    public void shouldClearIndexBeforeReindexing() {
        // given
        when(dataQualityAssessmentRepository.findAll(any(PageRequest.class)))
            .thenReturn(new PageImpl<>(List.of(), PageRequest.of(0, 100), 0));

        // when
        dataQualityReindexService.reindexDataQualityAssessments();

        // then
        verify(dataQualityAssessmentIndexRepository).deleteAll();
        verify(dataQualityAssessmentIndexer, never()).index(any(), any(), any());
    }

    @Test
    public void shouldIndexEveryStoredAssessment() {
        // given
        var assessment = assessmentWithContent("{\"id\":1,\"title\":\"Title\"}");
        stubSinglePage(assessment);

        doReturn(DummyDTO.class).when(revisionHydratorRegistry).getDtoClass(ENTITY_TYPE);

        try (var listener = mockStatic(DataQualityAssessmentListener.class)) {
            listener.when(() -> DataQualityAssessmentListener.resolveTargetType(ENTITY_TYPE))
                .thenReturn("DOCUMENT");

            // when
            dataQualityReindexService.reindexDataQualityAssessments();

            // then
            verify(dataQualityAssessmentIndexer)
                .index(eq(assessment), eq("DOCUMENT"), any(DummyDTO.class));
        }
    }

    @Test
    public void shouldSkipAssessmentWhenTargetTypeCannotBeResolved() {
        // given
        var assessment = assessmentWithContent("{\"id\":1,\"title\":\"Title\"}");
        stubSinglePage(assessment);

        try (var listener = mockStatic(DataQualityAssessmentListener.class)) {
            listener.when(() -> DataQualityAssessmentListener.resolveTargetType(ENTITY_TYPE))
                .thenReturn(null);

            // when
            dataQualityReindexService.reindexDataQualityAssessments();

            // then
            verify(dataQualityAssessmentIndexer, never()).index(any(), any(), any());
            verify(revisionHydratorRegistry, never()).getDtoClass(any());
        }
    }

    @Test
    public void shouldSkipAssessmentWhenStoredContentIsNotDeserializable() {
        // given
        var assessment = assessmentWithContent("{\"id\":\"not-a-number\"}");
        stubSinglePage(assessment);

        doReturn(DummyDTO.class).when(revisionHydratorRegistry).getDtoClass(ENTITY_TYPE);

        try (var listener = mockStatic(DataQualityAssessmentListener.class)) {
            listener.when(() -> DataQualityAssessmentListener.resolveTargetType(ENTITY_TYPE))
                .thenReturn("DOCUMENT");

            // when
            dataQualityReindexService.reindexDataQualityAssessments();

            // then
            verify(dataQualityAssessmentIndexer, never()).index(any(), any(), any());
        }
    }

    @Test
    public void shouldProcessAssessmentsInAscendingFinishedAtOrder() {
        // given
        when(dataQualityAssessmentRepository.findAll(any(PageRequest.class)))
            .thenReturn(new PageImpl<>(List.of(), PageRequest.of(0, 100), 0));

        // when
        dataQualityReindexService.reindexDataQualityAssessments();

        // then
        var captor = ArgumentCaptor.forClass(PageRequest.class);
        verify(dataQualityAssessmentRepository).findAll(captor.capture());

        var sortOrder = captor.getValue().getSort().getOrderFor("finishedAt");
        assertNotNull(sortOrder);
        assertTrue(sortOrder.isAscending());
        assertEquals(100, captor.getValue().getPageSize());
    }

    @Test
    public void shouldReindexWhenReindexEventIsReceived() {
        // given
        when(dataQualityAssessmentRepository.findAll(any(PageRequest.class)))
            .thenReturn(new PageImpl<>(List.of(), PageRequest.of(0, 100), 0));

        // when
        dataQualityReindexService.handleDataQualityAssessmentReindexEvent(
            new DataQualityAssessmentReindexEvent());

        // then
        verify(dataQualityAssessmentIndexRepository).deleteAll();
        verify(dataQualityAssessmentRepository).findAll(any(PageRequest.class));
    }

    record DummyDTO(Integer id, String title) {
    }
}
