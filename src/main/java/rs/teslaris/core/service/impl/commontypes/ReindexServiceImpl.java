package rs.teslaris.core.service.impl.commontypes;

import java.util.ArrayList;
import java.util.List;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.jetbrains.annotations.NotNull;
import org.springframework.stereotype.Service;
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
import rs.teslaris.core.service.interfaces.person.OrganisationUnitService;
import rs.teslaris.core.service.interfaces.person.PersonService;
import rs.teslaris.core.service.interfaces.user.UserService;

@Service
@RequiredArgsConstructor
@Slf4j
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


    @Override
    public void reindexDatabase(List<EntityType> indexesToRepopulate) {
        var threadPool = new ArrayList<Thread>();

        if (indexesToRepopulate.contains(EntityType.USER_ACCOUNT)) {
            var userThread = new Thread(userService::reindexUsers);
            userThread.start();
            threadPool.add(userThread);
        }

        if (indexesToRepopulate.contains(EntityType.JOURNAL)) {
            var journalThread = new Thread(journalService::reindexJournals);
            journalThread.start();
            threadPool.add(journalThread);
        }

        if (indexesToRepopulate.contains(EntityType.PUBLISHER)) {
            var publisherThread = new Thread(publisherService::reindexPublishers);
            publisherThread.start();
            threadPool.add(publisherThread);
        }

        if (indexesToRepopulate.contains(EntityType.PERSON)) {
            var personThread = new Thread(personService::reindexPersons);
            personThread.start();
            threadPool.add(personThread);
        }

        if (indexesToRepopulate.contains(EntityType.ORGANISATION_UNIT)) {
            var organisationUnitThread = new Thread(
                organisationUnitService::reindexOrganisationUnits);
            organisationUnitThread.start();
            threadPool.add(organisationUnitThread);
        }

        if (indexesToRepopulate.contains(EntityType.BOOK_SERIES)) {
            var bookSeriesThread = new Thread(bookSeriesService::reindexBookSeries);
            bookSeriesThread.start();
            threadPool.add(bookSeriesThread);
        }

        if (indexesToRepopulate.contains(EntityType.EVENT)) {
            var conferenceThread = new Thread(conferenceService::reindexConferences);
            conferenceThread.start();
            threadPool.add(conferenceThread);
        }

        if (indexesToRepopulate.contains(EntityType.PUBLICATION)) {
            var reindexPublicationsThread = getReindexPublicationsThread();
            threadPool.add(reindexPublicationsThread);
        }

        try {
            for (Thread thread : threadPool) {
                thread.join();
            }
        } catch (InterruptedException e) {
            log.error("Thread interrupted while waiting for reindexing to complete", e);
        }
    }

    @NotNull
    private Thread getReindexPublicationsThread() {
        var reindexPublicationsThread = new Thread(() -> {
            documentFileService.deleteIndexes();
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
        });

        reindexPublicationsThread.start();
        return reindexPublicationsThread;
    }
}
