package rs.teslaris.assessment.service.impl;

import java.util.List;
import java.util.function.BiConsumer;
import java.util.function.Function;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import rs.teslaris.assessment.repository.EventAssessmentClassificationRepository;
import rs.teslaris.assessment.repository.EventIndicatorRepository;
import rs.teslaris.assessment.repository.PublicationSeriesAssessmentClassificationRepository;
import rs.teslaris.assessment.repository.PublicationSeriesIndicatorRepository;
import rs.teslaris.assessment.service.interfaces.AssessmentMergeService;
import rs.teslaris.core.service.interfaces.document.ConferenceService;
import rs.teslaris.core.service.interfaces.document.JournalService;

@Service
@RequiredArgsConstructor
@Transactional
public class AssessmentMergeServiceImpl implements AssessmentMergeService {

    private final JournalService journalService;

    private final ConferenceService conferenceService;

    private final EventIndicatorRepository eventIndicatorRepository;

    private final EventAssessmentClassificationRepository eventAssessmentClassificationRepository;

    private final PublicationSeriesIndicatorRepository publicationSeriesIndicatorRepository;

    private final PublicationSeriesAssessmentClassificationRepository
        publicationSeriesAssessmentClassificationRepository;


    @Override
    public void switchAllIndicatorsToOtherJournal(Integer sourceId, Integer targetId) {
        processChunks(
            sourceId,
            (srcId, journalIndicator) -> {
                var existingIndicatorValue =
                    publicationSeriesIndicatorRepository.existsByPublicationSeriesIdAndSourceAndYearAndCategory(
                        targetId, journalIndicator.getSource(), journalIndicator.getFromDate(),
                        journalIndicator.getCategoryIdentifier(),
                        journalIndicator.getIndicator().getCode());
                existingIndicatorValue.ifPresent(publicationSeriesIndicatorRepository::delete);

                journalIndicator.setPublicationSeries(journalService.findJournalById(targetId));
            },
            pageRequest -> publicationSeriesIndicatorRepository.findIndicatorsForPublicationSeries(
                sourceId, pageRequest).getContent()
        );
    }

    @Override
    public void switchAllClassificationsToOtherJournal(Integer sourceId, Integer targetId) {
        processChunks(
            sourceId,
            (srcId, journalClassification) -> {
                var existingClassificationValue =
                    publicationSeriesAssessmentClassificationRepository.findClassificationForPublicationSeriesAndCategoryAndYearAndCommission(
                        targetId, journalClassification.getCategoryIdentifier(),
                        journalClassification.getClassificationYear(),
                        journalClassification.getCommission().getId());
                existingClassificationValue.ifPresent(
                    publicationSeriesAssessmentClassificationRepository::delete);

                journalClassification.setPublicationSeries(
                    journalService.findJournalById(targetId));
            },
            pageRequest -> publicationSeriesAssessmentClassificationRepository.findClassificationsForPublicationSeries(
                sourceId, pageRequest).getContent()
        );
    }

    @Override
    public void switchAllIndicatorsToOtherEvent(Integer sourceId, Integer targetId) {
        processChunks(
            sourceId,
            (srcId, eventIndicator) -> {
                var existingIndicatorValue =
                    eventIndicatorRepository.existsByEventIdAndSourceAndYear(
                        targetId, eventIndicator.getSource(), eventIndicator.getFromDate(),
                        eventIndicator.getIndicator().getCode());
                existingIndicatorValue.ifPresent(eventIndicatorRepository::delete);

                eventIndicator.setEvent(conferenceService.findConferenceById(targetId));
            },
            pageRequest -> eventIndicatorRepository.findIndicatorsForEvent(sourceId, pageRequest)
                .getContent()
        );
    }

    @Override
    public void switchAllClassificationsToOtherEvent(Integer sourceId, Integer targetId) {
        processChunks(
            sourceId,
            (srcId, eventClassification) -> {
                var existingClassificationValue =
                    eventAssessmentClassificationRepository.findAssessmentClassificationsForEventAndCommissionAndYear(
                        targetId, eventClassification.getCommission().getId(),
                        eventClassification.getClassificationYear());
                existingClassificationValue.ifPresent(
                    eventAssessmentClassificationRepository::delete);

                eventClassification.setEvent(conferenceService.findConferenceById(targetId));
            },
            pageRequest -> eventAssessmentClassificationRepository.findAssessmentClassificationsForEvent(
                sourceId, pageRequest).getContent()
        );
    }

    private <T> void processChunks(int sourceId,
                                   BiConsumer<Integer, T> switchOperation,
                                   Function<PageRequest, List<T>> fetchChunk) {
        var pageNumber = 0;
        var chunkSize = 10;
        var hasNextPage = true;

        while (hasNextPage) {
            List<T> chunk = fetchChunk.apply(PageRequest.of(pageNumber, chunkSize));

            chunk.forEach(item -> switchOperation.accept(sourceId, item));

            pageNumber++;
            hasNextPage = chunk.size() == chunkSize;
        }
    }
}
