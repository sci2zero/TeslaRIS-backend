package rs.teslaris.exporter.service.impl;

import java.util.Arrays;
import java.util.List;
import java.util.Objects;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.locks.ReentrantLock;
import java.util.function.BiFunction;
import java.util.function.Function;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.query.Criteria;
import org.springframework.data.mongodb.core.query.Query;
import org.springframework.scheduling.annotation.Async;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import rs.teslaris.core.annotation.Traceable;
import rs.teslaris.core.model.document.BookSeries;
import rs.teslaris.core.model.document.Conference;
import rs.teslaris.core.model.document.Dataset;
import rs.teslaris.core.model.document.GeneticMaterial;
import rs.teslaris.core.model.document.Journal;
import rs.teslaris.core.model.document.JournalPublication;
import rs.teslaris.core.model.document.MaterialProduct;
import rs.teslaris.core.model.document.Monograph;
import rs.teslaris.core.model.document.MonographPublication;
import rs.teslaris.core.model.document.Patent;
import rs.teslaris.core.model.document.Proceedings;
import rs.teslaris.core.model.document.ProceedingsPublication;
import rs.teslaris.core.model.document.Software;
import rs.teslaris.core.model.document.Thesis;
import rs.teslaris.core.model.institution.OrganisationUnit;
import rs.teslaris.core.model.person.Person;
import rs.teslaris.core.repository.document.BookSeriesRepository;
import rs.teslaris.core.repository.document.ConferenceRepository;
import rs.teslaris.core.repository.document.DatasetRepository;
import rs.teslaris.core.repository.document.GeneticMaterialRepository;
import rs.teslaris.core.repository.document.JournalPublicationRepository;
import rs.teslaris.core.repository.document.JournalRepository;
import rs.teslaris.core.repository.document.MaterialProductRepository;
import rs.teslaris.core.repository.document.MonographPublicationRepository;
import rs.teslaris.core.repository.document.MonographRepository;
import rs.teslaris.core.repository.document.PatentRepository;
import rs.teslaris.core.repository.document.ProceedingsPublicationRepository;
import rs.teslaris.core.repository.document.ProceedingsRepository;
import rs.teslaris.core.repository.document.SoftwareRepository;
import rs.teslaris.core.repository.document.ThesisRepository;
import rs.teslaris.core.repository.institution.OrganisationUnitRepository;
import rs.teslaris.core.repository.person.PersonRepository;
import rs.teslaris.exporter.model.common.ExportDocument;
import rs.teslaris.exporter.model.common.ExportEvent;
import rs.teslaris.exporter.model.common.ExportOrganisationUnit;
import rs.teslaris.exporter.model.common.ExportPerson;
import rs.teslaris.exporter.model.common.ExportPublicationType;
import rs.teslaris.exporter.model.converter.ExportDocumentConverter;
import rs.teslaris.exporter.model.converter.ExportEventConverter;
import rs.teslaris.exporter.model.converter.ExportOrganisationUnitConverter;
import rs.teslaris.exporter.model.converter.ExportPersonConverter;
import rs.teslaris.exporter.model.converter.ExportPublicationSeriesConverter;
import rs.teslaris.exporter.service.interfaces.CommonExportService;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
@Traceable
@Slf4j
public class CommonExportServiceImpl implements CommonExportService {

    private final MongoTemplate mongoTemplate;

    private final OrganisationUnitRepository organisationUnitRepository;

    private final PersonRepository personRepository;

    private final ConferenceRepository conferenceRepository;

    private final DatasetRepository datasetRepository;

    private final SoftwareRepository softwareRepository;

    private final PatentRepository patentRepository;

    private final JournalRepository journalRepository;

    private final BookSeriesRepository bookSeriesRepository;

    private final JournalPublicationRepository journalPublicationRepository;

    private final MonographRepository monographRepository;

    private final ProceedingsRepository proceedingsRepository;

    private final ProceedingsPublicationRepository proceedingsPublicationRepository;

    private final MonographPublicationRepository monographPublicationRepository;

    private final ThesisRepository thesisRepository;

    private final GeneticMaterialRepository geneticMaterialRepository;

    private final MaterialProductRepository materialProductRepository;

    private final ReentrantLock organisationUnitExportLock = new ReentrantLock();

    private final ReentrantLock personExportLock = new ReentrantLock();

    private final ReentrantLock eventExportLock = new ReentrantLock();

    private final ReentrantLock documentExportLock = new ReentrantLock();

    @Value("${export-to-common.allowed}")
    private Boolean exportAllowed;


