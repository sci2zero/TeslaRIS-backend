package rs.teslaris.assessment.service.impl.eventlistener;

import com.google.common.util.concurrent.Striped;
import java.time.LocalDate;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Objects;
import java.util.concurrent.locks.Lock;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import net.javacrumbs.shedlock.spring.annotation.SchedulerLock;
import org.springframework.context.event.EventListener;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Sort;
import org.springframework.scheduling.annotation.Async;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;
import rs.teslaris.assessment.model.classification.AssessmentBuffer;
import rs.teslaris.assessment.repository.classification.AssessmentBufferRepository;
import rs.teslaris.assessment.repository.classification.DocumentAssessmentClassificationRepository;
import rs.teslaris.assessment.service.interfaces.classification.DocumentAssessmentClassificationService;
import rs.teslaris.assessment.service.interfaces.classification.PersonAssessmentClassificationService;
import rs.teslaris.core.applicationevent.AllResearcherPointsReindexingEvent;
import rs.teslaris.core.applicationevent.EntityAssessmentChanged;
import rs.teslaris.core.applicationevent.MonographDateChanged;
import rs.teslaris.core.applicationevent.ResearcherPointsReindexingEvent;
import rs.teslaris.core.indexmodel.DocumentPublicationIndex;
import rs.teslaris.core.indexmodel.DocumentPublicationType;
import rs.teslaris.core.indexrepository.DocumentPublicationIndexRepository;
import rs.teslaris.core.util.functional.FunctionalUtil;

@Component
@RequiredArgsConstructor
@Slf4j
public class AssessmentEventListener {

    private final PersonAssessmentClassificationService personAssessmentClassificationService;

    private final DocumentAssessmentClassificationService documentAssessmentClassificationService;

    private final DocumentAssessmentClassificationRepository
        documentAssessmentClassificationRepository;

    private final DocumentPublicationIndexRepository documentPublicationIndexRepository;

    private final AssessmentBufferRepository assessmentBufferRepository;

    private final Striped<Lock> locks = Striped.lock(1024);


    @EventListener
    @SchedulerLock(name = "researcher-point-reindex")
    @Transactional
    protected void handleResearcherPointsReindexing(ResearcherPointsReindexingEvent event) {
        if (Objects.isNull(event.personIds()) || event.personIds().isEmpty()) {
            return;
        }

        var assessmentBuffer =
            assessmentBufferRepository.findById(1).orElse(new AssessmentBuffer());

        assessmentBuffer.getPersonIdsToReindex().addAll(event.personIds());
        assessmentBufferRepository.save(assessmentBuffer);
    }

    @Scheduled(cron = "0 0 */2 * * *")
    @SchedulerLock(name = "researcher-point-reindex")
    protected void performBufferedResearcherPointReindexing() {
        long start = System.nanoTime();
        log.info("performBufferedResearcherPointReindexing started at {}", System.nanoTime());

        var buffer = assessmentBufferRepository.findById(1).orElse(new AssessmentBuffer());

        var idsToProcess = new HashSet<>(buffer.getPersonIdsToReindex()); // atomic snapshot
        buffer.getPersonIdsToReindex().removeAll(idsToProcess);
        assessmentBufferRepository.save(buffer);

        if (idsToProcess.isEmpty()) {
            log.info(
                "performBufferedResearcherPointReindexing exited due to empty wait list. Took {} ms",
                (System.nanoTime() - start) / 1_000_000.0);
            return;
        }

        personAssessmentClassificationService.reindexPublicationPointsForAllResearchers(
            idsToProcess.stream().toList(), Collections.emptyList());

        log.info("performBufferedResearcherPointReindexing took {} ms",
            (System.nanoTime() - start) / 1_000_000.0);
    }

    @EventListener
    @SchedulerLock(name = "researcher-point-reindex")
    protected void handleAllResearcherPointsReindexing(AllResearcherPointsReindexingEvent ignored) {
        long start = System.nanoTime();
        log.info("handleAllResearcherPointsReindexing started at {}", System.nanoTime());

        assessmentBufferRepository.findById(1).ifPresent(assessmentBuffer -> {
            assessmentBuffer.getPersonIdsToReindex().clear();
            assessmentBufferRepository.save(assessmentBuffer);
        });

        personAssessmentClassificationService.reindexPublicationPointsForAllResearchers(
            Collections.emptyList(), Collections.emptyList());

        log.info("handleAllResearcherPointsReindexing took {} ms",
            (System.nanoTime() - start) / 1_000_000.0);
    }

    @Scheduled(cron = "${assessment.person.assessment-points}")
    @SchedulerLock(name = "researcher-point-reindex")
    @Transactional(readOnly = true)
    protected void reindexResearcherPointsScheduled() {
        long start = System.nanoTime();
        log.info("reindexResearcherPointsScheduled started at {}", System.nanoTime());

        personAssessmentClassificationService.reindexPublicationPointsForAllResearchers(
            Collections.emptyList(), Collections.emptyList());

        log.info("reindexResearcherPointsScheduled took {} ms",
            (System.nanoTime() - start) / 1_000_000.0);
    }

    @EventListener
    @Transactional
    protected void handleMonographDateChanged(MonographDateChanged event) {
        documentAssessmentClassificationRepository.setUpdatedYearForDocumentAssessments(
            event.monographId(), event.year());
    }

