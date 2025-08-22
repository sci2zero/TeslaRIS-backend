package rs.teslaris.assessment.service.impl.statistics;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.BiConsumer;
import java.util.function.BiFunction;
import java.util.function.Consumer;
import java.util.function.Function;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.codehaus.janino.ExpressionEvaluator;
import org.springframework.context.annotation.Primary;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.elasticsearch.repository.ElasticsearchRepository;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import rs.teslaris.assessment.model.indicator.DocumentIndicator;
import rs.teslaris.assessment.model.indicator.EntityIndicator;
import rs.teslaris.assessment.model.indicator.EventIndicator;
import rs.teslaris.assessment.model.indicator.OrganisationUnitIndicator;
import rs.teslaris.assessment.model.indicator.PersonIndicator;
import rs.teslaris.assessment.model.indicator.PublicationSeriesIndicator;
import rs.teslaris.assessment.repository.indicator.DocumentIndicatorRepository;
import rs.teslaris.assessment.repository.indicator.EventIndicatorRepository;
import rs.teslaris.assessment.repository.indicator.OrganisationUnitIndicatorRepository;
import rs.teslaris.assessment.repository.indicator.PersonIndicatorRepository;
import rs.teslaris.assessment.repository.indicator.PublicationSeriesIndicatorRepository;
import rs.teslaris.assessment.service.interfaces.indicator.IndicatorService;
import rs.teslaris.assessment.service.interfaces.statistics.StatisticsService;
import rs.teslaris.assessment.util.IndicatorMappingConfigurationLoader;
import rs.teslaris.core.indexmodel.BookSeriesIndex;
import rs.teslaris.core.indexmodel.DocumentPublicationIndex;
import rs.teslaris.core.indexmodel.EventIndex;
import rs.teslaris.core.indexmodel.JournalIndex;
import rs.teslaris.core.indexmodel.OrganisationUnitIndex;
import rs.teslaris.core.indexmodel.PersonIndex;
import rs.teslaris.core.indexmodel.statistics.StatisticsIndex;
import rs.teslaris.core.indexmodel.statistics.StatisticsType;
import rs.teslaris.core.indexrepository.BookSeriesIndexRepository;
import rs.teslaris.core.indexrepository.DocumentPublicationIndexRepository;
import rs.teslaris.core.indexrepository.EventIndexRepository;
import rs.teslaris.core.indexrepository.JournalIndexRepository;
import rs.teslaris.core.indexrepository.OrganisationUnitIndexRepository;
import rs.teslaris.core.indexrepository.PersonIndexRepository;
import rs.teslaris.core.indexrepository.statistics.StatisticsIndexRepository;
import rs.teslaris.core.model.document.Document;
import rs.teslaris.core.model.document.Event;
import rs.teslaris.core.model.document.PublicationSeries;
import rs.teslaris.core.model.institution.OrganisationUnit;
import rs.teslaris.core.model.person.Person;
import rs.teslaris.core.repository.document.BookSeriesRepository;
import rs.teslaris.core.repository.document.DocumentRepository;
import rs.teslaris.core.repository.document.EventRepository;
import rs.teslaris.core.repository.document.PublicationSeriesRepository;
import rs.teslaris.core.repository.institution.OrganisationUnitRepository;
import rs.teslaris.core.repository.person.PersonRepository;
import rs.teslaris.core.service.interfaces.document.DocumentDownloadTracker;
import rs.teslaris.core.util.deduplication.Mergeable;
import rs.teslaris.core.util.exceptionhandling.exception.NotFoundException;
import rs.teslaris.core.util.functional.FunctionalUtil;
import rs.teslaris.core.util.tracing.SessionTrackingUtil;

@Service
@Primary
@RequiredArgsConstructor
@Slf4j
@Transactional
public class StatisticsServiceImpl implements StatisticsService, DocumentDownloadTracker {

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

    private final PublicationSeriesRepository publicationSeriesRepository;

    private final PublicationSeriesIndicatorRepository publicationSeriesIndicatorRepository;

    private final EventRepository eventRepository;

    private final EventIndicatorRepository eventIndicatorRepository;

    private final JournalIndexRepository journalIndexRepository;

    private final EventIndexRepository eventIndexRepository;

    private final BookSeriesRepository bookSeriesRepository;

    private final BookSeriesIndexRepository bookSeriesIndexRepository;


    @Override
    public List<String> fetchStatisticsTypeIndicators(StatisticsType statisticsType) {
        return IndicatorMappingConfigurationLoader.fetchStatisticsTypeIndicators(statisticsType);
    }

