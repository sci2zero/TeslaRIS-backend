package rs.teslaris.core.service.impl.commontypes;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import rs.teslaris.core.annotation.Traceable;
import rs.teslaris.core.applicationevent.AllResearcherPointsReindexingEvent;
import rs.teslaris.core.indexmodel.EntityType;
import rs.teslaris.core.service.interfaces.commontypes.ReindexService;
import rs.teslaris.core.service.interfaces.document.BookSeriesService;
import rs.teslaris.core.service.interfaces.document.ConferenceService;
import rs.teslaris.core.service.interfaces.document.DatasetService;
import rs.teslaris.core.service.interfaces.document.DocumentFileService;
import rs.teslaris.core.service.interfaces.document.DocumentPublicationService;
import rs.teslaris.core.service.interfaces.document.JournalPublicationService;
import rs.teslaris.core.service.interfaces.document.JournalService;
import rs.teslaris.core.service.interfaces.document.MonographPublicationService;
import rs.teslaris.core.service.interfaces.document.MonographService;
import rs.teslaris.core.service.interfaces.document.PatentService;
import rs.teslaris.core.service.interfaces.document.ProceedingsPublicationService;
import rs.teslaris.core.service.interfaces.document.ProceedingsService;
import rs.teslaris.core.service.interfaces.document.PublisherService;
import rs.teslaris.core.service.interfaces.document.SoftwareService;
import rs.teslaris.core.service.interfaces.document.ThesisService;
import rs.teslaris.core.service.interfaces.institution.OrganisationUnitService;
import rs.teslaris.core.service.interfaces.person.PersonService;
import rs.teslaris.core.service.interfaces.user.UserService;

@Service
@RequiredArgsConstructor
@Slf4j
@Traceable
public class ReindexServiceImpl implements ReindexService {

    private final UserService userService;

    private final PublisherService publisherService;

    private final PersonService personService;

    private final OrganisationUnitService organisationUnitService;

    private final JournalService journalService;

    private final BookSeriesService bookSeriesService;

    private final ConferenceService conferenceService;

    private final DocumentPublicationService documentPublicationService;

    private final DocumentFileService documentFileService;

    private final JournalPublicationService journalPublicationService;

    private final ProceedingsPublicationService proceedingsPublicationService;

    private final ProceedingsService proceedingsService;

    private final PatentService patentService;

    private final SoftwareService softwareService;

    private final DatasetService datasetService;

    private final MonographService monographService;

    private final MonographPublicationService monographPublicationService;

    private final ThesisService thesisService;

    private final ApplicationEventPublisher applicationEventPublisher;


    @Override
    public void reindexDatabase(List<EntityType> indexesToRepopulate) {
        List<CompletableFuture<Void>> futures = new ArrayList<>();

        if (indexesToRepopulate.contains(EntityType.USER_ACCOUNT)) {
            futures.add(userService.reindexUsers());
        }

        if (indexesToRepopulate.contains(EntityType.JOURNAL)) {
            futures.add(journalService.reindexJournals());
        }

        if (indexesToRepopulate.contains(EntityType.PUBLISHER)) {
            futures.add(publisherService.reindexPublishers());
        }

        if (indexesToRepopulate.contains(EntityType.PERSON)) {
            futures.add(personService.reindexPersons());
        }

        if (indexesToRepopulate.contains(EntityType.ORGANISATION_UNIT)) {
            futures.add(organisationUnitService.reindexOrganisationUnits());
        }

        if (indexesToRepopulate.contains(EntityType.BOOK_SERIES)) {
            futures.add(bookSeriesService.reindexBookSeries());
        }

        if (indexesToRepopulate.contains(EntityType.EVENT)) {
            futures.add(conferenceService.reindexConferences());
        }

        if (indexesToRepopulate.contains(EntityType.DOCUMENT_FILE)) {
            futures.add(reindexDocumentFiles());
        }

        if (indexesToRepopulate.contains(EntityType.PUBLICATION)) {
            futures.add(reindexPublications());
        }

        try {
            CompletableFuture.allOf(futures.toArray(new CompletableFuture[0])).join();

            if (indexesToRepopulate.contains(EntityType.PUBLICATION) ||
                indexesToRepopulate.contains(EntityType.PERSON)) {
                applicationEventPublisher.publishEvent(new AllResearcherPointsReindexingEvent());
            }
        } catch (CompletionException e) {
            log.error("Error during parallel reindexing. Reason: ", e);
        }
    }

    @Async("reindexExecutor")
    public CompletableFuture<Void> reindexPublications() {
        documentPublicationService.deleteIndexes();

        journalPublicationService.reindexJournalPublications();
        proceedingsPublicationService.reindexProceedingsPublications();
        patentService.reindexPatents();
        softwareService.reindexSoftware();
        datasetService.reindexDatasets();
        monographService.reindexMonographs();
        monographPublicationService.reindexMonographPublications();
        proceedingsService.reindexProceedings();
        thesisService.reindexTheses();

        return CompletableFuture.completedFuture(null);
    }

    @Async("reindexExecutor")
    public CompletableFuture<Void> reindexDocumentFiles() {
        documentFileService.deleteIndexes();
        documentFileService.reindexDocumentFiles();

        return CompletableFuture.completedFuture(null);
    }
}