    @Override
    public void exportOrganisationUnitsToCommonModel(boolean allTime) {
        if (!organisationUnitExportLock.tryLock()) {
            log.info("OU export already in progress, skipping this run.");
            return;
        }

        try {
            exportEntities(
                organisationUnitRepository::findAllModified,
                ExportOrganisationUnitConverter::toCommonExportModel,
                ExportOrganisationUnit.class,
                OrganisationUnit::getId,
                allTime,
                null
            );
        } catch (Exception e) {
            log.error("Organisation Unit export set not completed due to an error.", e);
        } finally {
            organisationUnitExportLock.unlock();
        }
    }

    @Override
    public void exportPersonsToCommonModel(boolean allTime) {
        if (!personExportLock.tryLock()) {
            log.info("Person export already in progress, skipping this run.");
            return;
        }

        try {
            exportEntities(
                personRepository::findAllModified,
                ExportPersonConverter::toCommonExportModel,
                ExportPerson.class,
                Person::getId,
                allTime,
                null
            );
        } catch (Exception e) {
            log.error("Person export set not completed due to an error.", e);
        } finally {
            personExportLock.unlock();
        }
    }

    @Override
    public void exportConferencesToCommonModel(boolean allTime) {
        if (!eventExportLock.tryLock()) {
            log.info("Event export already in progress, skipping this run.");
            return;
        }

        try {
            exportEntities(
                conferenceRepository::findAllModified,
                ExportEventConverter::toCommonExportModel,
                ExportEvent.class,
                Conference::getId,
                allTime,
                null
            );
        } catch (Exception e) {
            log.error("Event export set not completed due to an error.", e);
        } finally {
            eventExportLock.unlock();
        }
    }

    @Override
    public void exportDocumentsToCommonModel(boolean allTime,
                                             List<ExportPublicationType> exportTypes) {
        if (!documentExportLock.tryLock()) {
            log.info("Document export already in progress, skipping this run.");
            return;
        }

        if (Objects.isNull(exportTypes) || exportTypes.isEmpty()) {
            exportTypes = Arrays.asList(ExportPublicationType.values());
        }

        try {
            List<CompletableFuture<Void>> futures = exportTypes.stream()
                .map(exportType -> createExportFuture(exportType, allTime))
                .filter(Objects::nonNull)
                .toList();

            if (!futures.isEmpty()) {
                CompletableFuture.allOf(futures.toArray(new CompletableFuture[0])).join();
                log.info("Completed export for types: {}", exportTypes);
            } else {
                log.info("No valid export types specified, skipping export.");
            }

        } catch (Exception e) {
            log.error("Document export set not completed due to an error.", e);
        } finally {
            documentExportLock.unlock();
        }
    }

    private CompletableFuture<Void> createExportFuture(ExportPublicationType exportType,
                                                       boolean allTime) {
        try {
            return switch (exportType) {
                case DATASET -> exportEntitiesAsync(
                    datasetRepository::findAllModified,
                    ExportDocumentConverter::toCommonExportModel,
                    ExportDocument.class,
                    Dataset::getId,
                    allTime,
                    ExportPublicationType.DATASET
                );
                case SOFTWARE -> exportEntitiesAsync(
                    softwareRepository::findAllModified,
                    ExportDocumentConverter::toCommonExportModel,
                    ExportDocument.class,
                    Software::getId,
                    allTime,
                    ExportPublicationType.SOFTWARE
                );
                case PATENT -> exportEntitiesAsync(
                    patentRepository::findAllModified,
                    ExportDocumentConverter::toCommonExportModel,
                    ExportDocument.class,
                    Patent::getId,
                    allTime,
                    ExportPublicationType.PATENT
                );
                case JOURNAL -> exportEntitiesAsync(
                    journalRepository::findAllModified,
                    ExportPublicationSeriesConverter::toCommonExportModel,
                    ExportDocument.class,
                    Journal::getId,
                    allTime,
                    ExportPublicationType.JOURNAL
                );
                case BOOK_SERIES -> exportEntitiesAsync(
                    bookSeriesRepository::findAllModified,
                    ExportPublicationSeriesConverter::toCommonExportModel,
                    ExportDocument.class,
                    BookSeries::getId,
                    allTime,
                    ExportPublicationType.BOOK_SERIES
                );
                case JOURNAL_PUBLICATION -> exportEntitiesAsync(
                    journalPublicationRepository::findAllModified,
                    ExportDocumentConverter::toCommonExportModel,
                    ExportDocument.class,
                    JournalPublication::getId,
                    allTime,
                    ExportPublicationType.JOURNAL_PUBLICATION
                );
                case PROCEEDINGS -> exportEntitiesAsync(
                    proceedingsRepository::findAllModified,
                    ExportDocumentConverter::toCommonExportModel,
                    ExportDocument.class,
                    Proceedings::getId,
                    allTime,
                    ExportPublicationType.PROCEEDINGS
                );
                case PROCEEDINGS_PUBLICATION -> exportEntitiesAsync(
                    proceedingsPublicationRepository::findAllModified,
                    ExportDocumentConverter::toCommonExportModel,
                    ExportDocument.class,
                    ProceedingsPublication::getId,
                    allTime,
                    ExportPublicationType.PROCEEDINGS_PUBLICATION
                );
                case MONOGRAPH -> exportEntitiesAsync(
                    monographRepository::findAllModified,
                    ExportDocumentConverter::toCommonExportModel,
                    ExportDocument.class,
                    Monograph::getId,
                    allTime,
                    ExportPublicationType.MONOGRAPH
                );
                case MONOGRAPH_PUBLICATION -> exportEntitiesAsync(
                    monographPublicationRepository::findAllModified,
                    ExportDocumentConverter::toCommonExportModel,
                    ExportDocument.class,
                    MonographPublication::getId,
                    allTime,
                    ExportPublicationType.MONOGRAPH_PUBLICATION
                );
                case THESIS -> exportEntitiesAsync(
                    thesisRepository::findAllModified,
                    ExportDocumentConverter::toCommonExportModel,
                    ExportDocument.class,
                    Thesis::getId,
                    allTime,
                    ExportPublicationType.THESIS
                );
                case MATERIAL_PRODUCT -> exportEntitiesAsync(
                    materialProductRepository::findAllModified,
                    ExportDocumentConverter::toCommonExportModel,
                    ExportDocument.class,
                    MaterialProduct::getId,
                    allTime,
                    ExportPublicationType.MATERIAL_PRODUCT
                );
                case GENETIC_MATERIAL -> exportEntitiesAsync(
                    geneticMaterialRepository::findAllModified,
                    ExportDocumentConverter::toCommonExportModel,
                    ExportDocument.class,
                    GeneticMaterial::getId,
                    allTime,
                    ExportPublicationType.GENETIC_MATERIAL
                );
            };
        } catch (Exception e) {
            log.error("Failed to create export future for type: {}", exportType, e);
            return null;
        }
    }

