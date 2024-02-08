package rs.teslaris.core.service.impl.commontypes;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import rs.teslaris.core.service.interfaces.commontypes.ReindexService;
import rs.teslaris.core.service.interfaces.document.BookSeriesService;
import rs.teslaris.core.service.interfaces.document.ConferenceService;
import rs.teslaris.core.service.interfaces.document.DocumentPublicationService;
import rs.teslaris.core.service.interfaces.document.JournalPublicationService;
import rs.teslaris.core.service.interfaces.document.JournalService;
import rs.teslaris.core.service.interfaces.document.ProceedingsPublicationService;
import rs.teslaris.core.service.interfaces.document.PublisherService;
import rs.teslaris.core.service.interfaces.person.OrganisationUnitService;
import rs.teslaris.core.service.interfaces.person.PersonService;
import rs.teslaris.core.service.interfaces.user.UserService;

@Service
@RequiredArgsConstructor
public class ReindexServiceImpl implements ReindexService {

    private final UserService userService;

    private final PublisherService publisherService;

    private final PersonService personService;

    private final OrganisationUnitService organisationUnitService;

    private final JournalService journalService;

    private final BookSeriesService bookSeriesService;

    private final ConferenceService conferenceService;

    private final DocumentPublicationService documentPublicationService;

    private final JournalPublicationService journalPublicationService;

    private final ProceedingsPublicationService proceedingsPublicationService;


    @Override
    public void reindexDatabase() {
        userService.reindexUsers();
        publisherService.reindexPublishers();
        personService.reindexPersons();
        organisationUnitService.reindexOrganisationUnits();
        journalService.reindexJournals();
        bookSeriesService.reindexBookSeries();
        conferenceService.reindexConferences();

        documentPublicationService.deleteIndexes();
        journalPublicationService.reindexJournalPublications();
        proceedingsPublicationService.reindexProceedingsPublications();
    }
}
