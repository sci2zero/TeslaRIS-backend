package rs.teslaris.core.unit;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import org.junit.jupiter.api.Test;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import rs.teslaris.core.indexmodel.DocumentPublicationIndex;
import rs.teslaris.core.indexmodel.PersonIndex;
import rs.teslaris.core.indexrepository.DocumentPublicationIndexRepository;
import rs.teslaris.core.model.document.Monograph;
import rs.teslaris.core.model.document.PersonDocumentContribution;
import rs.teslaris.core.model.person.DeclinedDocumentClaim;
import rs.teslaris.core.model.person.Person;
import rs.teslaris.core.repository.document.PersonDocumentContributionRepository;
import rs.teslaris.core.repository.person.DeclinedDocumentClaimRepository;
import rs.teslaris.core.service.impl.document.DocumentClaimingServiceImpl;
import rs.teslaris.core.service.interfaces.commontypes.SearchService;
import rs.teslaris.core.service.interfaces.document.DocumentPublicationService;
import rs.teslaris.core.service.interfaces.person.PersonService;

@SpringBootTest
public class DocumentClaimingServiceTest {

    @Mock
    private DocumentPublicationIndexRepository documentPublicationIndexRepository;

    @Mock
    private SearchService<PersonIndex> personSearchService;

    @Mock
    private PersonService personService;

    @Mock
    private PersonDocumentContributionRepository personDocumentContributionRepository;

    @Mock
    private DocumentPublicationService documentPublicationService;

    @Mock
    private DeclinedDocumentClaimRepository declinedDocumentClaimRepository;

    @InjectMocks
    private DocumentClaimingServiceImpl documentClaimingService;


    @Test
    public void shouldFindPotentialClaimsForPerson() {
        // Given
        var userId = 1;
        var pageable = PageRequest.of(0, 10);
        var personId = 42;
        var page = new PageImpl<>(List.of(new DocumentPublicationIndex()));

        when(personService.getPersonIdForUserId(userId)).thenReturn(personId);
        when(documentPublicationIndexRepository.findByClaimerIds(personId, pageable)).thenReturn(
            page);

        // When
        var result = documentClaimingService.findPotentialClaimsForPerson(userId, pageable);

        // Then
        assertEquals(page, result);
        verify(personService, times(1)).getPersonIdForUserId(userId);
        verify(documentPublicationIndexRepository, times(1)).findByClaimerIds(personId, pageable);
    }

    @Test
    void shouldClaimDocumentSuccessfully() {
        // Given
        var userId = 1;
        var personId = 2;
        var documentId = 3;

        var person = new Person();
        person.setId(personId);

        var contribution = new PersonDocumentContribution();
        contribution.setOrderNumber(1); // index + 1
        var document = new Monograph();
        document.setId(documentId);
        document.setContributors(Set.of(contribution));

        var documentIndex = new DocumentPublicationIndex();
        documentIndex.setDatabaseId(documentId);
        documentIndex.setClaimerIds(new ArrayList<>(List.of(personId)));
        documentIndex.setClaimerOrdinals(new ArrayList<>(List.of(0)));
        documentIndex.setAuthorIds(new ArrayList<>(List.of(-1)));

        when(personService.getPersonIdForUserId(userId)).thenReturn(personId);
        when(personService.findOne(personId)).thenReturn(person);
        when(documentPublicationService.findOne(documentId)).thenReturn(document);
        when(
            documentPublicationIndexRepository.findDocumentPublicationIndexByDatabaseId(documentId))
            .thenReturn(Optional.of(documentIndex));
        when(personDocumentContributionRepository.findUnmanagedContributionsForDocument(documentId))
            .thenReturn(List.of()); // Optional: not used in method logic

        // When
        documentClaimingService.claimDocument(userId, documentId);

        // Then
        assertEquals(person, contribution.getPerson());
        assertEquals(personId, documentIndex.getAuthorIds().get(0));
        assertTrue(documentIndex.getClaimerIds().isEmpty());
        assertTrue(documentIndex.getClaimerOrdinals().isEmpty());

        verify(documentPublicationService).save(document);
        verify(documentPublicationIndexRepository).save(documentIndex);
    }