    @Override
    public void savePublicationSeriesView(Integer publicationSeriesId) {
        var statisticsEntry = new StatisticsIndex();
        statisticsEntry.setPublicationSeriesId(publicationSeriesId);
        saveView(statisticsEntry);
        log.info(
            "STATISTICS - CONTEXT: {} - TRACKING_COOKIE: {} - IP: {} - TYPE: PUBLICATION_SERIES_VIEW - ID: {}",
            SessionTrackingUtil.getCurrentTracingContextId(),
            SessionTrackingUtil.getJSessionId(),
            SessionTrackingUtil.getCurrentClientIP(),
            publicationSeriesId
        );
    }

    @Override
    public void saveEventView(Integer eventId) {
        var statisticsEntry = new StatisticsIndex();
        statisticsEntry.setEventId(eventId);
        saveView(statisticsEntry);
        log.info(
            "STATISTICS - CONTEXT: {} - TRACKING_COOKIE: {} - IP: {} - TYPE: EVENT_VIEW - ID: {}",
            SessionTrackingUtil.getCurrentTracingContextId(),
            SessionTrackingUtil.getJSessionId(),
            SessionTrackingUtil.getCurrentClientIP(),
            eventId
        );
    }

    @Override
    public void savePersonView(Integer personId) {
        var statisticsEntry = new StatisticsIndex();
        statisticsEntry.setPersonId(personId);
        saveView(statisticsEntry);
        log.info(
            "STATISTICS - CONTEXT: {} - TRACKING_COOKIE: {} - IP: {} - TYPE: PERSON_VIEW - ID: {}",
            SessionTrackingUtil.getCurrentTracingContextId(),
            SessionTrackingUtil.getJSessionId(),
            SessionTrackingUtil.getCurrentClientIP(),
            personId
        );
    }

    @Override
    public void saveDocumentView(Integer documentId) {
        var statisticsEntry = new StatisticsIndex();
        statisticsEntry.setDocumentId(documentId);
        saveView(statisticsEntry);
        log.info(
            "STATISTICS - CONTEXT: {} - TRACKING_COOKIE: {} - IP: {} - TYPE: DOCUMENT_VIEW - ID: {}",
            SessionTrackingUtil.getCurrentTracingContextId(),
            SessionTrackingUtil.getJSessionId(),
            SessionTrackingUtil.getCurrentClientIP(),
            documentId
        );
    }

    @Override
    public void saveOrganisationUnitView(Integer organisationUnitId) {
        var statisticsEntry = new StatisticsIndex();
        statisticsEntry.setOrganisationUnitId(organisationUnitId);
        saveView(statisticsEntry);
        log.info(
            "STATISTICS - CONTEXT: {} - TRACKING_COOKIE: {} - IP: {} - TYPE: ORGANISATION_UNIT_VIEW - ID: {}",
            SessionTrackingUtil.getCurrentTracingContextId(),
            SessionTrackingUtil.getJSessionId(),
            SessionTrackingUtil.getCurrentClientIP(),
            organisationUnitId
        );
    }

    @Override
    public void saveDocumentDownload(Integer documentId) {
        var statisticsEntry = new StatisticsIndex();
        statisticsEntry.setDocumentId(documentId);
        saveDownload(statisticsEntry);
        log.info(
            "STATISTICS - CONTEXT: {} - TRACKING_COOKIE: {} - IP: {} - TYPE: DOCUMENT_DOWNLOAD - ID: {}",
            SessionTrackingUtil.getCurrentTracingContextId(),
            SessionTrackingUtil.getJSessionId(),
            SessionTrackingUtil.getCurrentClientIP(),
            documentId
        );
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
        index.setSessionId(SessionTrackingUtil.getJSessionId());

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
        } else if (Objects.nonNull(index.getPublicationSeriesId())) {
            updateIndicator(index.getPublicationSeriesId(),
                indicator.getCode(),
                publicationSeriesRepository::findById,
                publicationSeriesIndicatorRepository::findIndicatorForCodeAndPublicationSeriesId,
                (id) -> new PublicationSeriesIndicator(),
                entityIndicator -> {
                    entityIndicator.setNumericValue(entityIndicator.getNumericValue() + 1);
                    entityIndicator.setTimestamp(LocalDateTime.now());
                    publicationSeriesIndicatorRepository.save(entityIndicator);
                });
        } else if (Objects.nonNull(index.getEventId())) {
            updateIndicator(index.getEventId(),
                indicator.getCode(),
                eventRepository::findById,
                eventIndicatorRepository::findIndicatorsForCodeAndEventId,
                (id) -> new EventIndicator(),
                entityIndicator -> {
                    entityIndicator.setNumericValue(entityIndicator.getNumericValue() + 1);
                    entityIndicator.setTimestamp(LocalDateTime.now());
                    eventIndicatorRepository.save(entityIndicator);
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
                (start, ids) -> {
                    var totalCount = new AtomicInteger(0);
                    ids.forEach(id -> {
                        totalCount.addAndGet(
                            statisticsIndexRepository.countByTimestampBetweenAndTypeAndDocumentId(
                                start, LocalDateTime.now(), statisticsType.name(), id));
                    });
                    return totalCount.get();
                },
                DocumentPublicationIndex::getDatabaseId,
                documentIndicatorRepository::findIndicatorForCodeAndDocumentId,
                id -> new DocumentIndicator(),
                DocumentIndicator::setDocument,
                documentIndicatorRepository,
                "Document"
            );
        });