    @EventListener
    @Async("taskExecutor")
    protected void handleEntityAssessmentChanged(EntityAssessmentChanged event) {
        log.info(
            "Starting entity assessment change processing for entityType={}, entityId={}, commissionId={}, deleted={}",
            event.entityType(), event.entityId(), event.commissionId(), event.deleted());

        var assessmentStartDate = LocalDate.of(2000, 1, 1);

        withLock(event.commissionId(), () -> {
            try {
                if (event.deleted()) {
                    log.info("Processing DELETE operation for entityType={}, entityId={}",
                        event.entityType(), event.entityId());

                    String publicationType;
                    switch (event.entityType()) {
                        case PUBLICATION_SERIES ->
                            publicationType = DocumentPublicationType.JOURNAL_PUBLICATION.name();
                        case EVENT -> publicationType =
                            DocumentPublicationType.PROCEEDINGS_PUBLICATION.name();
                        case MONOGRAPH ->
                            publicationType = DocumentPublicationType.MONOGRAPH_PUBLICATION.name();
                        default -> {
                            log.warn("Unsupported entityType={} for delete operation, skipping",
                                event.entityType());
                            return;
                        }
                    }

                    var finalPublicationType = publicationType;
                    log.info("Fetching document IDs for deletion (type={}, entityId={})",
                        finalPublicationType, event.entityId());

                    long startTime = System.currentTimeMillis();
                    var allIds = FunctionalUtil.mapAllPages(
                        1000,
                        Sort.by("databaseId"),
                        pageRequest ->
                            switch (event.entityType()) {
                                case PUBLICATION_SERIES ->
                                    documentPublicationIndexRepository.findByTypeAndJournalId(
                                        finalPublicationType, event.entityId(), pageRequest);
                                case EVENT ->
                                    documentPublicationIndexRepository.findByTypeAndEventId(
                                        finalPublicationType, event.entityId(), pageRequest);
                                case MONOGRAPH ->
                                    documentPublicationIndexRepository.findByTypeAndMonographId(
                                        finalPublicationType, event.entityId(), pageRequest);
                                default -> {
                                    log.error("Unexpected entity type in page fetch: {}",
                                        event.entityType());
                                    yield Page.empty();
                                }
                            },
                        DocumentPublicationIndex::getDatabaseId
                    );

                    long fetchTime = System.currentTimeMillis() - startTime;
                    log.info("Fetched {} document IDs in {} ms for entityId={}",
                        allIds.size(), fetchTime, event.entityId());

                    if (!allIds.isEmpty()) {
                        log.info(
                            "Deleting assessment classifications for {} documents, commissionId={}",
                            allIds.size(), event.commissionId());

                        startTime = System.currentTimeMillis();
                        documentAssessmentClassificationRepository
                            .deleteByDocumentIdsAndCommissionId(allIds, event.commissionId(),
                                false);

                        long deleteTime = System.currentTimeMillis() - startTime;
                        log.info("Deleted {} assessment classifications in {} ms",
                            allIds.size(), deleteTime);
                    } else {
                        log.info("No documents found to delete for entityId={}", event.entityId());
                    }
                } else {
                    log.info("Processing ADD/UPDATE operation for entityType={}, entityId={}",
                        event.entityType(), event.entityId());
                }

                log.info("Starting classification for entityType={}, entityId={}, commissionId={}",
                    event.entityType(), event.entityId(), event.commissionId());

                long classificationStartTime = System.currentTimeMillis();
                switch (event.entityType()) {
                    case PUBLICATION_SERIES ->
                        documentAssessmentClassificationService.classifyJournalPublications(
                            assessmentStartDate, event.commissionId(), Collections.emptyList(),
                            Collections.emptyList(), List.of(event.entityId()), false);
                    case EVENT ->
                        documentAssessmentClassificationService.classifyProceedingsPublications(
                            assessmentStartDate, event.commissionId(), Collections.emptyList(),
                            Collections.emptyList(), List.of(event.entityId()), false);
                    case MONOGRAPH ->
                        documentAssessmentClassificationService.classifyMonographPublications(
                            assessmentStartDate, event.commissionId(), Collections.emptyList(),
                            Collections.emptyList(), List.of(event.entityId()), false);
                    default -> log.warn("Unhandled entityType={} in classification switch",
                        event.entityType());
                }

                long classificationTime = System.currentTimeMillis() - classificationStartTime;
                log.info("Classification completed in {} ms for entityType={}, entityId={}",
                    classificationTime, event.entityType(), event.entityId());

            } catch (Exception e) {
                log.error(
                    "Error processing EntityAssessmentChanged event: entityType={}, entityId={}, commissionId={}",
                    event.entityType(), event.entityId(), event.commissionId(), e);
            } finally {
                log.info("Completed processing for entityType={}, entityId={}, commissionId={}",
                    event.entityType(), event.entityId(), event.commissionId());
            }
        });
    }

    public void withLock(Integer commissionId, Runnable action) {
        Lock lock = locks.get(commissionId);
        lock.lock();
        log.info("🔐 Acquired lock for commissionId={}", commissionId);
        try {
            action.run();
        } finally {
            lock.unlock();
            log.info("🔐 Released lock for commissionId={}", commissionId);
        }
    }
}
