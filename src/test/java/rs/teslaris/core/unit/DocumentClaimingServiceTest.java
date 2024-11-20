package rs.teslaris.core.unit;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
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
import rs.teslaris.core.model.document.AffiliationStatement;
import rs.teslaris.core.model.document.Monograph;
import rs.teslaris.core.model.document.PersonDocumentContribution;
import rs.teslaris.core.model.person.DeclinedDocumentClaim;
import rs.teslaris.core.model.person.Person;
import rs.teslaris.core.model.person.PersonName;
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
    public void shouldClaimDocument() {
        // Given
        var userId = 1;
        var documentId = 123;
        var personId = 42;
        var person = new Person();
        person.setId(personId);
        person.setName(new PersonName("John", null, "Doe", null, null));
        person.setOtherNames(Set.of(new PersonName("J.", null, "Doe", null, null)));

        var contribution1 = new PersonDocumentContribution();
        var affiliationStatement1 = new AffiliationStatement();
        affiliationStatement1.setDisplayPersonName(new PersonName("John", null, "Doe", null, null));
        contribution1.setAffiliationStatement(affiliationStatement1);
        var contribution2 = new PersonDocumentContribution();
        var affiliationStatement2 = new AffiliationStatement();
        affiliationStatement2.setDisplayPersonName(new PersonName("John", null, "Doe", null, null));
        contribution2.setAffiliationStatement(affiliationStatement1);

        var document = new DocumentPublicationIndex();
        document.setDatabaseId(documentId);
        document.setAuthorIds(new ArrayList<>(List.of(-1, -1)));
        document.setClaimerIds(new ArrayList<>(List.of(personId)));

        when(personService.getPersonIdForUserId(userId)).thenReturn(personId);
        when(personService.findOne(personId)).thenReturn(person);
        when(personDocumentContributionRepository.findUnmanagedContributionsForDocument(documentId))
            .thenReturn(List.of(contribution1, contribution2));
        when(
            documentPublicationIndexRepository.findDocumentPublicationIndexByDatabaseId(documentId))
            .thenReturn(Optional.of(document));

        // When
        documentClaimingService.claimDocument(userId, documentId);

        // Then
        verify(personService, times(1)).getPersonIdForUserId(userId);
        verify(personService, times(1)).findOne(personId);
        verify(personDocumentContributionRepository, times(1))
            .findUnmanagedContributionsForDocument(documentId);
        verify(personDocumentContributionRepository, times(1)).save(any());

        assertFalse(document.getClaimerIds().contains(personId));
        verify(documentPublicationIndexRepository, times(1)).save(any());
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