        var personTask = CompletableFuture.runAsync(() -> {
            updateEntityStatisticInPeriod(
                startPeriods,
                indicatorCodes,
                personIndexRepository,
                personRepository,
                (start, ids) -> {
                    var totalCount = new AtomicInteger(0);
                    ids.forEach(id -> {
                        totalCount.addAndGet(
                            statisticsIndexRepository.countByTimestampBetweenAndTypeAndPersonId(
                                start, LocalDateTime.now(), statisticsType.name(), id));
                    });
                    return totalCount.get();
                },
                PersonIndex::getDatabaseId,
                personIndicatorRepository::findIndicatorForCodeAndPersonId,
                id -> new PersonIndicator(),
                PersonIndicator::setPerson,
                personIndicatorRepository,
                "Person"
            );
        });

        var organisationTask = CompletableFuture.runAsync(() -> {
            updateEntityStatisticInPeriod(
                startPeriods,
                indicatorCodes,
                organisationUnitIndexRepository,
                organisationUnitRepository,
                (start, ids) -> {
                    var totalCount = new AtomicInteger(0);
                    ids.forEach(id -> {
                        totalCount.addAndGet(
                            statisticsIndexRepository.countByTimestampBetweenAndTypeAndOrganisationUnitId(
                                start, LocalDateTime.now(), statisticsType.name(), id));
                    });
                    return totalCount.get();
                },
                OrganisationUnitIndex::getDatabaseId,
                organisationUnitIndicatorRepository::findIndicatorForCodeAndOrganisationUnitId,
                id -> new OrganisationUnitIndicator(),
                OrganisationUnitIndicator::setOrganisationUnit,
                organisationUnitIndicatorRepository,
                "OrganisationUnit"
            );
        });

        var journalsTask = CompletableFuture.runAsync(() -> {
            updateEntityStatisticInPeriod(
                startPeriods,
                indicatorCodes,
                journalIndexRepository,
                publicationSeriesRepository,
                (start, ids) -> {
                    var totalCount = new AtomicInteger(0);
                    ids.forEach(id -> {
                        totalCount.addAndGet(
                            statisticsIndexRepository.countByTimestampBetweenAndTypeAndPublicationSeriesId(
                                start, LocalDateTime.now(), statisticsType.name(), id));
                    });
                    return totalCount.get();
                },
                JournalIndex::getDatabaseId,
                publicationSeriesIndicatorRepository::findIndicatorForCodeAndPublicationSeriesId,
                id -> new PublicationSeriesIndicator(),
                PublicationSeriesIndicator::setPublicationSeries,
                publicationSeriesIndicatorRepository,
                "Journal"
            );
        });

        var bookSeriesTask = CompletableFuture.runAsync(() -> {
            updateEntityStatisticInPeriod(
                startPeriods,
                indicatorCodes,
                bookSeriesIndexRepository,
                bookSeriesRepository,
                (start, ids) -> {
                    var totalCount = new AtomicInteger(0);
                    ids.forEach(id -> {
                        totalCount.addAndGet(
                            statisticsIndexRepository.countByTimestampBetweenAndTypeAndPublicationSeriesId(
                                start, LocalDateTime.now(), statisticsType.name(), id));
                    });
                    return totalCount.get();
                },
                BookSeriesIndex::getDatabaseId,
                publicationSeriesIndicatorRepository::findIndicatorForCodeAndPublicationSeriesId,
                id -> new PublicationSeriesIndicator(),
                PublicationSeriesIndicator::setPublicationSeries,
                publicationSeriesIndicatorRepository,
                "BookSeries"
            );
        });

