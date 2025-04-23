package rs.teslaris.assessment.service.impl.statistics;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;
import java.util.function.BiConsumer;
import java.util.function.BiFunction;
import java.util.function.Consumer;
import java.util.function.Function;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.codehaus.janino.ExpressionEvaluator;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.elasticsearch.repository.ElasticsearchRepository;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import rs.teslaris.assessment.model.DocumentIndicator;
import rs.teslaris.assessment.model.EntityIndicator;
import rs.teslaris.assessment.model.OrganisationUnitIndicator;
import rs.teslaris.assessment.model.PersonIndicator;
import rs.teslaris.assessment.repository.DocumentIndicatorRepository;
import rs.teslaris.assessment.repository.OrganisationUnitIndicatorRepository;
import rs.teslaris.assessment.repository.PersonIndicatorRepository;
import rs.teslaris.assessment.service.interfaces.IndicatorService;
import rs.teslaris.assessment.service.interfaces.statistics.StatisticsService;
import rs.teslaris.assessment.util.IndicatorMappingConfigurationLoader;
import rs.teslaris.core.indexmodel.DocumentPublicationIndex;
import rs.teslaris.core.indexmodel.OrganisationUnitIndex;
import rs.teslaris.core.indexmodel.PersonIndex;
import rs.teslaris.core.indexmodel.statistics.StatisticsIndex;
import rs.teslaris.core.indexmodel.statistics.StatisticsType;
import rs.teslaris.core.indexrepository.DocumentPublicationIndexRepository;
import rs.teslaris.core.indexrepository.OrganisationUnitIndexRepository;
import rs.teslaris.core.indexrepository.PersonIndexRepository;
import rs.teslaris.core.indexrepository.statistics.StatisticsIndexRepository;
import rs.teslaris.core.model.document.Document;
import rs.teslaris.core.model.institution.OrganisationUnit;
import rs.teslaris.core.model.person.Person;
import rs.teslaris.core.repository.document.DocumentRepository;
import rs.teslaris.core.repository.person.OrganisationUnitRepository;
import rs.teslaris.core.repository.person.PersonRepository;
import rs.teslaris.core.util.FunctionalUtil;
import rs.teslaris.core.util.SessionUtil;
import rs.teslaris.core.util.exceptionhandling.exception.NotFoundException;

@Service
@RequiredArgsConstructor
@Slf4j
@Transactional
public class StatisticsServiceImpl implements StatisticsService {

    private final StatisticsIndexRepository statisticsIndexRepository;

    private final IndicatorService indicatorService;

    private final DocumentPublicationIndexRepository documentPublicationIndexRepository;

    private final PersonIndexRepository personIndexRepository;

    private final OrganisationUnitIndexRepository organisationUnitIndexRepository;

    private final DocumentIndicatorRepository documentIndicatorRepository;

    private final PersonIndicatorRepository personIndicatorRepository;

    private final OrganisationUnitIndicatorRepository organisationUnitIndicatorRepository;

    private final DocumentRepository documentRepository;

    private final PersonRepository personRepository;

    private final OrganisationUnitRepository organisationUnitRepository;


    @Override
    public List<String> fetchStatisticsTypeIndicators(StatisticsType statisticsType) {
        return IndicatorMappingConfigurationLoader.fetchStatisticsTypeIndicators(statisticsType);
    }

    @Override
    public void savePersonView(Integer personId) {
        var statisticsEntry = new StatisticsIndex();
        statisticsEntry.setPersonId(personId);
        saveView(statisticsEntry);
        log.info("STATISTICS - VIEW for Person with ID {} by {}.", personId,
            SessionUtil.getJSessionId());
    }

    @Override
    public void saveDocumentView(Integer documentId) {
        var statisticsEntry = new StatisticsIndex();
        statisticsEntry.setDocumentId(documentId);
        saveView(statisticsEntry);
        log.info("STATISTICS - VIEW for Document with ID {} by {}.", documentId,
            SessionUtil.getJSessionId());
    }

