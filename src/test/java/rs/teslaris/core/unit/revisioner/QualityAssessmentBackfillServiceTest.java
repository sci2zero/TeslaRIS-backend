package rs.teslaris.core.unit.revisioner;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import java.util.ArrayList;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mock;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.data.domain.PageImpl;
import rs.teslaris.core.indexmodel.BookSeriesIndex;
import rs.teslaris.core.indexmodel.DocumentPublicationIndex;
import rs.teslaris.core.indexmodel.DocumentPublicationType;
import rs.teslaris.core.indexmodel.EntityType;
import rs.teslaris.core.indexmodel.EventIndex;
import rs.teslaris.core.indexmodel.EventType;
import rs.teslaris.core.indexmodel.JournalIndex;
import rs.teslaris.core.indexmodel.OrganisationUnitIndex;
import rs.teslaris.core.indexmodel.PersonIndex;
import rs.teslaris.core.indexmodel.PublisherIndex;
import rs.teslaris.core.service.interfaces.commontypes.SearchService;
import rs.teslaris.revisioner.model.QualityAssessmentTarget;
import rs.teslaris.revisioner.service.impl.QualityAssessmentBackfillServiceImpl;
import rs.teslaris.revisioner.service.interfaces.DataQualityService;
import rs.teslaris.revisioner.service.interfaces.RevisionService;

@SpringBootTest
public class QualityAssessmentBackfillServiceTest {

    private static final int PAGE_SIZE = 100;

    @Mock
    private RevisionService revisionService;

    @Mock
    private DataQualityService dataQualityService;

    @Mock
    private SearchService<PersonIndex> personSearchService;

    @Mock
    private SearchService<OrganisationUnitIndex> organisationUnitSearchService;

    @Mock
    private SearchService<EventIndex> eventSearchService;

    @Mock
    private SearchService<DocumentPublicationIndex> documentSearchService;

    @Mock
    private SearchService<JournalIndex> journalSearchService;

    @Mock
    private SearchService<BookSeriesIndex> bookSeriesSearchService;

    @Mock
    private SearchService<PublisherIndex> publisherSearchService;

    private QualityAssessmentBackfillServiceImpl qualityAssessmentBackfillService;


    @BeforeEach
    public void setUp() {
        qualityAssessmentBackfillService = new QualityAssessmentBackfillServiceImpl(
            revisionService, dataQualityService, personSearchService,
            organisationUnitSearchService, eventSearchService, documentSearchService,
            journalSearchService, bookSeriesSearchService, publisherSearchService);
    }

    private PersonIndex person(Integer databaseId) {
        var index = new PersonIndex();
        index.setDatabaseId(databaseId);

        return index;
    }

    private DocumentPublicationIndex document(Integer databaseId, String type) {
        var index = new DocumentPublicationIndex();
        index.setDatabaseId(databaseId);
        index.setType(type);

        return index;
    }

    private EventIndex event(Integer databaseId, EventType eventType) {
        var index = new EventIndex();
        index.setDatabaseId(databaseId);
        index.setEventType(eventType);

        return index;
    }

    private void stubPersons(PersonIndex... persons) {
        when(personSearchService.runQuery(any(), any(), eq(PersonIndex.class), anyString()))
            .thenReturn(new PageImpl<>(List.of(persons)));
    }

    @Test
    public void shouldDoNothingWhenNoTargetIsRequested() {
        // when
        qualityAssessmentBackfillService.performBackfill(List.of(), null, null, "PTCRIS", false);

        // then
        verifyNoInteractions(personSearchService);
        verifyNoInteractions(revisionService);
        verifyNoInteractions(dataQualityService);
    }

    @Test
    public void shouldDoNothingWhenTargetsAreNull() {
        // when
        qualityAssessmentBackfillService.performBackfill(null, List.of(1), null, "PTCRIS", false);

        // then
        verifyNoInteractions(personSearchService);
        verifyNoInteractions(revisionService);
    }

    @Test
    public void shouldCaptureCurrentStateForEveryScannedRecord() {
        // given
        stubPersons(person(1), person(2));

        when(revisionService.createRevisionFromCurrentState(anyString(), anyInt(), anyString()))
            .thenReturn(true);

        // when
        qualityAssessmentBackfillService.performBackfill(
            List.of(QualityAssessmentTarget.PERSON), List.of(1, 2), null, "PTCRIS", false);

        // then
        verify(revisionService).createRevisionFromCurrentState(EntityType.PERSON.name(), 1,
            "PTCRIS");
        verify(revisionService).createRevisionFromCurrentState(EntityType.PERSON.name(), 2,
            "PTCRIS");

        // A record that had no revision is assessed by the revision it just got.
        verifyNoInteractions(dataQualityService);
    }

