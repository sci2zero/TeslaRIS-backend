package rs.teslaris.revisioner.unit;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.atLeastOnce;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.inOrder;
import static org.mockito.Mockito.mockStatic;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.IntStream;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockedStatic;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.data.domain.Limit;
import org.springframework.transaction.PlatformTransactionManager;
import rs.teslaris.core.applicationevent.DataQualityAssessmentReindexEvent;
import rs.teslaris.core.indexmodel.DocumentPublicationType;
import rs.teslaris.revisioner.indexmodel.DataQualityAssessmentIndex;
import rs.teslaris.revisioner.indexrepository.DataQualityAssessmentIndexRepository;
import rs.teslaris.revisioner.model.EntityRevision;
import rs.teslaris.revisioner.model.qualityassessment.AssessedEntityKey;
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

    private static final String VALID_JSON = "{\"id\":1,\"title\":\"Title\"}";

    private static final Instant BASE_TIME = Instant.parse("2026-01-01T10:00:00Z");
    private static final Comparator<AssessedEntityKey> KEY_ORDER =
        Comparator.comparing(AssessedEntityKey::entityType)
            .thenComparing(AssessedEntityKey::entityId);
    private final Map<String, DataQualityAssessmentIndex> savedById = new HashMap<>();
    @Mock
    private DataQualityAssessmentRepository dataQualityAssessmentRepository;
    @Mock
    private DataQualityAssessmentIndexRepository dataQualityAssessmentIndexRepository;
    @Mock
    private DataQualityAssessmentIndexer dataQualityAssessmentIndexer;
    @Mock
    private RevisionHydratorRegistry revisionHydratorRegistry;
    @Mock
    private PlatformTransactionManager transactionManager;
    @InjectMocks
    private DataQualityReindexServiceImpl dataQualityReindexService;
    private MockedStatic<DataQualityAssessmentListener> listener;

    private static DataQualityAssessmentIndex documentFor(DataQualityAssessment assessment) {
        var document = new DataQualityAssessmentIndex();
        document.setId(String.valueOf(assessment.getId()));
        document.setEntityType(assessment.getRevision().getEntityType());
        document.setEntityId(assessment.getRevision().getEntityId());
        document.setProfileName(assessment.getProfileName());
        document.setAssessmentDate(
            LocalDateTime.ofInstant(assessment.getFinishedAt(), ZoneOffset.UTC));
        document.setValidTo(DataQualityAssessmentIndex.OPEN_INTERVAL_END);
        document.setLatest(true);
        return document;
    }

    private static LocalDateTime dateOf(DataQualityAssessment assessment) {
        return LocalDateTime.ofInstant(assessment.getFinishedAt(), ZoneOffset.UTC);
    }

    private static AssessedEntityKey keyOf(DataQualityAssessment assessment) {
        return new AssessedEntityKey(assessment.getRevision().getEntityType(),
            assessment.getRevision().getEntityId());
    }

    @BeforeEach
    public void setUp() {
        listener = mockStatic(DataQualityAssessmentListener.class);
        listener.when(() -> DataQualityAssessmentListener.resolveTargetTypes(ENTITY_TYPE))
            .thenReturn(List.of("Document"));

        doReturn(DummyDTO.class).when(revisionHydratorRegistry).getAssessmentDtoClass(ENTITY_TYPE);

        when(dataQualityAssessmentIndexer.buildIndex(any(), anyList(), any()))
            .thenAnswer(invocation -> documentFor(invocation.getArgument(0)));

        when(dataQualityAssessmentIndexRepository.saveAll(anyList())).thenAnswer(invocation -> {
            List<DataQualityAssessmentIndex> documents = invocation.getArgument(0);
            documents.forEach(document -> savedById.put(document.getId(), document));
            return documents;
        });
    }

    @AfterEach
    public void tearDown() {
        listener.close();
        savedById.clear();
    }

    private DataQualityAssessment assessment(int id, int entityId, String profileName,
                                             int minutesAfterBase, String json) {
        var revision = EntityRevision.builder()
            .entityType(ENTITY_TYPE)
            .entityId(entityId)
            .revisionTimestamp(BASE_TIME)
            .contentHash("hash")
            .compressedContent(CompressionUtil.compress(json))
            .build();

        var assessment = DataQualityAssessment.builder()
            .revision(revision)
            .profileName(profileName)
            .profileVersion("1.3")
            .finishedAt(BASE_TIME.plusSeconds(60L * minutesAfterBase))
            .build();
        assessment.setId(id);

        return assessment;
    }

    private DataQualityAssessment assessment(int id, int entityId, String profileName,
                                             int minutesAfterBase) {
        return assessment(id, entityId, profileName, minutesAfterBase, VALID_JSON);
    }

    /**
     * Fakes both keyset queries over an in-memory table, honouring their ordering and limit.
     */
    private void stubAssessments(List<DataQualityAssessment> stored) {
        when(dataQualityAssessmentRepository.findAssessedEntityKeysAfter(any(), any(),
            any(Limit.class))).thenAnswer(invocation -> {
            var after = new AssessedEntityKey(invocation.getArgument(0),
                invocation.getArgument(1));
            Limit limit = invocation.getArgument(2);

            return stored.stream()
                .map(DataQualityReindexServiceTest::keyOf)
                .distinct()
                .filter(key -> KEY_ORDER.compare(key, after) > 0)
                .sorted(KEY_ORDER)
                .limit(limit.max())
                .toList();
        });

        when(dataQualityAssessmentRepository.findAllWithRevisionForEntityRange(any(), any(),
            any(), any())).thenAnswer(invocation -> {
            var from = new AssessedEntityKey(invocation.getArgument(0),
                invocation.getArgument(1));
            var to = new AssessedEntityKey(invocation.getArgument(2),
                invocation.getArgument(3));

            return stored.stream()
                .filter(a -> KEY_ORDER.compare(keyOf(a), from) >= 0 &&
                    KEY_ORDER.compare(keyOf(a), to) <= 0)
                .sorted(Comparator.comparing(DataQualityReindexServiceTest::keyOf, KEY_ORDER)
                    .thenComparing(DataQualityAssessment::getProfileName)
                    .thenComparing(DataQualityAssessment::getFinishedAt)
                    .thenComparing(DataQualityAssessment::getId))
                .toList();
        });
    }

    private void stubAssessments(DataQualityAssessment... stored) {
        stubAssessments(List.of(stored));
    }

    private void assertLatest(DataQualityAssessmentIndex document) {
        assertTrue(document.isLatest());
        assertEquals(DataQualityAssessmentIndex.OPEN_INTERVAL_END, document.getValidTo());
    }

    private void assertSuperseded(DataQualityAssessmentIndex document, LocalDateTime validTo) {
        assertFalse(document.isLatest());
        assertEquals(validTo, document.getValidTo());
    }

    @Test
    public void shouldClearIndexBeforeReindexing() {
        // given
        stubAssessments();

        // when
        dataQualityReindexService.reindexDataQualityAssessments();

        // then
        var order = inOrder(dataQualityAssessmentIndexRepository, dataQualityAssessmentRepository);
        order.verify(dataQualityAssessmentIndexRepository).deleteAll();
        order.verify(dataQualityAssessmentRepository)
            .findAssessedEntityKeysAfter(eq(""), eq(Integer.MIN_VALUE), eq(Limit.of(100)));
        verify(dataQualityAssessmentIndexer, never()).buildIndex(any(), any(), any());
        verify(dataQualityAssessmentIndexRepository, never()).saveAll(anyList());
    }

    @Test
    public void shouldIndexEveryStoredAssessment() {
        // given
        var assessment = assessment(1, 1, "PTCRIS", 0);
        stubAssessments(assessment);

        // when
        dataQualityReindexService.reindexDataQualityAssessments();

        // then
        verify(dataQualityAssessmentIndexer)
            .buildIndex(eq(assessment), eq(List.of("Document")), any(DummyDTO.class));
        assertEquals(1, savedById.size());
        assertLatest(savedById.get("1"));
    }

    @Test
    public void shouldNeverQueryIndexToChainDocuments() {
        // given
        stubAssessments(assessment(1, 1, "PTCRIS", 0), assessment(2, 1, "PTCRIS", 5));

        // when
        dataQualityReindexService.reindexDataQualityAssessments();

        // then
        verify(dataQualityAssessmentIndexer, never()).index(any(), any(), any());
        verify(dataQualityAssessmentIndexRepository, never())
            .findByEntityTypeAndEntityIdAndProfileNameAndIsLatestTrue(any(), any(), any());
    }

    @Test
    public void shouldLoadAssessmentsInsideTransaction() {
        // given
        stubAssessments(assessment(1, 1, "PTCRIS", 0));

        // when
        dataQualityReindexService.reindexDataQualityAssessments();

        // then
        verify(transactionManager, atLeastOnce()).getTransaction(any());
        verify(transactionManager, atLeastOnce()).commit(any());
    }

    @Test
    public void shouldAdvanceKeysetCursorUntilNoEntitiesRemain() {
        // given
        var stored = IntStream.rangeClosed(1, 250)
            .mapToObj(entityId -> assessment(entityId, entityId, "PTCRIS", 0))
            .toList();
        stubAssessments(stored);

        // when
        dataQualityReindexService.reindexDataQualityAssessments();

        // then
        var afterTypes = ArgumentCaptor.forClass(String.class);
        var afterIds = ArgumentCaptor.forClass(Integer.class);
        verify(dataQualityAssessmentRepository, times(4))
            .findAssessedEntityKeysAfter(afterTypes.capture(), afterIds.capture(),
                eq(Limit.of(100)));
        assertEquals(List.of("", ENTITY_TYPE, ENTITY_TYPE, ENTITY_TYPE),
            afterTypes.getAllValues());
        assertEquals(List.of(Integer.MIN_VALUE, 100, 200, 250), afterIds.getAllValues());

        verify(dataQualityAssessmentRepository)
            .findAllWithRevisionForEntityRange(ENTITY_TYPE, 1, ENTITY_TYPE, 100);
        verify(dataQualityAssessmentRepository)
            .findAllWithRevisionForEntityRange(ENTITY_TYPE, 201, ENTITY_TYPE, 250);
        assertEquals(250, savedById.size());
    }

    @Test
    public void shouldChainAssessmentsOfSameEntityAndProfile() {
        // given
        var first = assessment(1, 1, "PTCRIS", 0);
        var second = assessment(2, 1, "PTCRIS", 5);
        var third = assessment(3, 1, "PTCRIS", 10);
        stubAssessments(first, second, third);

        // when
        dataQualityReindexService.reindexDataQualityAssessments();

        // then
        assertSuperseded(savedById.get("1"), dateOf(second));
        assertSuperseded(savedById.get("2"), dateOf(third));
        assertLatest(savedById.get("3"));
    }

    @Test
    public void shouldNotChainAcrossEntitiesOrProfiles() {
        // given
        stubAssessments(
            assessment(1, 1, "OTHER", 0),
            assessment(2, 1, "PTCRIS", 5),
            assessment(3, 2, "PTCRIS", 10));

        // when
        dataQualityReindexService.reindexDataQualityAssessments();

        // then
        assertLatest(savedById.get("1"));
        assertLatest(savedById.get("2"));
        assertLatest(savedById.get("3"));
    }

    @Test
    public void shouldKeepWholeChainInOneBatchAtBatchBoundary() {
        // given
        var stored = new ArrayList<DataQualityAssessment>();
        IntStream.rangeClosed(1, 99)
            .forEach(entityId -> stored.add(assessment(entityId, entityId, "PTCRIS", 0)));
        var first = assessment(1000, 100, "PTCRIS", 0);
        var second = assessment(1001, 100, "PTCRIS", 5);
        stored.add(second);
        stored.add(first);
        stored.add(assessment(1002, 101, "PTCRIS", 0));
        stubAssessments(stored);

        // when
        dataQualityReindexService.reindexDataQualityAssessments();

        // then
        assertSuperseded(savedById.get("1000"), dateOf(second));
        assertLatest(savedById.get("1001"));
        assertLatest(savedById.get("1002"));
    }

    @Test
    public void shouldSaveEachDocumentOnceWithFinalState() {
        // given
        var saveCounts = new HashMap<String, Integer>();
        doAnswer(invocation -> {
            List<DataQualityAssessmentIndex> documents = invocation.getArgument(0);
            documents.forEach(document -> saveCounts.merge(document.getId(), 1, Integer::sum));
            return documents;
        }).when(dataQualityAssessmentIndexRepository).saveAll(anyList());

        var stored = new ArrayList<DataQualityAssessment>();
        IntStream.rangeClosed(1, 150).forEach(entityId -> {
            stored.add(assessment(entityId * 10, entityId, "PTCRIS", 0));
            stored.add(assessment(entityId * 10 + 1, entityId, "PTCRIS", 5));
        });
        stubAssessments(stored);

        // when
        dataQualityReindexService.reindexDataQualityAssessments();

        // then
        assertEquals(300, saveCounts.size());
        assertTrue(saveCounts.values().stream().allMatch(count -> count == 1));
        verify(dataQualityAssessmentIndexRepository, never()).save(any());
    }

    @Test
    public void shouldSkipAssessmentWhenTargetTypeCannotBeResolved() {
        // given
        stubAssessments(assessment(1, 1, "PTCRIS", 0));
        listener.when(() -> DataQualityAssessmentListener.resolveTargetTypes(ENTITY_TYPE))
            .thenReturn(List.of());

        // when
        dataQualityReindexService.reindexDataQualityAssessments();

        // then
        verify(dataQualityAssessmentIndexer, never()).buildIndex(any(), any(), any());
        verify(revisionHydratorRegistry, never()).getAssessmentDtoClass(any());
        assertTrue(savedById.isEmpty());
    }

    @Test
    public void shouldSkipAssessmentWhenStoredContentIsNotDeserializable() {
        // given
        var broken = assessment(1, 1, "PTCRIS", 0, "{\"id\":\"not-a-number\"}");
        var healthy = assessment(2, 2, "PTCRIS", 0);
        stubAssessments(broken, healthy);

        // when
        dataQualityReindexService.reindexDataQualityAssessments();

        // then
        verify(dataQualityAssessmentIndexer, never()).buildIndex(eq(broken), any(), any());
        assertEquals(List.of("2"), List.copyOf(savedById.keySet()));
    }

    @Test
    public void shouldKeepChainingWhenOneDocumentFailsToBuild() {
        // given
        var first = assessment(1, 1, "PTCRIS", 0);
        var failing = assessment(2, 1, "PTCRIS", 5);
        var third = assessment(3, 1, "PTCRIS", 10);
        stubAssessments(first, failing, third);
        doThrow(new IllegalStateException("boom"))
            .when(dataQualityAssessmentIndexer).buildIndex(eq(failing), anyList(), any());

        // when
        dataQualityReindexService.reindexDataQualityAssessments();

        // then
        assertFalse(savedById.containsKey("2"));
        assertSuperseded(savedById.get("1"), dateOf(third));
        assertLatest(savedById.get("3"));
    }

    @Test
    public void shouldReindexWhenReindexEventIsReceived() {
        // given
        stubAssessments();

        // when
        dataQualityReindexService.handleDataQualityAssessmentReindexEvent(
            new DataQualityAssessmentReindexEvent());

        // then
        verify(dataQualityAssessmentIndexRepository).deleteAll();
        verify(dataQualityAssessmentRepository)
            .findAssessedEntityKeysAfter(any(), any(), any(Limit.class));
    }

    record DummyDTO(Integer id, String title) {
    }
}