    @Override
    public void saveOrganisationUnitView(Integer organisationUnitId) {
        var statisticsEntry = new StatisticsIndex();
        statisticsEntry.setOrganisationUnitId(organisationUnitId);
        saveView(statisticsEntry);
        log.info("STATISTICS - VIEW for OrganisationUnit with ID {} by {}.", organisationUnitId,
            SessionUtil.getJSessionId());
    }

    @Override
    public void saveDocumentDownload(Integer documentId) {
        var statisticsEntry = new StatisticsIndex();
        statisticsEntry.setDocumentId(documentId);
        saveDownload(statisticsEntry);
        log.info("STATISTICS - DOWNLOAD for Document with ID {} by {}.", documentId,
            SessionUtil.getJSessionId());
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
        index.setSessionId(SessionUtil.getJSessionId());

        updateTotalCount(index);

        statisticsIndexRepository.save(index);
    }

    protected void updateTotalCount(StatisticsIndex index) {
        var loaderName = index.getType().equals(StatisticsType.VIEW.name()) ? "updateTotalViews" :
            "updateTotalDownloads";
        var indicatorCodes =
            IndicatorMappingConfigurationLoader.getIndicatorNameForLoaderMethodName(loaderName);
        var indicator = indicatorService.getIndicatorByCode(indicatorCodes.getFirst());

        if (Objects.isNull(indicator)) {
            log.error("Indicator not configured for loader: {}", loaderName);
            return;
        }

        if (Objects.nonNull(index.getDocumentId())) {
            updateIndicator(index.getDocumentId(),
                indicator.getCode(),
                documentRepository::findById,
                documentIndicatorRepository::findIndicatorForCodeAndDocumentId,
                (id) -> new DocumentIndicator(),
                entityIndicator -> {
                    entityIndicator.setNumericValue(entityIndicator.getNumericValue() + 1);
                    entityIndicator.setTimestamp(LocalDateTime.now());
                    documentIndicatorRepository.save(entityIndicator);
                });
        } else if (Objects.nonNull(index.getPersonId())) {
            updateIndicator(index.getPersonId(),
                indicator.getCode(),
                personRepository::findById,
                personIndicatorRepository::findIndicatorForCodeAndPersonId,
                (id) -> new PersonIndicator(),
                entityIndicator -> {
                    entityIndicator.setNumericValue(entityIndicator.getNumericValue() + 1);
                    entityIndicator.setTimestamp(LocalDateTime.now());
                    personIndicatorRepository.save(entityIndicator);
                });
        } else if (Objects.nonNull(index.getOrganisationUnitId())) {
            updateIndicator(index.getOrganisationUnitId(),
                indicator.getCode(),
                organisationUnitRepository::findById,
                organisationUnitIndicatorRepository::findIndicatorForCodeAndOrganisationUnitId,
                (id) -> new OrganisationUnitIndicator(),
                entityIndicator -> {
                    entityIndicator.setNumericValue(entityIndicator.getNumericValue() + 1);
                    entityIndicator.setTimestamp(LocalDateTime.now());
                    organisationUnitIndicatorRepository.save(entityIndicator);
                });
        }
    }

    @Scheduled(cron = "${statistics.schedule.views}")
    protected void updatePeriodViews() {
        var indicatorCodes =
            IndicatorMappingConfigurationLoader.getIndicatorNameForLoaderMethodName(
                "updatePeriodViews");

        var startPeriods = loadStartPeriodsForIndicators(indicatorCodes, StatisticsType.VIEW);

        updateStatisticsFromPeriod(startPeriods, indicatorCodes,
            StatisticsType.VIEW);
    }

    @Scheduled(cron = "${statistics.schedule.downloads}")
    protected void updatePeriodDownloads() {
        var indicatorCodes =
            IndicatorMappingConfigurationLoader.getIndicatorNameForLoaderMethodName(
                "updatePeriodDownloads");

        var startPeriods = loadStartPeriodsForIndicators(indicatorCodes, StatisticsType.DOWNLOAD);

        updateStatisticsFromPeriod(startPeriods, indicatorCodes,
            StatisticsType.DOWNLOAD);
    }