    @Test
    public void shouldReassessRecordsThatAlreadyHaveRevisionsWhenRewriteIsRequested() {
        // given
        stubPersons(person(1));

        when(revisionService.createRevisionFromCurrentState(anyString(), anyInt(), anyString()))
            .thenReturn(false);

        // when
        qualityAssessmentBackfillService.performBackfill(
            List.of(QualityAssessmentTarget.PERSON), null, null, "PTCRIS", true);

        // then
        verify(dataQualityService).reassessLatestRevision(EntityType.PERSON.name(), 1, "PTCRIS");
    }

    @Test
    public void shouldLeaveExistingAssessmentsAloneWhenRewriteIsNotRequested() {
        // given
        stubPersons(person(1));

        when(revisionService.createRevisionFromCurrentState(anyString(), anyInt(), anyString()))
            .thenReturn(false);

        // when
        qualityAssessmentBackfillService.performBackfill(
            List.of(QualityAssessmentTarget.PERSON), null, null, "PTCRIS", false);

        // then
        verify(revisionService).createRevisionFromCurrentState(EntityType.PERSON.name(), 1,
            "PTCRIS");
        verifyNoInteractions(dataQualityService);
    }

    @Test
    public void shouldResolveEntityTypeOfDocumentsFromTheirIndexedType() {
        // given
        when(documentSearchService.runQuery(any(), any(), eq(DocumentPublicationIndex.class),
            anyString()))
            .thenReturn(new PageImpl<>(List.of(
                document(5, DocumentPublicationType.THESIS.name()),
                document(6, DocumentPublicationType.JOURNAL_PUBLICATION.name()))));

        // when
        qualityAssessmentBackfillService.performBackfill(
            List.of(QualityAssessmentTarget.DOCUMENT), null, List.of(7), "PTCRIS", false);

        // then
        verify(revisionService).createRevisionFromCurrentState(
            DocumentPublicationType.THESIS.name(), 5, "PTCRIS");
        verify(revisionService).createRevisionFromCurrentState(
            DocumentPublicationType.JOURNAL_PUBLICATION.name(), 6, "PTCRIS");
    }

    @Test
    public void shouldResolveEntityTypeOfEventsFromTheirTypeAndSkipUntypedOnes() {
        // given
        when(eventSearchService.runQuery(any(), any(), eq(EventIndex.class), anyString()))
            .thenReturn(new PageImpl<>(List.of(
                event(1, EventType.CONFERENCE),
                event(2, null))));

        // when
        qualityAssessmentBackfillService.performBackfill(
            List.of(QualityAssessmentTarget.EVENT), null, null, "PTCRIS", false);

        // then
        verify(revisionService).createRevisionFromCurrentState(EventType.CONFERENCE.name(), 1,
            "PTCRIS");
        verify(revisionService, never()).createRevisionFromCurrentState(anyString(), eq(2),
            anyString());
    }

    @Test
    public void shouldSkipRecordsWithoutDatabaseId() {
        // given
        stubPersons(person(null), person(3));

        // when
        qualityAssessmentBackfillService.performBackfill(
            List.of(QualityAssessmentTarget.PERSON), null, null, "PTCRIS", false);

        // then
        verify(revisionService, times(1)).createRevisionFromCurrentState(anyString(), anyInt(),
            anyString());
        verify(revisionService).createRevisionFromCurrentState(EntityType.PERSON.name(), 3,
            "PTCRIS");
    }

    @Test
    public void shouldKeepScanningWhenOneRecordFails() {
        // given
        stubPersons(person(1), person(2));

        when(revisionService.createRevisionFromCurrentState(EntityType.PERSON.name(), 1, "PTCRIS"))
            .thenThrow(new RuntimeException("entity is gone"));
        when(revisionService.createRevisionFromCurrentState(EntityType.PERSON.name(), 2, "PTCRIS"))
            .thenReturn(true);

        // when
        qualityAssessmentBackfillService.performBackfill(
            List.of(QualityAssessmentTarget.PERSON), null, null, "PTCRIS", false);

        // then
        verify(revisionService).createRevisionFromCurrentState(EntityType.PERSON.name(), 2,
            "PTCRIS");
    }