    @Async("taskExecutor")
    public <T, E> CompletableFuture<Void> exportEntitiesAsync(
        BiFunction<Pageable, Boolean, Page<T>> repositoryFunction,
        BiFunction<T, Boolean, E> converter,
        Class<E> exportClass,
        Function<T, Integer> idGetter,
        boolean allTime,
        ExportPublicationType exportPublicationType
    ) {
        try {
            exportEntities(repositoryFunction, converter, exportClass, idGetter, allTime,
                exportPublicationType);
        } catch (Exception e) {
            log.error("{} export set not completed in async thread due to an error. Reason: {}",
                exportClass.getName(), e.getMessage(), e);
        }

        return CompletableFuture.completedFuture(null);
    }

    private <T, E> void exportEntities(
        BiFunction<Pageable, Boolean, Page<T>> repositoryFunction,
        BiFunction<T, Boolean, E> converter,
        Class<E> exportClass,
        Function<T, Integer> idGetter,
        boolean allTime,
        ExportPublicationType exportPublicationType
    ) {
        int pageNumber = 0;
        int chunkSize = 100;
        boolean hasNextPage = true;

        while (hasNextPage) {
            List<T> chunk =
                repositoryFunction.apply(PageRequest.of(pageNumber, chunkSize), allTime)
                    .getContent();

            for (T entity : chunk) {
                var query = new Query();
                query.addCriteria(Criteria.where("database_id").is(idGetter.apply(entity)));

                if (Objects.nonNull(exportPublicationType)) {
                    query.addCriteria(Criteria.where("type").is(exportPublicationType.name()));
                }

                query.limit(1);

                var exportEntry = converter.apply(entity, true);

                mongoTemplate.remove(query, exportClass);
                mongoTemplate.save(exportEntry);
            }

            pageNumber++;
            hasNextPage = chunk.size() == chunkSize;
        }
    }

    @Scheduled(cron = "${export-to-common.schedule.documents}")
    protected void performScheduledDocumentExport() {
        if (!exportAllowed) {
            return;
        }

        exportDocumentsToCommonModel(false, Arrays.asList(ExportPublicationType.values()));
    }

    @Scheduled(cron = "${export-to-common.schedule.event}")
    protected void performScheduledEventExport() {
        if (!exportAllowed) {
            return;
        }

        exportConferencesToCommonModel(false);
    }

    @Scheduled(cron = "${export-to-common.schedule.person}")
    protected void performScheduledPersonExport() {
        if (!exportAllowed) {
            return;
        }

        exportPersonsToCommonModel(false);
    }

    @Scheduled(cron = "${export-to-common.schedule.ou}")
    protected void performScheduledOrganisationUnitExport() {
        if (!exportAllowed) {
            return;
        }

        exportOrganisationUnitsToCommonModel(false);
    }
}