    private List<LocalDateTime> loadStartPeriodsForIndicators(List<String> indicatorCodes,
                                                              StatisticsType statisticsType) {
        List<LocalDateTime> startPeriods = new ArrayList<>();

        indicatorCodes.forEach(code -> {
            var indicator = indicatorService.getIndicatorByCode(code);

            if (Objects.isNull(indicator)) {
                log.error("Indicator not configured for loader: {}", code);
                throw new NotFoundException("Missing indicator for code: " + code);
            }

            try {
                var expression =
                    IndicatorMappingConfigurationLoader.getLocaleOffsetForStatisticsPeriod(
                        statisticsType, code);

                if (Objects.isNull(expression)) {
                    return; // continue loop
                }

                var fullExpression =
                    "import java.time.LocalDateTime;" + expression
                        .replace("import", "")
                        .replaceAll("[^a-zA-Z0-9(),.\\s]", "");

                var ee = new ExpressionEvaluator();
                ee.setReturnType(LocalDateTime.class);

                ee.cook(fullExpression);

                startPeriods.add((LocalDateTime) ee.evaluate(new Object[0]));
            } catch (Exception e) {
                log.error("Offset expression failed to run. Reason: {}", e.getMessage());
                throw new RuntimeException(e);
            }
        });

        return startPeriods;
    }

    private void updateStatisticsFromPeriod(List<LocalDateTime> startPeriods,
                                            List<String> indicatorCodes,
                                            StatisticsType statisticsType) {
        var documentTask = CompletableFuture.runAsync(() -> {
            updateEntityStatisticInPeriod(
                startPeriods,
                indicatorCodes,
                documentPublicationIndexRepository,
                documentRepository,
                (start, document) -> statisticsIndexRepository.countByTimestampBetweenAndTypeAndDocumentId(
                    start, LocalDateTime.now(), statisticsType.name(),
                    document.getDatabaseId()),
                DocumentPublicationIndex::getDatabaseId,
                documentIndicatorRepository::findIndicatorForCodeAndDocumentId,
                id -> new DocumentIndicator(),
                (entityIndicator, document) -> entityIndicator.setDocument(
                    documentRepository.findById(document.getDatabaseId()).orElseThrow()),
                documentIndicatorRepository
            );
        });

        var personTask = CompletableFuture.runAsync(() -> {
            updateEntityStatisticInPeriod(
                startPeriods,
                indicatorCodes,
                personIndexRepository,
                personRepository,
                (start, person) -> statisticsIndexRepository.countByTimestampBetweenAndTypeAndPersonId(
                    start, LocalDateTime.now(), statisticsType.name(), person.getDatabaseId()),
                PersonIndex::getDatabaseId,
                personIndicatorRepository::findIndicatorForCodeAndPersonId,
                id -> new PersonIndicator(),
                (entityIndicator, person) -> entityIndicator.setPerson(
                    personRepository.findById(person.getDatabaseId()).orElseThrow()),
                personIndicatorRepository
            );
        });

        var organisationTask = CompletableFuture.runAsync(() -> {
            updateEntityStatisticInPeriod(
                startPeriods,
                indicatorCodes,
                organisationUnitIndexRepository,
                organisationUnitRepository,
                (start, ou) -> statisticsIndexRepository.countByTimestampBetweenAndTypeAndOrganisationUnitId(
                    start, LocalDateTime.now(), statisticsType.name(), ou.getDatabaseId()),
                OrganisationUnitIndex::getDatabaseId,
                organisationUnitIndicatorRepository::findIndicatorForCodeAndOrganisationUnitId,
                id -> new OrganisationUnitIndicator(),
                (entityIndicator, ou) -> entityIndicator.setOrganisationUnit(
                    organisationUnitRepository.findById(ou.getDatabaseId()).orElseThrow()),
                organisationUnitIndicatorRepository
            );
        });

        var allTasks = CompletableFuture.allOf(documentTask, personTask, organisationTask);

        try {
            allTasks.get();
        } catch (InterruptedException | ExecutionException e) {
            log.error("Error while updating statistics for " + statisticsType.name(), e);
            Thread.currentThread().interrupt();
        }
    }

