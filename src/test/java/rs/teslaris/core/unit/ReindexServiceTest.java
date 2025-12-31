package rs.teslaris.core.unit;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.stream.Stream;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.ApplicationEventPublisher;
import rs.teslaris.core.applicationevent.AllResearcherPointsReindexingEvent;
import rs.teslaris.core.applicationevent.HarvestExternalIndicatorsEvent;
import rs.teslaris.core.indexmodel.EntityType;
import rs.teslaris.core.service.impl.commontypes.ReindexServiceImpl;
import rs.teslaris.core.service.interfaces.document.BookSeriesService;
import rs.teslaris.core.service.interfaces.document.ConferenceService;
import rs.teslaris.core.service.interfaces.document.DatasetService;
import rs.teslaris.core.service.interfaces.document.DocumentFileService;
import rs.teslaris.core.service.interfaces.document.DocumentPublicationService;
import rs.teslaris.core.service.interfaces.document.JournalPublicationService;
import rs.teslaris.core.service.interfaces.document.JournalService;
import rs.teslaris.core.service.interfaces.document.MaterialProductService;
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
import rs.teslaris.core.util.functional.Pair;

@SpringBootTest
public class ReindexServiceTest {

    @Mock
    private UserService userService;

    @Mock
    private PublisherService publisherService;

    @Mock
    private PersonService personService;

    @Mock
    private OrganisationUnitService organisationUnitService;

    @Mock
    private JournalService journalService;

    @Mock
    private BookSeriesService bookSeriesService;

    @Mock
    private ConferenceService conferenceService;

    @Mock
    private DocumentPublicationService documentPublicationService;

    @Mock
    private DocumentFileService documentFileService;

    @Mock
    private JournalPublicationService journalPublicationService;

    @Mock
    private ProceedingsService proceedingsService;

    @Mock
    private ProceedingsPublicationService proceedingsPublicationService;

    @Mock
    private PatentService patentService;

    @Mock
    private SoftwareService softwareService;

    @Mock
    private DatasetService datasetService;

    @Mock
    private MonographService monographService;

    @Mock
    private MonographPublicationService monographPublicationService;

    @Mock
    private ThesisService thesisService;

    @Mock
    private ApplicationEventPublisher applicationEventPublisher;

    @Mock
    private MaterialProductService materialProductService;

    @InjectMocks
    private ReindexServiceImpl reindexService;


    static Stream<Pair<EntityType, Boolean>> indexTypeProvider() {
        return Stream.of(
            new Pair<>(EntityType.USER_ACCOUNT, false),
            new Pair<>(EntityType.JOURNAL, false),
            new Pair<>(EntityType.PUBLISHER, false),
            new Pair<>(EntityType.PERSON, false),
            new Pair<>(EntityType.ORGANISATION_UNIT, false),
            new Pair<>(EntityType.BOOK_SERIES, false),
            new Pair<>(EntityType.EVENT, false),
            new Pair<>(EntityType.PUBLICATION, false),
            new Pair<>(EntityType.DOCUMENT_FILE, false),
            new Pair<>(EntityType.USER_ACCOUNT, true),
            new Pair<>(EntityType.JOURNAL, true),
            new Pair<>(EntityType.PUBLISHER, true),
            new Pair<>(EntityType.PERSON, true),
            new Pair<>(EntityType.ORGANISATION_UNIT, true),
            new Pair<>(EntityType.BOOK_SERIES, true),
            new Pair<>(EntityType.EVENT, true),
            new Pair<>(EntityType.PUBLICATION, true),
            new Pair<>(EntityType.DOCUMENT_FILE, true)
        );
    }

