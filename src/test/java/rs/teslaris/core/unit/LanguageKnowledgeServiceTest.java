package rs.teslaris.core.unit;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.isNull;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.Optional;
import java.util.Set;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.springframework.boot.test.context.SpringBootTest;
import rs.teslaris.core.dto.document.DocumentFileDTO;
import rs.teslaris.core.dto.document.DocumentFileResponseDTO;
import rs.teslaris.core.dto.person.LanguageKnowledgeRequestDTO;
import rs.teslaris.core.model.commontypes.Language;
import rs.teslaris.core.model.commontypes.LanguageTag;
import rs.teslaris.core.model.commontypes.MultiLingualContent;
import rs.teslaris.core.model.document.DocumentFile;
import rs.teslaris.core.model.person.LanguageKnowledge;
import rs.teslaris.core.model.person.LanguageLevel;
import rs.teslaris.core.model.person.Person;
import rs.teslaris.core.repository.person.LanguageKnowledgeRepository;
import rs.teslaris.core.service.impl.person.LanguageKnowledgeServiceImpl;
import rs.teslaris.core.service.interfaces.commontypes.LanguageService;
import rs.teslaris.core.service.interfaces.document.DocumentFileService;
import rs.teslaris.core.service.interfaces.person.PersonService;
import rs.teslaris.core.util.exceptionhandling.exception.ExpertiseOrSkillAlreadyExistsException;
import rs.teslaris.core.util.exceptionhandling.exception.NotFoundException;

@SpringBootTest
public class LanguageKnowledgeServiceTest {

    @Mock
    private LanguageKnowledgeRepository languageKnowledgeRepository;

    @Mock
    private PersonService personService;

    @Mock
    private LanguageService languageService;

    @Mock
    private DocumentFileService documentFileService;

    @InjectMocks
    private LanguageKnowledgeServiceImpl languageKnowledgeService;


    private Language language() {
        var languageTag = new LanguageTag();
        languageTag.setLanguageTag("EN");

        var language = new Language();
        language.setId(2);
        language.setLanguageCode("ENG");
        language.setName(Set.of(new MultiLingualContent(languageTag, "English", 1)));

        return language;
    }

    private LanguageKnowledgeRequestDTO requestDTO() {
        var dto = new LanguageKnowledgeRequestDTO();
        dto.setLanguageId(2);
        dto.setMotherTongue(false);
        dto.setReading(LanguageLevel.C1);
        dto.setOverall(LanguageLevel.B2);

        return dto;
    }

    @Test
    public void shouldAddLanguageKnowledge() {
        // Given
        var dto = requestDTO();

        when(personService.findOne(1)).thenReturn(new Person());
        when(languageService.findLanguageById(2)).thenReturn(language());
        when(languageKnowledgeRepository.save(any(LanguageKnowledge.class))).thenAnswer(
            invocation -> invocation.getArgument(0));

        // When
        var responseDTO = languageKnowledgeService.addLanguageKnowledge(1, dto);

        // Then
        assertNotNull(responseDTO);
        assertEquals(2, responseDTO.getLanguage().getId());
        assertEquals(LanguageLevel.C1, responseDTO.getReading());
        assertEquals(LanguageLevel.B2, responseDTO.getOverall());
    }

    @Test
    public void shouldDeriveNameFromLanguage() {
        // Given
        var dto = requestDTO();

        when(personService.findOne(1)).thenReturn(new Person());
        when(languageService.findLanguageById(2)).thenReturn(language());
        when(languageKnowledgeRepository.save(any(LanguageKnowledge.class))).thenAnswer(
            invocation -> invocation.getArgument(0));

        // When
        languageKnowledgeService.addLanguageKnowledge(1, dto);

        // Then
        var captor = ArgumentCaptor.forClass(LanguageKnowledge.class);
        verify(languageKnowledgeRepository).save(captor.capture());
        assertEquals(1, captor.getValue().getName().size());
        assertEquals("English", captor.getValue().getName().iterator().next().getContent());
    }

    @Test
    public void shouldThrowExceptionWhenLanguageIsADuplicate() {
        // Given
        var dto = requestDTO();

        when(personService.findOne(1)).thenReturn(new Person());
        when(languageKnowledgeRepository.existsForPersonWithLanguage(eq(1), eq(2),
            isNull())).thenReturn(true);

        // When
        assertThrows(ExpertiseOrSkillAlreadyExistsException.class,
            () -> languageKnowledgeService.addLanguageKnowledge(1, dto));

        // Then
        verify(languageKnowledgeRepository, never()).save(any(LanguageKnowledge.class));
    }