    private <T, D, I extends EntityIndicator> void updateEntityStatisticInPeriod(
        List<LocalDateTime> startPeriods,
        List<String> indicatorCodes,
        ElasticsearchRepository<T, String> indexRepository,
        JpaRepository<D, Integer> entityRepository,
        BiFunction<LocalDateTime, T, Integer> countFunction,
        Function<T, Integer> getEntityId,
        BiFunction<String, Integer, Optional<I>> findIndicatorByEntityId,
        Function<Integer, I> createNewIndicator,
        BiConsumer<I, T> setEntity,
        JpaRepository<I, Integer> entityIndicatorRepository) {

        int pageNumber = 0;
        int chunkSize = 50;
        boolean hasNextPage = true;

        while (hasNextPage) {
            List<T> chunk =
                indexRepository.findAll(PageRequest.of(pageNumber, chunkSize)).getContent();

            chunk.forEach(entity -> {
                Integer entityId = getEntityId.apply(entity);

                FunctionalUtil.forEachWithCounter(indicatorCodes, (i, indicatorCode) -> {
                    Integer statisticsCount = countFunction.apply(startPeriods.get(i), entity);

                    updateIndicator(entityId,
                        indicatorCode,
                        entityRepository::findById,
                        findIndicatorByEntityId,
                        id -> {
                            I indicator = createNewIndicator.apply(id);
                            setEntity.accept(indicator, entity);
                            return indicator;
                        },
                        entityIndicator -> {
                            entityIndicator.setNumericValue(Double.valueOf(statisticsCount));
                            entityIndicator.setTimestamp(LocalDateTime.now());
                            entityIndicator.setFromDate(startPeriods.get(i).toLocalDate());
                            entityIndicator.setToDate(LocalDate.now());
                            entityIndicatorRepository.save(entityIndicator);
                        });
                });

            });

            pageNumber++;
            hasNextPage = chunk.size() == chunkSize;
        }
    }

    private <T, R> void updateIndicator(Integer id,
                                        String indicatorCode,
                                        Function<Integer, Optional<T>> findEntityById,
                                        BiFunction<String, Integer, Optional<R>> findIndicator,
                                        Function<Integer, R> createIndicator,
                                        Consumer<R> updateAndSaveIndicatorValue) {

        var optionalIndicator = findIndicator.apply(indicatorCode, id);
        R indicatorEntity;
        if (optionalIndicator.isEmpty()) {
            indicatorEntity = createIndicator.apply(id);
            if (indicatorEntity instanceof DocumentIndicator) {
                ((DocumentIndicator) indicatorEntity).setDocument(
                    (Document) findEntityById.apply(id).orElseThrow());
            } else if (indicatorEntity instanceof PersonIndicator) {
                ((PersonIndicator) indicatorEntity).setPerson(
                    (Person) findEntityById.apply(id).orElseThrow());
            } else if (indicatorEntity instanceof OrganisationUnitIndicator) {
                ((OrganisationUnitIndicator) indicatorEntity).setOrganisationUnit(
                    (OrganisationUnit) findEntityById.apply(id).orElseThrow());
            }
            ((EntityIndicator) indicatorEntity).setIndicator(
                indicatorService.getIndicatorByCode(indicatorCode));
            ((EntityIndicator) indicatorEntity).setNumericValue(0.0);
        } else {
            indicatorEntity = optionalIndicator.get();
        }

        updateAndSaveIndicatorValue.accept(indicatorEntity);
    }
}