    @ParameterizedTest
    @MethodSource("indexTypeProvider")
    void testReindexDatabase(Pair<EntityType, Boolean> indexType) {
        // Given
        var indexesToRepopulate = List.of(indexType.a);

        // Return completed futures for all mocked services
        when(userService.reindexUsers()).thenReturn(CompletableFuture.completedFuture(null));
        when(journalService.reindexJournals()).thenReturn(CompletableFuture.completedFuture(null));
        when(publisherService.reindexPublishers()).thenReturn(
            CompletableFuture.completedFuture(null));
        when(personService.reindexPersons()).thenReturn(CompletableFuture.completedFuture(null));
        when(organisationUnitService.reindexOrganisationUnits()).thenReturn(
            CompletableFuture.completedFuture(null));
        when(bookSeriesService.reindexBookSeries()).thenReturn(
            CompletableFuture.completedFuture(null));
        when(conferenceService.reindexConferences()).thenReturn(
            CompletableFuture.completedFuture(null));
        when(documentFileService.reindexDocumentFiles()).thenReturn(
            CompletableFuture.completedFuture(null));

        // When
        reindexService.reindexDatabase(indexesToRepopulate, indexType.b, null);

        // Then
        verify(userService,
            indexType.a.equals(EntityType.USER_ACCOUNT) ? times(1) : never()).reindexUsers();
        verify(publisherService,
            indexType.a.equals(EntityType.PUBLISHER) ? times(1) : never()).reindexPublishers();
        verify(personService,
            indexType.a.equals(EntityType.PERSON) ? times(1) : never()).reindexPersons();
        verify(organisationUnitService,
            indexType.a.equals(EntityType.ORGANISATION_UNIT) ? times(1) :
                never()).reindexOrganisationUnits();
        verify(journalService,
            indexType.a.equals(EntityType.JOURNAL) ? times(1) : never()).reindexJournals();
        verify(bookSeriesService,
            indexType.a.equals(EntityType.BOOK_SERIES) ? times(1) : never()).reindexBookSeries();
        verify(conferenceService,
            indexType.a.equals(EntityType.EVENT) ? times(1) : never()).reindexConferences();
        verify(documentFileService,
            indexType.a.equals(EntityType.DOCUMENT_FILE) ? times(1) :
                never()).reindexDocumentFiles();

        if (indexType.a.equals(EntityType.PUBLICATION)) {
            verify(documentPublicationService).deleteIndexes();
            verify(journalPublicationService).reindexJournalPublications();
            verify(proceedingsPublicationService).reindexProceedingsPublications();
            verify(patentService).reindexPatents();
            verify(softwareService).reindexSoftware();
            verify(datasetService).reindexDatasets();
            verify(monographService).reindexMonographs();
            verify(monographPublicationService).reindexMonographPublications();
            verify(proceedingsService).reindexProceedings();
            verify(thesisService).reindexTheses();
            verify(applicationEventPublisher).publishEvent(
                any(AllResearcherPointsReindexingEvent.class));
        } else if (indexType.a.equals(EntityType.DOCUMENT_FILE)) {
            verify(documentFileService).deleteIndexes();
            verify(documentFileService).reindexDocumentFiles();
        } else if (indexType.a.equals(EntityType.PERSON)) {
            verify(applicationEventPublisher).publishEvent(
                any(AllResearcherPointsReindexingEvent.class));
        } else {
            verify(documentFileService, never()).deleteIndexes();
            verify(documentPublicationService, never()).deleteIndexes();
            verify(journalPublicationService, never()).reindexJournalPublications();
            verify(proceedingsPublicationService, never()).reindexProceedingsPublications();
            verify(patentService, never()).reindexPatents();
            verify(softwareService, never()).reindexSoftware();
            verify(datasetService, never()).reindexDatasets();
            verify(monographService, never()).reindexMonographs();
            verify(monographPublicationService, never()).reindexMonographPublications();
            verify(proceedingsService, never()).reindexProceedings();
            verify(thesisService, never()).reindexTheses();
        }

        if (indexType.b) {
            applicationEventPublisher.publishEvent(any(HarvestExternalIndicatorsEvent.class));
        }
    }
}
