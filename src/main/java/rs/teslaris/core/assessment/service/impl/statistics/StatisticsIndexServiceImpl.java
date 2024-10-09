package rs.teslaris.core.assessment.service.impl.statistics;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Objects;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.PageRequest;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import rs.teslaris.core.assessment.model.DocumentIndicator;
import rs.teslaris.core.assessment.repository.DocumentIndicatorRepository;
import rs.teslaris.core.assessment.repository.OrganisationUnitIndicatorRepository;
import rs.teslaris.core.assessment.repository.PersonIndicatorRepository;
import rs.teslaris.core.assessment.service.interfaces.IndicatorService;
import rs.teslaris.core.assessment.service.interfaces.statistics.StatisticsIndexService;
import rs.teslaris.core.assessment.util.IndicatorMappingConfigurationLoader;
import rs.teslaris.core.indexmodel.DocumentPublicationIndex;
import rs.teslaris.core.indexmodel.statistics.StatisticsIndex;
import rs.teslaris.core.indexmodel.statistics.StatisticsType;
import rs.teslaris.core.indexrepository.DocumentPublicationIndexRepository;
import rs.teslaris.core.indexrepository.statistics.StatisticsIndexRepository;
import rs.teslaris.core.repository.document.DocumentRepository;

@Service
@RequiredArgsConstructor
@Slf4j
@Transactional
public class StatisticsIndexServiceImpl implements StatisticsIndexService {

    private final StatisticsIndexRepository statisticsIndexRepository;

    private final IndicatorService indicatorService;

    private final DocumentPublicationIndexRepository documentPublicationIndexRepository;

    private final DocumentIndicatorRepository documentIndicatorRepository;

    private final DocumentRepository documentRepository;

    private final PersonIndicatorRepository personIndicatorRepository;

    private final OrganisationUnitIndicatorRepository organisationUnitIndicatorRepository;


    @Override
    public void savePersonView(Integer personId) {
        var statisticsEntry = new StatisticsIndex();
        statisticsEntry.setPersonId(personId);
        saveView(statisticsEntry);
    }

    @Override
    public void saveDocumentView(Integer documentId) {
        var statisticsEntry = new StatisticsIndex();
        statisticsEntry.setDocumentId(documentId);
        saveView(statisticsEntry);
    }

    @Override
    public void saveOrganisationUnitView(Integer organisationUnitId) {
        var statisticsEntry = new StatisticsIndex();
        statisticsEntry.setOrganisationUnitId(organisationUnitId);
        saveView(statisticsEntry);
    }

    @Override
    public void saveDocumentDownload(Integer documentId) {
        var statisticsEntry = new StatisticsIndex();
        statisticsEntry.setDocumentId(documentId);
        saveDownload(statisticsEntry);
    }

    private void saveView(StatisticsIndex index) {
        index.setType(StatisticsType.VIEW.name());
        save(index);
    }

    private void saveDownload(StatisticsIndex index) {
        index.setType(StatisticsType.DOWNLOAD.name());
        save(index);
    }

    private void save(StatisticsIndex index) {
        index.setTimestamp(LocalDateTime.now());

        updateTotalViews(index);

        statisticsIndexRepository.save(index);
    }

    protected void updateTotalViews(StatisticsIndex index) {
        var indicatorCode = IndicatorMappingConfigurationLoader.getIndicatorNameForLoaderMethodName(
            "updateTotalViews");
        var indicator = indicatorService.getIndicatorByCode(indicatorCode);

        if (Objects.isNull(indicator)) {
            log.error("Indicator not configured for loader: {}", "updateDailyViews");
            return;
        }

        if (Objects.nonNull(index.getDocumentId())) {
            var documentIndicator =
                documentIndicatorRepository.findIndicatorForCodeAndDocumentId(indicatorCode,
                    index.getDocumentId());
            if (Objects.isNull(documentIndicator)) {
                documentIndicator = new DocumentIndicator();
                documentIndicator.setDocument(
                    documentRepository.findById(index.getDocumentId()).get());
                documentIndicator.setIndicator(indicator);
                documentIndicator.setNumericValue(0.0);
            }

            documentIndicator.setNumericValue(documentIndicator.getNumericValue() + 1d);
            documentIndicator.setTimestamp(LocalDateTime.now());

            documentIndicatorRepository.save(documentIndicator);
        }
        // TODO: add other types
    }

    @Scheduled(cron = "${statistics.schedule.dailyViews}")
    protected void updateDailyViews() {
        var indicatorCode = IndicatorMappingConfigurationLoader.getIndicatorNameForLoaderMethodName(
            "updateDailyViews");
        var indicator = indicatorService.getIndicatorByCode(indicatorCode);

        if (Objects.isNull(indicator)) {
            log.error("Indicator not configured for loader: {}", "updateDailyViews");
            return;
        }

        LocalDateTime now = LocalDateTime.now();
        LocalDateTime last24Hours = now.minusHours(24);

        int pageNumber = 0;
        int chunkSize = 50;
        boolean hasNextPage = true;

        while (hasNextPage) {

            List<DocumentPublicationIndex> chunk =
                documentPublicationIndexRepository.findAll(PageRequest.of(pageNumber, chunkSize))
                    .getContent();

            chunk.forEach(document -> {
                var viewCount =
                    statisticsIndexRepository.countByTimestampBetweenAndTypeAndDocumentId(
                        last24Hours,
                        now, StatisticsType.VIEW.name(),
                        document.getDatabaseId());

                var documentIndicator =
                    documentIndicatorRepository.findIndicatorForCodeAndDocumentId(indicatorCode,
                        document.getDatabaseId());
                if (Objects.isNull(documentIndicator)) {
                    documentIndicator = new DocumentIndicator();
                    documentIndicator.setDocument(
                        documentRepository.findById(document.getDatabaseId()).get());
                    documentIndicator.setIndicator(indicator);
                }

                documentIndicator.setNumericValue(Double.valueOf(viewCount));
                documentIndicator.setTimestamp(LocalDateTime.now());

                documentIndicatorRepository.save(documentIndicator);

                documentIndicatorRepository.save(documentIndicator);
            });

            pageNumber++;
            hasNextPage = chunk.size() == chunkSize;
        }
    }
}
