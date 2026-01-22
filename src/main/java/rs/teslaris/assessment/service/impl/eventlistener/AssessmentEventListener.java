package rs.teslaris.assessment.service.impl.eventlistener;

import java.time.LocalDate;
import java.util.Collections;
import java.util.List;
import java.util.Objects;
import lombok.RequiredArgsConstructor;
import org.springframework.context.event.EventListener;
import org.springframework.data.domain.Pageable;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;
import rs.teslaris.assessment.model.AssessmentMeasure;
import rs.teslaris.assessment.repository.AssessmentRulebookRepository;
import rs.teslaris.assessment.repository.classification.DocumentAssessmentClassificationRepository;
import rs.teslaris.assessment.service.interfaces.classification.DocumentAssessmentClassificationService;
import rs.teslaris.assessment.service.interfaces.classification.PersonAssessmentClassificationService;
import rs.teslaris.core.applicationevent.AllResearcherPointsReindexingEvent;
import rs.teslaris.core.applicationevent.EntityAssessmentChanged;
import rs.teslaris.core.applicationevent.MonographDateChanged;
import rs.teslaris.core.applicationevent.ResearcherPointsReindexingEvent;
import rs.teslaris.core.indexrepository.PersonIndexRepository;
import rs.teslaris.core.util.exceptionhandling.exception.NotFoundException;

@Component
@RequiredArgsConstructor
public class AssessmentEventListener {

    private final PersonIndexRepository personIndexRepository;

    private final AssessmentRulebookRepository assessmentRulebookRepository;

    private final PersonAssessmentClassificationService personAssessmentClassificationService;

    private final DocumentAssessmentClassificationService documentAssessmentClassificationService;

    private final DocumentAssessmentClassificationRepository
        documentAssessmentClassificationRepository;


    @Async
    @EventListener
    @Transactional(readOnly = true)
    protected void handleResearcherPointsReindexing(ResearcherPointsReindexingEvent event) {
        if (Objects.isNull(event.personIds()) || event.personIds().isEmpty()) {
            return;
        }

        var assessmentMeasures = loadAssessmentMeasures();

        event.personIds()
            .forEach(personId -> personIndexRepository.findByDatabaseId(personId)
                .ifPresent(personIndex ->
                    personAssessmentClassificationService.reindexPublicationPointsForResearcher(
                        personIndex, assessmentMeasures))
            );
    }

    @EventListener
    @Transactional(readOnly = true)
    protected void handleAllResearcherPointsReindexing(AllResearcherPointsReindexingEvent ignored) {
        personAssessmentClassificationService.reindexPublicationPointsForAllResearchers();
    }

    private List<AssessmentMeasure> loadAssessmentMeasures() {
        return assessmentRulebookRepository
            .readAssessmentMeasuresForRulebook(Pageable.unpaged(), findDefaultRulebookId())
            .getContent();
    }

    private Integer findDefaultRulebookId() {
        return assessmentRulebookRepository.findDefaultRulebook().orElseGet(
            () -> assessmentRulebookRepository.findById(1)
                .orElseThrow(() -> new NotFoundException("noRulebooksDefinedMessage"))).getId();
    }

    @EventListener
    @Transactional
    protected void handleMonographDateChanged(MonographDateChanged event) {
        documentAssessmentClassificationRepository.setUpdatedYearForDocumentAssessments(
            event.monographId(), event.year());
    }

    @EventListener
    @Async
    protected void handleEntityAssessmentChanged(EntityAssessmentChanged event) {
        var assessmentStartDate = LocalDate.of(2000, 1, 1);

        switch (event.entityType()) {
            case PUBLICATION_SERIES ->
                documentAssessmentClassificationService.classifyJournalPublications(
                    assessmentStartDate, event.commissionId(), Collections.emptyList(),
                    Collections.emptyList(), List.of(event.entityId()));
            case EVENT -> documentAssessmentClassificationService.classifyProceedingsPublications(
                assessmentStartDate, event.commissionId(), Collections.emptyList(),
                Collections.emptyList(), List.of(event.entityId()));
            case MONOGRAPH -> documentAssessmentClassificationService.classifyMonographPublications(
                assessmentStartDate, event.commissionId(), Collections.emptyList(),
                Collections.emptyList(), List.of(event.entityId()));
        }
    }
}