    @Test
    public void shouldThrowExceptionWhenMotherTongueAlreadyExists() {
        // Given
        var dto = requestDTO();
        dto.setMotherTongue(true);

        when(personService.findOne(1)).thenReturn(new Person());
        when(languageKnowledgeRepository.existsMotherTongueForPerson(eq(1), isNull())).thenReturn(
            true);

        // When
        assertThrows(ExpertiseOrSkillAlreadyExistsException.class,
            () -> languageKnowledgeService.addLanguageKnowledge(1, dto));

        // Then
        verify(languageKnowledgeRepository, never()).save(any(LanguageKnowledge.class));
    }

    @Test
    public void shouldUpdateLanguageKnowledge() {
        // Given
        var dto = requestDTO();
        dto.setWriting(LanguageLevel.A2);

        var person = new Person();
        person.setId(1);

        var languageKnowledge = new LanguageKnowledge();
        languageKnowledge.setId(3);
        languageKnowledge.setPerson(person);

        when(languageKnowledgeRepository.findById(3)).thenReturn(Optional.of(languageKnowledge));
        when(languageService.findLanguageById(2)).thenReturn(language());
        when(languageKnowledgeRepository.save(any(LanguageKnowledge.class))).thenAnswer(
            invocation -> invocation.getArgument(0));

        // When
        var responseDTO = languageKnowledgeService.updateLanguageKnowledge(3, dto);

        // Then
        assertNotNull(responseDTO);
        assertEquals(LanguageLevel.A2, responseDTO.getWriting());
        verify(languageKnowledgeRepository).existsForPersonWithLanguage(1, 2, 3);
    }

    @Test
    public void shouldDeleteLanguageKnowledge() {
        // Given
        var person = new Person();
        person.setId(1);

        var languageKnowledge = new LanguageKnowledge();
        languageKnowledge.setId(3);
        languageKnowledge.setPerson(person);
        person.getExpertisesAndSkills().add(languageKnowledge);

        when(languageKnowledgeRepository.findById(3)).thenReturn(Optional.of(languageKnowledge));

        // When
        languageKnowledgeService.deleteLanguageKnowledge(3, 1);

        // Then
        assertTrue(languageKnowledge.getDeleted());
        assertTrue(person.getExpertisesAndSkills().contains(languageKnowledge));
    }

    @Test
    public void shouldThrowExceptionWhenLanguageKnowledgeIsForAnotherPerson() {
        // Given
        var person = new Person();
        person.setId(2);

        var languageKnowledge = new LanguageKnowledge();
        languageKnowledge.setId(3);
        languageKnowledge.setPerson(person);

        when(languageKnowledgeRepository.findById(3)).thenReturn(Optional.of(languageKnowledge));

        // When
        assertThrows(NotFoundException.class,
            () -> languageKnowledgeService.deleteLanguageKnowledge(3, 1));

        // Then
        assertFalse(languageKnowledge.getDeleted());
    }

    @Test
    public void shouldAddProof() {
        // Given
        var proof = new DocumentFileDTO();
        var languageKnowledge = new LanguageKnowledge();
        languageKnowledge.setId(3);
        languageKnowledge.setPerson(new Person());

        var documentFile = new DocumentFile();
        documentFile.setId(1);

        when(languageKnowledgeRepository.findById(3)).thenReturn(Optional.of(languageKnowledge));
        when(documentFileService.saveNewPersonalDocument(proof, false,
            languageKnowledge.getPerson())).thenReturn(documentFile);

        // When
        var responseDTO = languageKnowledgeService.addProof(3, proof);

        // Then
        assertNotNull(responseDTO);
        assertEquals(1, responseDTO.getId());
        assertEquals(1, languageKnowledge.getProofs().size());
    }

    @Test
    public void shouldUpdateProof() {
        // Given
        var updatedProof = new DocumentFileDTO();
        updatedProof.setId(1);

        var documentFile = new DocumentFileResponseDTO();
        documentFile.setId(1);

        when(documentFileService.editDocumentFile(updatedProof, false)).thenReturn(documentFile);

        // When
        var responseDTO = languageKnowledgeService.updateProof(updatedProof);

        // Then
        assertNotNull(responseDTO);
        assertEquals(1, responseDTO.getId());
    }

    @Test
    public void shouldDeleteProof() {
        // Given
        var languageKnowledge = new LanguageKnowledge();
        var documentFile = new DocumentFile();
        documentFile.setId(1);
        languageKnowledge.getProofs().add(documentFile);

        when(languageKnowledgeRepository.findById(3)).thenReturn(Optional.of(languageKnowledge));
        when(documentFileService.findDocumentFileById(1)).thenReturn(documentFile);
        doNothing().when(documentFileService).deleteDocumentFile(anyString());

        // When
        languageKnowledgeService.deleteProof(1, 3);

        // Then
        assertTrue(languageKnowledge.getProofs().isEmpty());
    }
}
