package rs.teslaris.core.unit;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.List;
import java.util.Optional;
import java.util.Set;
import org.junit.jupiter.api.Test;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import rs.teslaris.core.indexmodel.DocumentPublicationIndex;
import rs.teslaris.core.indexrepository.DocumentPublicationIndexRepository;
import rs.teslaris.core.model.document.JournalPublication;
import rs.teslaris.core.model.document.PersonDocumentContribution;
import rs.teslaris.core.model.document.Thesis;
import rs.teslaris.core.model.document.ThesisResearchOutput;
import rs.teslaris.core.model.person.Person;
import rs.teslaris.core.repository.document.ThesisResearchOutputRepository;
import rs.teslaris.core.service.impl.document.ThesisResearchOutputServiceImpl;
import rs.teslaris.core.service.interfaces.document.DocumentPublicationService;
import rs.teslaris.core.service.interfaces.document.ThesisService;
import rs.teslaris.core.util.exceptionhandling.exception.NotFoundException;
import rs.teslaris.core.util.exceptionhandling.exception.ThesisException;

@SpringBootTest
public class ThesisResearchOutputServiceTest {

    @Mock
    private DocumentPublicationIndexRepository documentPublicationIndexRepository;

    @Mock
    private ThesisResearchOutputRepository thesisResearchOutputRepository;

    @Mock
    private ThesisService thesisService;

    @Mock
    private DocumentPublicationService documentPublicationService;

    @InjectMocks
    private ThesisResearchOutputServiceImpl thesisResearchOutputService;


    @Test
    void shouldThrowNotFoundExceptionWhenThesisDoesNotExist() {
        // Given
        var thesisId = 1;
        var pageable = PageRequest.of(0, 10);
        when(documentPublicationIndexRepository.findDocumentPublicationIndexByDatabaseId(thesisId))
            .thenReturn(Optional.empty());

        // When / Then
        assertThrows(NotFoundException.class, () ->
            thesisResearchOutputService.readResearchOutputsForThesis(thesisId, pageable));
        verify(documentPublicationIndexRepository).findDocumentPublicationIndexByDatabaseId(
            thesisId);
    }

    @Test
    void shouldReturnResearchOutputsWhenThesisExists() {
        // Given
        var thesisId = 1;
        var pageable = PageRequest.of(0, 10);
        var thesisIndex = mock(DocumentPublicationIndex.class);
        when(thesisIndex.getResearchOutputIds()).thenReturn(List.of(2, 3));
        when(documentPublicationIndexRepository.findDocumentPublicationIndexByDatabaseId(thesisId))
            .thenReturn(Optional.of(thesisIndex));
        Page<DocumentPublicationIndex> mockPage = mock(Page.class);
        when(
            documentPublicationIndexRepository.findDocumentPublicationIndexByDatabaseIdIn(anyList(),
                eq(pageable)))
            .thenReturn(mockPage);

        // When
        Page<DocumentPublicationIndex> result =
            thesisResearchOutputService.readResearchOutputsForThesis(thesisId, pageable);

        // Then
        assertNotNull(result);
        verify(documentPublicationIndexRepository).findDocumentPublicationIndexByDatabaseId(
            thesisId);
        verify(documentPublicationIndexRepository).findDocumentPublicationIndexByDatabaseIdIn(
            anyList(), eq(pageable));
    }

    @Test
    void shouldThrowExceptionWhenAddingThesisAsResearchOutputToItself() {
        // Given
        var thesisId = 1;

        // When / Then
        assertThrows(
            ThesisException.class,
            () -> thesisResearchOutputService.addResearchOutput(thesisId, thesisId));
    }

    @Test
    void shouldThrowExceptionWhenResearchOutputAlreadyExists() {
        // Given
        Integer thesisId = 1, researchOutputId = 2;
        when(thesisResearchOutputRepository.findByThesisIdAndResearchOutputId(thesisId,
            researchOutputId))
            .thenReturn(Optional.of(mock(ThesisResearchOutput.class)));

        // When / Then
        assertThrows(ThesisException.class,
            () -> thesisResearchOutputService.addResearchOutput(thesisId, researchOutputId));
    }

    @Test
    void shouldAddResearchOutputSuccessfully() {
        // Given
        Integer thesisId = 1, researchOutputId = 2;
        var thesis = mock(Thesis.class);
        var document = mock(JournalPublication.class);
        var person1 = new Person();
        person1.setId(1);

        var thesisContributor = new PersonDocumentContribution();
        thesisContributor.setPerson(person1);
        var documentContributor = new PersonDocumentContribution();
        documentContributor.setPerson(person1);

        when(thesisService.getThesisById(thesisId)).thenReturn(thesis);
        when(documentPublicationService.findOne(researchOutputId)).thenReturn(document);
        when(thesis.getContributors()).thenReturn(Set.of(thesisContributor));
        when(document.getContributors()).thenReturn(Set.of(documentContributor));

        // When
        thesisResearchOutputService.addResearchOutput(thesisId, researchOutputId);

        // Then
        verify(thesisResearchOutputRepository).save(any(ThesisResearchOutput.class));
    }

    @Test
    void shouldRemoveResearchOutputWhenExists() {
        // Given
        Integer thesisId = 1, researchOutputId = 2;
        var researchOutput = mock(ThesisResearchOutput.class);
        when(thesisResearchOutputRepository.findByThesisIdAndResearchOutputId(thesisId,
            researchOutputId))
            .thenReturn(Optional.of(researchOutput));

        // When
        thesisResearchOutputService.removeResearchOutput(thesisId, researchOutputId);

        // Then
        verify(thesisResearchOutputRepository).delete(researchOutput);
    }

    @Test
    void shouldThrowNotFoundExceptionWhenRemovingNonExistentResearchOutput() {
        // Given
        Integer thesisId = 1, researchOutputId = 2;
        when(thesisResearchOutputRepository.findByThesisIdAndResearchOutputId(thesisId,
            researchOutputId))
            .thenReturn(Optional.empty());

        // When / Then
        assertThrows(NotFoundException.class,
            () -> thesisResearchOutputService.removeResearchOutput(thesisId, researchOutputId));
    }

    @Test
    void shouldThrowExceptionWhenAuthorshipIsDisjoint() {
        // Given
        var thesis = mock(Thesis.class);
        var document = mock(JournalPublication.class);
        var person1 = new Person();
        person1.setId(1);
        var person2 = new Person();
        person2.setId(2);

        var thesisContributor = new PersonDocumentContribution();
        thesisContributor.setPerson(person1);
        var documentContributor = new PersonDocumentContribution();
        documentContributor.setPerson(person2);

        when(thesisService.getThesisById(1)).thenReturn(thesis);
        when(documentPublicationService.findOne(2)).thenReturn(document);
        when(thesis.getContributors()).thenReturn(Set.of(thesisContributor));
        when(document.getContributors()).thenReturn(Set.of(documentContributor));

        // When
        ThesisException exception = assertThrows(ThesisException.class,
            () -> thesisResearchOutputService.addResearchOutput(1, 2));

        // Then
        assertEquals("notYourPublicationMessage", exception.getMessage());
    }
}