    @Test
    void shouldNotClaimIfPersonIsNotClaimer() {
        // Given
        var userId = 1;
        var personId = 2;
        var documentId = 3;

        var person = new Person();
        person.setId(personId);

        var documentIndex = new DocumentPublicationIndex();
        documentIndex.setDatabaseId(documentId);
        documentIndex.setClaimerIds(new ArrayList<>(List.of(99))); // Not the person
        documentIndex.setClaimerOrdinals(new ArrayList<>(List.of(0)));

        when(personService.getPersonIdForUserId(userId)).thenReturn(personId);
        when(personService.findOne(personId)).thenReturn(person);
        when(
            documentPublicationIndexRepository.findDocumentPublicationIndexByDatabaseId(documentId))
            .thenReturn(Optional.of(documentIndex));
        when(personDocumentContributionRepository.findUnmanagedContributionsForDocument(documentId))
            .thenReturn(List.of());

        // When
        documentClaimingService.claimDocument(userId, documentId);

        // Then
        verify(documentPublicationService, never()).save(any());
        verify(documentPublicationIndexRepository, never()).save(any());
    }

    @Test
    void shouldNotClaimIfMatchingContributionIsMissing() {
        // Given
        var userId = 1;
        var personId = 2;
        var documentId = 3;

        var person = new Person();
        person.setId(personId);

        var document = new Monograph();
        document.setId(documentId);
        document.setContributors(Set.of()); // No contributors

        var documentIndex = new DocumentPublicationIndex();
        documentIndex.setDatabaseId(documentId);
        documentIndex.setClaimerIds(new ArrayList<>(List.of(personId)));
        documentIndex.setClaimerOrdinals(new ArrayList<>(List.of(0)));

        when(personService.getPersonIdForUserId(userId)).thenReturn(personId);
        when(personService.findOne(personId)).thenReturn(person);
        when(documentPublicationService.findOne(documentId)).thenReturn(document);
        when(
            documentPublicationIndexRepository.findDocumentPublicationIndexByDatabaseId(documentId))
            .thenReturn(Optional.of(documentIndex));
        when(personDocumentContributionRepository.findUnmanagedContributionsForDocument(documentId))
            .thenReturn(List.of());

        // When
        documentClaimingService.claimDocument(userId, documentId);

        // Then
        verify(documentPublicationService, never()).save(any());
        verify(documentPublicationIndexRepository, never()).save(any());
    }

    @Test
    void shouldDeclineDocumentClaimSuccessfully() {
        // Given
        var userId = 1;
        var personId = 2;
        var documentId = 3;

        var person = new Person();
        person.setId(personId);

        var document = new Monograph();
        document.setId(documentId);

        DocumentPublicationIndex documentIndex = new DocumentPublicationIndex();
        documentIndex.setDatabaseId(documentId);
        documentIndex.setClaimerIds(new ArrayList<>(List.of(personId)));

        when(personService.getPersonIdForUserId(userId)).thenReturn(personId);
        when(personService.findOne(personId)).thenReturn(person);
        when(documentPublicationService.findOne(documentId)).thenReturn(document);
        when(
            documentPublicationIndexRepository.findDocumentPublicationIndexByDatabaseId(documentId))
            .thenReturn(Optional.of(documentIndex));

        // When
        documentClaimingService.declineDocumentClaim(userId, documentId);

        // Then
        verify(declinedDocumentClaimRepository).save(any());
    }

    @Test
    void shouldHandleMissingDocumentIndexGracefully() {
        // Given
        var userId = 1;
        var personId = 2;
        var documentId = 3;

        var person = new Person();
        person.setId(personId);

        var document = new Monograph();
        document.setId(documentId);

        when(personService.getPersonIdForUserId(userId)).thenReturn(personId);
        when(personService.findOne(personId)).thenReturn(person);
        when(documentPublicationService.findOne(documentId)).thenReturn(document);
        when(
            documentPublicationIndexRepository.findDocumentPublicationIndexByDatabaseId(documentId))
            .thenReturn(Optional.empty());

        // When
        documentClaimingService.declineDocumentClaim(userId, documentId);

        // Then
        verify(declinedDocumentClaimRepository).save(any(DeclinedDocumentClaim.class));
        verify(documentPublicationIndexRepository, never()).save(any());
    }

    @Test
    void shouldThrowExceptionWhenPersonServiceFails() {
        // Given
        var userId = 1;
        var documentId = 3;

        when(personService.getPersonIdForUserId(userId))
            .thenThrow(new RuntimeException("Person service failed"));

        // When
        assertThrows(RuntimeException.class, () ->
            documentClaimingService.declineDocumentClaim(userId, documentId)
        );

        // Then
        verifyNoInteractions(declinedDocumentClaimRepository);
        verifyNoInteractions(documentPublicationService);
        verifyNoInteractions(documentPublicationIndexRepository);
    }
}