        var eventsTask = CompletableFuture.runAsync(() -> {
            updateEntityStatisticInPeriod(
                startPeriods,
                indicatorCodes,
                eventIndexRepository,
                eventRepository,
                (start, ids) -> {
                    var totalCount = new AtomicInteger(0);
                    ids.forEach(id -> {
                        totalCount.addAndGet(
                            statisticsIndexRepository.countByTimestampBetweenAndTypeAndEventId(
                                start, LocalDateTime.now(), statisticsType.name(), id));
                    });
                    return totalCount.get();
                },
                EventIndex::getDatabaseId,
                eventIndicatorRepository::findIndicatorsForCodeAndEventId,
                id -> new EventIndicator(),
                EventIndicator::setEvent,
                eventIndicatorRepository,
                "Event"
            );
        });

        var allTasks =
            CompletableFuture.allOf(documentTask, personTask, organisationTask, journalsTask,
                eventsTask, bookSeriesTask);

        try {
            allTasks.get();
        } catch (InterruptedException | ExecutionException e) {
            log.error("Error while updating statistics for {}", statisticsType.name(), e);
            Thread.currentThread().interrupt();
        }
    }

    private <T, D extends Mergeable, I extends EntityIndicator> void updateEntityStatisticInPeriod(
        List<LocalDateTime> startPeriods,
        List<String> indicatorCodes,
        ElasticsearchRepository<T, String> indexRepository,
        JpaRepository<D, Integer> entityRepository,
        BiFunction<LocalDateTime, List<Integer>, Integer> countFunction,
        Function<T, Integer> getEntityId,
        BiFunction<String, Integer, Optional<I>> findIndicatorByEntityId,
        Function<Integer, I> createNewIndicator,
        BiConsumer<I, D> setEntity,
        JpaRepository<I, Integer> entityIndicatorRepository,
        String entityTypeName) {

        int pageNumber = 0;
        int chunkSize = 50;
        boolean hasNextPage = true;

        while (hasNextPage) {
            List<T> chunk =
                indexRepository.findAll(PageRequest.of(pageNumber, chunkSize)).getContent();

            chunk.forEach(indexEntity -> {
                Integer entityId = getEntityId.apply(indexEntity);

                entityRepository.findById(entityId).ifPresentOrElse(dbEntity -> {
                    FunctionalUtil.forEachWithCounter(indicatorCodes, (i, indicatorCode) -> {
                        var allEntityIds = new ArrayList<>(dbEntity.getMergedIds());
                        allEntityIds.add(entityId);

                        Integer statisticsCount =
                            countFunction.apply(startPeriods.get(i), allEntityIds);

                        var exclusions = IndicatorMappingConfigurationLoader.getExclusionsForClass(
                            dbEntity.getClass().getName());
                        if (exclusions.contains(indicatorCode)) {
                            return;
                        }

                        updateIndicator(entityId,
                            indicatorCode,
                            ignored -> Optional.of(dbEntity), // avoid second DB call
                            findIndicatorByEntityId,
                            id -> {
                                I indicator = createNewIndicator.apply(id);
                                setEntity.accept(indicator, dbEntity);
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
                }, () -> {
                    log.warn("{} with DB ID {} not found. Skipping indicator updates.",
                        entityTypeName, entityId);
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
            var fetchedEntity = findEntityById.apply(id);
            if (fetchedEntity.isEmpty()) {
                // Should not happen, but just in case
                log.warn(
                    "Entity with ID '{}' does not exist in the DB. Skipping calculating '{}'.", id,
                    indicatorCode);
                return;
            }

            indicatorEntity = createIndicator.apply(id);

            if (indicatorEntity instanceof DocumentIndicator) {
                ((DocumentIndicator) indicatorEntity).setDocument((Document) fetchedEntity.get());
            } else if (indicatorEntity instanceof PersonIndicator) {
                ((PersonIndicator) indicatorEntity).setPerson((Person) fetchedEntity.get());
            } else if (indicatorEntity instanceof OrganisationUnitIndicator) {
                ((OrganisationUnitIndicator) indicatorEntity).setOrganisationUnit(
                    (OrganisationUnit) fetchedEntity.get());
            } else if (indicatorEntity instanceof PublicationSeriesIndicator) {
                ((PublicationSeriesIndicator) indicatorEntity).setPublicationSeries(
                    (PublicationSeries) fetchedEntity.get());
            } else if (indicatorEntity instanceof EventIndicator) {
                ((EventIndicator) indicatorEntity).setEvent((Event) fetchedEntity.get());
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