    @Test
    public void shouldFetchAnotherBatchWhileTheIndexKeepsDelivering() {
        // given (a full batch means there may be more, an incomplete one ends the scan)
        var fullBatch = new ArrayList<PersonIndex>();
        for (int databaseId = 1; databaseId <= PAGE_SIZE; databaseId++) {
            fullBatch.add(person(databaseId));
        }

        when(personSearchService.runQuery(any(), any(), eq(PersonIndex.class), anyString()))
            .thenReturn(new PageImpl<>(fullBatch))
            .thenReturn(new PageImpl<>(List.of(person(PAGE_SIZE + 1))));

        // when
        qualityAssessmentBackfillService.performBackfill(
            List.of(QualityAssessmentTarget.PERSON), null, null, "PTCRIS", false);

        // then
        verify(personSearchService, times(2))
            .runQuery(any(), any(), eq(PersonIndex.class), anyString());
        verify(revisionService, times(PAGE_SIZE + 1))
            .createRevisionFromCurrentState(anyString(), anyInt(), anyString());
    }

    @Test
    public void shouldScanEveryRequestedTarget() {
        // given
        stubPersons(person(1));

        when(publisherSearchService.runQuery(any(), any(), eq(PublisherIndex.class), anyString()))
            .thenReturn(new PageImpl<>(List.of()));

        // when
        qualityAssessmentBackfillService.performBackfill(
            List.of(QualityAssessmentTarget.PERSON, QualityAssessmentTarget.PUBLISHER),
            null, null, "PTCRIS", false);

        // then
        verify(personSearchService).runQuery(any(), any(), eq(PersonIndex.class), anyString());
        verify(publisherSearchService)
            .runQuery(any(), any(), eq(PublisherIndex.class), anyString());

        verifyNoInteractions(journalSearchService);
        verifyNoInteractions(bookSeriesSearchService);
        verifyNoInteractions(eventSearchService);
        verifyNoInteractions(documentSearchService);
        verifyNoInteractions(organisationUnitSearchService);
    }

    @Test
    public void shouldScanIndexesWithoutMatchingAssociationInFull() {
        // given (book series carry neither person nor organisation unit ids)
        when(bookSeriesSearchService.runQuery(any(), any(), eq(BookSeriesIndex.class), anyString()))
            .thenReturn(new PageImpl<>(List.of()));

        // when
        qualityAssessmentBackfillService.performBackfill(
            List.of(QualityAssessmentTarget.BOOK_SERIES), List.of(1), List.of(2), "PTCRIS", false);

        // then
        verify(bookSeriesSearchService)
            .runQuery(any(), any(), eq(BookSeriesIndex.class), anyString());
    }

    @Test
    public void shouldNotTouchDataQualityServiceWhenNothingIsIndexed() {
        // given
        when(organisationUnitSearchService.runQuery(any(), any(), eq(OrganisationUnitIndex.class),
            anyString())).thenReturn(new PageImpl<>(List.of()));

        // when
        qualityAssessmentBackfillService.performBackfill(
            List.of(QualityAssessmentTarget.ORGANISATION_UNIT), null, null, "PTCRIS", true);

        // then
        verifyNoInteractions(revisionService);
        verifyNoInteractions(dataQualityService);
    }

    @Test
    public void shouldReassessEveryScannedRecordWhenRewriteIsRequested() {
        // given
        stubPersons(person(1), person(2));

        when(revisionService.createRevisionFromCurrentState(anyString(), anyInt(), anyString()))
            .thenReturn(false);

        // when
        qualityAssessmentBackfillService.performBackfill(
            List.of(QualityAssessmentTarget.PERSON), null, null, "PTCRIS", true);

        // then
        verify(dataQualityService).reassessLatestRevision(EntityType.PERSON.name(), 1, "PTCRIS");
        verify(dataQualityService).reassessLatestRevision(EntityType.PERSON.name(), 2, "PTCRIS");
        verify(dataQualityService, never()).reassessLatestRevision(anyString(), eq(3), anyString());
    }

    @Test
    public void shouldNotFailWhenReassessmentThrows() {
        // given
        stubPersons(person(1), person(2));

        when(revisionService.createRevisionFromCurrentState(anyString(), anyInt(), anyString()))
            .thenReturn(false);
        when(dataQualityService.reassessLatestRevision(EntityType.PERSON.name(), 1, "PTCRIS"))
            .thenThrow(new RuntimeException("elasticsearch is down"));

        // when
        qualityAssessmentBackfillService.performBackfill(
            List.of(QualityAssessmentTarget.PERSON), null, null, "PTCRIS", true);

        // then
        verify(dataQualityService).reassessLatestRevision(EntityType.PERSON.name(), 2, "PTCRIS");
    }
}
