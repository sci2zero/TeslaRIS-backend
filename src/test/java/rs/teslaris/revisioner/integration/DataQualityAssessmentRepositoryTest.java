package rs.teslaris.revisioner.integration;

import static org.junit.jupiter.api.Assertions.assertEquals;

import jakarta.persistence.EntityManager;
import java.time.Instant;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.data.domain.Limit;
import org.springframework.transaction.annotation.Transactional;
import rs.teslaris.revisioner.model.EntityRevision;
import rs.teslaris.revisioner.model.qualityassessment.AssessedEntityKey;
import rs.teslaris.revisioner.model.qualityassessment.DataQualityAssessment;
import rs.teslaris.revisioner.repository.DataQualityAssessmentRepository;
import rs.teslaris.revisioner.util.CompressionUtil;

/**
 * Runs the reindex keyset queries against Postgres: row-value comparison, DISTINCT with a
 * constructor expression, and reading large-object revision content through the fetch join.
 */
@SpringBootTest
@Transactional
public class DataQualityAssessmentRepositoryTest {

    // Sort after every real entity type, so data already in the database stays out of range.
    private static final String CURSOR_TYPE = "ZZZ_KEYSET";

    private static final String TYPE_A = "ZZZ_KEYSET_A";

    private static final String TYPE_B = "ZZZ_KEYSET_B";

    private static final Instant BASE_TIME = Instant.parse("2026-01-01T10:00:00Z");

    @Autowired
    private DataQualityAssessmentRepository repository;

    @Autowired
    private EntityManager entityManager;


    @BeforeEach
    public void setUp() {
        var a1 = persistRevision(TYPE_A, 1, "{\"a\":1}");
        var a2 = persistRevision(TYPE_A, 2, "{\"a\":2}");
        persistRevision(TYPE_A, 3, "{\"unassessed\":true}");
        var b1 = persistRevision(TYPE_B, 1, "{\"b\":1}");

        persistAssessment(a1, "PTCRIS", 10);
        persistAssessment(a1, "PTCRIS", 0);
        persistAssessment(a1, "OTHER", 5);
        persistAssessment(a1, "OTHER", 5);
        persistAssessment(a2, "PTCRIS", 0);
        persistAssessment(b1, "PTCRIS", 0);

        entityManager.flush();
        entityManager.clear();
    }

    private EntityRevision persistRevision(String entityType, int entityId, String json) {
        var revision = EntityRevision.builder()
            .entityType(entityType)
            .entityId(entityId)
            .revisionTimestamp(BASE_TIME)
            .contentHash("hash-" + entityType + "-" + entityId)
            .compressedContent(CompressionUtil.compress(json))
            .build();
        entityManager.persist(revision);
        return revision;
    }

    private void persistAssessment(EntityRevision revision, String profileName,
                                   int minutesAfterBase) {
        var finishedAt = BASE_TIME.plusSeconds(60L * minutesAfterBase);
        var assessment = DataQualityAssessment.builder()
            .revision(revision)
            .profileName(profileName)
            .profileVersion("1.0")
            .engineVersion("1.0.0")
            .startedAt(finishedAt)
            .finishedAt(finishedAt)
            .valid(true)
            .publicationCandidate(false)
            .passedRules(0)
            .infoFailedRules(0)
            .warningFailedRules(0)
            .errorFailedRules(0)
            .blockingFailedRules(0)
            .totalPoints(0.0)
            .achievedPointsNormalised(0.0)
            .qualityScore(0.0)
            .totalPointsFair(0.0)
            .achievedFairPointsNormalised(0.0)
            .qualityScoreFair(0.0)
            .activitiesCount(0)
            .build();
        entityManager.persist(assessment);
    }

    @Test
    public void shouldReturnAssessedEntityKeysAfterCursorInOrder() {
        var keys =
            repository.findAssessedEntityKeysAfter(CURSOR_TYPE, Integer.MIN_VALUE, Limit.of(10));

        assertEquals(List.of(
            new AssessedEntityKey(TYPE_A, 1),
            new AssessedEntityKey(TYPE_A, 2),
            new AssessedEntityKey(TYPE_B, 1)), keys);
    }

    @Test
    public void shouldHonourLimitAndResumeAfterLastKey() {
        var firstBatch =
            repository.findAssessedEntityKeysAfter(CURSOR_TYPE, Integer.MIN_VALUE, Limit.of(2));
        var last = firstBatch.getLast();
        var secondBatch =
            repository.findAssessedEntityKeysAfter(last.entityType(), last.entityId(),
                Limit.of(2));

        assertEquals(List.of(new AssessedEntityKey(TYPE_A, 1), new AssessedEntityKey(TYPE_A, 2)),
            firstBatch);
        assertEquals(List.of(new AssessedEntityKey(TYPE_B, 1)), secondBatch);
    }

    @Test
    public void shouldLoadRangeInChainOrderWithRevisionContent() {
        var assessments =
            repository.findAllWithRevisionForEntityRange(TYPE_A, 1, TYPE_A, 2);

        assertEquals(5, assessments.size());
        assertEquals(
            List.of("1/OTHER", "1/OTHER", "1/PTCRIS", "1/PTCRIS", "2/PTCRIS"),
            assessments.stream()
                .map(a -> a.getRevision().getEntityId() + "/" + a.getProfileName())
                .toList());

        // Equal finishedAt falls back to id, and the PTCRIS chain is oldest first.
        assertEquals(true, assessments.get(0).getId() < assessments.get(1).getId());
        assertEquals(true,
            assessments.get(2).getFinishedAt().isBefore(assessments.get(3).getFinishedAt()));

        assertEquals("{\"a\":1}",
            CompressionUtil.decompress(
                assessments.getFirst().getRevision().getCompressedContent()));
    }

    @Test
    public void shouldLoadInclusiveRangeAcrossEntityTypes() {
        var assessments =
            repository.findAllWithRevisionForEntityRange(TYPE_A, 2, TYPE_B, 1);

        assertEquals(List.of(TYPE_A + "/2", TYPE_B + "/1"),
            assessments.stream()
                .map(a -> a.getRevision().getEntityType() + "/" + a.getRevision().getEntityId())
                .toList());
    }
}
