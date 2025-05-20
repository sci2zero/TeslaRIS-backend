package rs.teslaris.exporter.service.impl;

import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.function.BiFunction;
import java.util.function.Function;
import lombok.RequiredArgsConstructor;
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
import rs.teslaris.core.model.document.Conference;
import rs.teslaris.core.model.document.Dataset;
import rs.teslaris.core.model.document.Journal;
import rs.teslaris.core.model.document.JournalPublication;
import rs.teslaris.core.model.document.Monograph;
import rs.teslaris.core.model.document.MonographPublication;
import rs.teslaris.core.model.document.Patent;
import rs.teslaris.core.model.document.Proceedings;
import rs.teslaris.core.model.document.ProceedingsPublication;
import rs.teslaris.core.model.document.Software;
import rs.teslaris.core.model.document.Thesis;
import rs.teslaris.core.model.institution.OrganisationUnit;
import rs.teslaris.core.model.person.Person;
import rs.teslaris.core.repository.document.ConferenceRepository;
import rs.teslaris.core.repository.document.DatasetRepository;
import rs.teslaris.core.repository.document.JournalPublicationRepository;
import rs.teslaris.core.repository.document.JournalRepository;
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
import rs.teslaris.exporter.model.converter.ExportDocumentConverter;
import rs.teslaris.exporter.model.converter.ExportEventConverter;
import rs.teslaris.exporter.model.converter.ExportOrganisationUnitConverter;
import rs.teslaris.exporter.model.converter.ExportPersonConverter;
import rs.teslaris.exporter.model.converter.ExportPublicationSeriesConverter;
import rs.teslaris.exporter.service.interfaces.CommonExportService;

@Service
@RequiredArgsConstructor
@Transactional
public class CommonExportServiceImpl implements CommonExportService {

    private final MongoTemplate mongoTemplate;

    private final OrganisationUnitRepository organisationUnitRepository;

    private final PersonRepository personRepository;

    private final ConferenceRepository conferenceRepository;

    private final DatasetRepository datasetRepository;

    private final SoftwareRepository softwareRepository;

    private final PatentRepository patentRepository;

    private final JournalRepository journalRepository;

    private final JournalPublicationRepository journalPublicationRepository;

    private final MonographRepository monographRepository;

    private final ProceedingsRepository proceedingsRepository;

    private final ProceedingsPublicationRepository proceedingsPublicationRepository;

    private final MonographPublicationRepository monographPublicationRepository;

    private final ThesisRepository thesisRepository;


    @Override
    @Scheduled(cron = "${export-to-common.schedule.ou}")
    public void exportOrganisationUnitsToCommonModel() {
        exportEntities(
            organisationUnitRepository::findAllModifiedInLast24Hours,
            ExportOrganisationUnitConverter::toCommonExportModel,
            ExportOrganisationUnit.class,
            OrganisationUnit::getId
        );
    }

    @Override
    @Scheduled(cron = "${export-to-common.schedule.person}")
    public void exportPersonsToCommonModel() {
        exportEntities(
            personRepository::findAllModifiedInLast24Hours,
            ExportPersonConverter::toCommonExportModel,
            ExportPerson.class,
            Person::getId
        );
    }

    @Override
    @Scheduled(cron = "${export-to-common.schedule.event}")
    public void exportConferencesToCommonModel() {
        exportEntities(
            conferenceRepository::findAllModifiedInLast24Hours,
            ExportEventConverter::toCommonExportModel,
            ExportEvent.class,
            Conference::getId
        );
    }

    @Override
    @Scheduled(cron = "${export-to-common.schedule.documents}")
    public void exportDocumentsToCommonModel() {
        var datasetFuture = exportEntitiesAsync(
            datasetRepository::findAllModifiedInLast24Hours,
            ExportDocumentConverter::toCommonExportModel,
            ExportDocument.class,
            Dataset::getId
        );

        var softwareFuture = exportEntitiesAsync(
            softwareRepository::findAllModifiedInLast24Hours,
            ExportDocumentConverter::toCommonExportModel,
            ExportDocument.class,
            Software::getId
        );

        var patentFuture = exportEntitiesAsync(
            patentRepository::findAllModifiedInLast24Hours,
            ExportDocumentConverter::toCommonExportModel,
            ExportDocument.class,
            Patent::getId
        );

        var journalFuture = exportEntitiesAsync(
            journalRepository::findAllModifiedInLast24Hours,
            ExportPublicationSeriesConverter::toCommonExportModel,
            ExportDocument.class,
            Journal::getId
        );

        var journalPublicationFuture = exportEntitiesAsync(
            journalPublicationRepository::findAllModifiedInLast24Hours,
            ExportDocumentConverter::toCommonExportModel,
            ExportDocument.class,
            JournalPublication::getId
        );

        var proceedingsFuture = exportEntitiesAsync(
            proceedingsRepository::findAllModifiedInLast24Hours,
            ExportDocumentConverter::toCommonExportModel,
            ExportDocument.class,
            Proceedings::getId
        );

        var proceedingsPublicationFuture = exportEntitiesAsync(
            proceedingsPublicationRepository::findAllModifiedInLast24Hours,
            ExportDocumentConverter::toCommonExportModel,
            ExportDocument.class,
            ProceedingsPublication::getId
        );

        var monographFuture = exportEntitiesAsync(
            monographRepository::findAllModifiedInLast24Hours,
            ExportDocumentConverter::toCommonExportModel,
            ExportDocument.class,
            Monograph::getId
        );

        var monographPublicationFuture = exportEntitiesAsync(
            monographPublicationRepository::findAllModifiedInLast24Hours,
            ExportDocumentConverter::toCommonExportModel,
            ExportDocument.class,
            MonographPublication::getId
        );

        var thesisFuture = exportEntitiesAsync(
            thesisRepository::findAllModifiedInLast24Hours,
            ExportDocumentConverter::toCommonExportModel,
            ExportDocument.class,
            Thesis::getId
        );

        CompletableFuture.allOf(
            datasetFuture, softwareFuture, patentFuture,
            journalFuture, journalPublicationFuture,
            proceedingsFuture, proceedingsPublicationFuture,
            monographFuture, monographPublicationFuture,
            thesisFuture
        ).join();
    }

    @Async("taskExecutor")
    public <T, E> CompletableFuture<Void> exportEntitiesAsync(
        Function<Pageable, Page<T>> repositoryFunction,
        BiFunction<T, Boolean, E> converter,
        Class<E> exportClass,
        Function<T, Integer> idGetter
    ) {
        exportEntities(repositoryFunction, converter, exportClass, idGetter);
        return CompletableFuture.completedFuture(null);
    }

    private <T, E> void exportEntities(
        Function<Pageable, Page<T>> repositoryFunction,
        BiFunction<T, Boolean, E> converter,
        Class<E> exportClass,
        Function<T, Integer> idGetter
    ) {
        int pageNumber = 0;
        int chunkSize = 10;
        boolean hasNextPage = true;

        while (hasNextPage) {
            List<T> chunk =
                repositoryFunction.apply(PageRequest.of(pageNumber, chunkSize)).getContent();
            for (T entity : chunk) {
                var query = new Query();
                query.addCriteria(Criteria.where("database_id").is(idGetter.apply(entity)));
                query.limit(1);

                var exportEntry = converter.apply(entity, true);

                mongoTemplate.remove(query, exportClass);
                mongoTemplate.save(exportEntry);
            }

            pageNumber++;
            hasNextPage = chunk.size() == chunkSize;
        }
    }
}
