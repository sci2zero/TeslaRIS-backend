package rs.teslaris.core.unit;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.Test;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import rs.teslaris.core.indexmodel.DocumentPublicationIndex;
import rs.teslaris.core.indexmodel.DocumentPublicationType;
import rs.teslaris.core.indexrepository.DocumentPublicationIndexRepository;
import rs.teslaris.core.model.document.Journal;
import rs.teslaris.core.model.document.JournalPublication;
import rs.teslaris.core.model.document.PersonDocumentContribution;
import rs.teslaris.core.model.person.Person;
import rs.teslaris.core.repository.document.DocumentRepository;
import rs.teslaris.core.repository.document.JournalPublicationRepository;
import rs.teslaris.core.service.impl.comparator.MergeServiceImpl;
import rs.teslaris.core.service.interfaces.document.DocumentPublicationService;
import rs.teslaris.core.service.interfaces.document.JournalPublicationService;
import rs.teslaris.core.service.interfaces.document.JournalService;
import rs.teslaris.core.service.interfaces.person.PersonService;

@SpringBootTest
public class MergerServiceTest {

    @Mock
    private JournalService journalService;

    @Mock
    private JournalPublicationService journalPublicationService;

    @Mock
    private JournalPublicationRepository journalPublicationRepository;

    @Mock
    private DocumentPublicationIndexRepository documentPublicationIndexRepository;

    @Mock
    private DocumentPublicationService documentPublicationService;

    @Mock
    private DocumentRepository documentRepository;

    @Mock
    private PersonService personService;

    @InjectMocks
    private MergeServiceImpl mergeService;


    @Test
    void switchJournalPublicationToOtherJournal_shouldPerformSwitch() {
        var targetJournalId = 1;
        var publicationId = 2;

        var publication = new JournalPublication();
        when(journalPublicationRepository.findById(publicationId)).thenReturn(
            Optional.of(publication));
        var targetJournal = new Journal();
        when(journalService.findJournalById(targetJournalId)).thenReturn(targetJournal);

        mergeService.switchJournalPublicationToOtherJournal(targetJournalId, publicationId);

        verify(journalPublicationRepository).findById(publicationId);
        verify(journalService).findJournalById(targetJournalId);
        verify(journalPublicationRepository).save(publication);
        assertEquals(targetJournal, publication.getJournal());
    }

    @Test
    void switchAllPublicationsToOtherJournal_shouldPerformSwitchForAll() {
        var sourceId = 1;
        var targetId = 2;

        var publicationIndex1 = new DocumentPublicationIndex();
        publicationIndex1.setDatabaseId(1);
        var publicationIndex2 = new DocumentPublicationIndex();
        publicationIndex2.setDatabaseId(2);
        var page1 = new PageImpl<>(
            List.of(publicationIndex1, publicationIndex2));
        var page2 = new PageImpl<DocumentPublicationIndex>(List.of());

        when(documentPublicationIndexRepository.findByTypeAndJournalId(
            DocumentPublicationType.JOURNAL_PUBLICATION.name(), sourceId, PageRequest.of(0, 10)))
            .thenReturn(page1);
        when(documentPublicationIndexRepository.findByTypeAndJournalId(
            DocumentPublicationType.JOURNAL_PUBLICATION.name(), sourceId, PageRequest.of(1, 10)))
            .thenReturn(page2);

        var publication1 = new JournalPublication();
        var publication2 = new JournalPublication();
        when(journalPublicationRepository.findById(publicationIndex1.getDatabaseId())).thenReturn(
            Optional.of(publication1));
        when(journalPublicationRepository.findById(publicationIndex2.getDatabaseId())).thenReturn(
            Optional.of(publication2));

        Journal targetJournal = new Journal();
        when(journalService.findJournalById(targetId)).thenReturn(targetJournal);

        mergeService.switchAllPublicationsToOtherJournal(sourceId, targetId);

        verify(journalPublicationRepository, times(2)).save(any(JournalPublication.class));
        verify(documentPublicationIndexRepository, times(1)).findByTypeAndJournalId(
            eq(DocumentPublicationType.JOURNAL_PUBLICATION.name()), eq(sourceId),
            any(PageRequest.class));
        assertEquals(targetJournal, publication1.getJournal());
        assertEquals(targetJournal, publication2.getJournal());
    }

    @Test
    void switchPublicationToOtherPerson_shouldPerformSwitch() {
        // Given
        var sourcePersonId = 1;
        var targetPersonId = 2;
        var publicationId = 2;

        var publication = new JournalPublication();
        var contribution = new PersonDocumentContribution();
        var contributor = new Person();
        contributor.setId(sourcePersonId);
        contribution.setPerson(contributor);

        publication.getContributors().add(contribution);
        when(documentPublicationService.findDocumentById(publicationId)).thenReturn(publication);

        var otherPerson = new Person();
        otherPerson.setId(targetPersonId);
        when(personService.findOne(targetPersonId)).thenReturn(otherPerson);

        // When
        mergeService.switchPublicationToOtherPerson(sourcePersonId, targetPersonId, publicationId);

        // Then
        assertEquals(contribution.getPerson().getId(), targetPersonId);
        verify(documentRepository).save(publication);
    }

    @Test
    void switchAllPublicationsToOtherPerson_shouldPerformSwitchForAll() {
        var sourceId = 1;
        var targetId = 2;

        var publicationIndex1 = new DocumentPublicationIndex();
        publicationIndex1.setDatabaseId(1);
        var publicationIndex2 = new DocumentPublicationIndex();
        publicationIndex2.setDatabaseId(2);
        var page1 = new PageImpl<>(
            List.of(publicationIndex1, publicationIndex2));
        var page2 = new PageImpl<DocumentPublicationIndex>(List.of());

        when(documentPublicationService.findResearcherPublications(sourceId,
            PageRequest.of(0, 10))).thenReturn(page1);
        when(documentPublicationService.findResearcherPublications(sourceId,
            PageRequest.of(1, 10))).thenReturn(page2);

        var contribution = new PersonDocumentContribution();
        var contributor = new Person();
        contributor.setId(sourceId);
        contribution.setPerson(contributor);

        var publication1 = new JournalPublication();
        publication1.addDocumentContribution(contribution);
        var publication2 = new JournalPublication();
        publication2.addDocumentContribution(contribution);

        when(documentPublicationService.findDocumentById(
            publicationIndex1.getDatabaseId())).thenReturn(publication1);
        when(documentPublicationService.findDocumentById(
            publicationIndex2.getDatabaseId())).thenReturn(publication2);

        var otherPerson = new Person();
        otherPerson.setId(targetId);
        when(personService.findOne(targetId)).thenReturn(otherPerson);

        mergeService.switchAllPublicationToOtherPerson(sourceId, targetId);

        verify(documentRepository, times(2)).save(any(JournalPublication.class));
        assertEquals(contribution.getPerson().getId(), targetId);
    }
}
