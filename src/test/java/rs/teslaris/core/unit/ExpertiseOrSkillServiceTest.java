package rs.teslaris.core.unit;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.HashSet;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.Test;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.springframework.boot.test.context.SpringBootTest;
import rs.teslaris.core.dto.document.DocumentFileDTO;
import rs.teslaris.core.dto.document.DocumentFileResponseDTO;
import rs.teslaris.core.dto.person.ExpertiseOrSkillDTO;
import rs.teslaris.core.model.document.DocumentFile;
import rs.teslaris.core.model.person.ExpertiseOrSkill;
import rs.teslaris.core.model.person.Person;
import rs.teslaris.core.repository.person.ExpertiseOrSkillRepository;
import rs.teslaris.core.service.impl.person.ExpertiseOrSkillServiceImpl;
import rs.teslaris.core.service.interfaces.commontypes.MultilingualContentService;
import rs.teslaris.core.service.interfaces.document.DocumentFileService;
import rs.teslaris.core.service.interfaces.person.PersonService;

@SpringBootTest
public class ExpertiseOrSkillServiceTest {

    @Mock
    private ExpertiseOrSkillRepository expertiseOrSkillRepository;

    @Mock
    private PersonService personService;

    @Mock
    private MultilingualContentService multilingualContentService;

    @Mock
    private DocumentFileService documentFileService;

    @InjectMocks
    private ExpertiseOrSkillServiceImpl expertiseOrSkillService;


    @Test
    public void shouldAddExpertiseOrSkill() {
        // Given
        var dto = new ExpertiseOrSkillDTO();

        var person = new Person();

        var newExpertiseOrSkill = new ExpertiseOrSkill();

        when(personService.findOne(1)).thenReturn(person);
        when(expertiseOrSkillRepository.save(any(ExpertiseOrSkill.class))).thenReturn(
            newExpertiseOrSkill);

        // When
        var responseDTO = expertiseOrSkillService.addExpertiseOrSkill(1, dto);

        // Then
        assertNotNull(responseDTO);
    }

    @Test
    public void shouldUpdateExpertiseOrSkill() {
        // Given
        ExpertiseOrSkillDTO dto = new ExpertiseOrSkillDTO();

        ExpertiseOrSkill expertiseOrSkill = new ExpertiseOrSkill();
        expertiseOrSkill.setId(1);

        when(expertiseOrSkillRepository.findById(1)).thenReturn(
            Optional.of(new ExpertiseOrSkill()));
        when(expertiseOrSkillRepository.save(any(ExpertiseOrSkill.class))).thenReturn(
            expertiseOrSkill);

        // When
        var responseDTO = expertiseOrSkillService.updateExpertiseOrSkill(1, dto);

        // Then
        assertNotNull(responseDTO);
    }

    @Test
    public void shouldDeleteExpertiseOrSkill() {
        // Given
        var person = new Person();
        var expertiseOrSkill = new ExpertiseOrSkill();
        expertiseOrSkill.setId(1);
        var expertiseOrSkills = new HashSet<ExpertiseOrSkill>();
        expertiseOrSkills.add(expertiseOrSkill);
        person.setExpertisesAndSkills(expertiseOrSkills);

        when(personService.findOne(1)).thenReturn(person);
        when(expertiseOrSkillRepository.findById(1)).thenReturn(
            Optional.of(new ExpertiseOrSkill()));
        doNothing().when(expertiseOrSkillRepository).deleteById(1);

        // When
        expertiseOrSkillService.deleteExpertiseOrSkill(1, 1);

        // Then
        assertTrue(person.getExpertisesAndSkills().isEmpty());
    }

    @Test
    public void shouldAddProof() {
        // Given
        var proof = new DocumentFileDTO();
        var expertiseOrSkill = new ExpertiseOrSkill();
        expertiseOrSkill.setId(1);
        var documentFile = new DocumentFile();
        documentFile.setId(1);

        when(expertiseOrSkillRepository.findById(1)).thenReturn(Optional.of(expertiseOrSkill));
        when(documentFileService.saveNewDocument(proof, false)).thenReturn(documentFile);

        // When
        var responseDTO = expertiseOrSkillService.addProof(1, proof);

        // Then
        assertNotNull(responseDTO);
        assertEquals(1, responseDTO.getId());
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
        var responseDTO = expertiseOrSkillService.updateProof(updatedProof);

        // Then
        assertNotNull(responseDTO);
        assertEquals(1, responseDTO.getId());
    }

    @Test
    public void shouldDeleteProof() {
        // Given
        ExpertiseOrSkill expertiseOrSkill = new ExpertiseOrSkill();
        DocumentFile documentFile = new DocumentFile();
        documentFile.setId(1);
        expertiseOrSkill.getProofs().add(documentFile);

        when(expertiseOrSkillRepository.findById(1)).thenReturn(Optional.of(expertiseOrSkill));
        when(documentFileService.findDocumentFileById(1)).thenReturn(documentFile);
        doNothing().when(documentFileService).deleteDocumentFile(anyString());

        // When
        expertiseOrSkillService.deleteProof(1, 1);

        // Then
        assertTrue(expertiseOrSkill.getProofs().isEmpty());
    }

    @Test
    void testSwitchSkills() {
        // Given
        var sourcePersonId = 1;
        var targetPersonId = 2;
        var skillIds = List.of(100, 101, 102);

        var sourcePerson = new Person();
        sourcePerson.setId(sourcePersonId);
        var sourceSkills = new HashSet<ExpertiseOrSkill>();
        skillIds.forEach(id -> {
            var skill = new ExpertiseOrSkill();
            skill.setId(id);
            sourceSkills.add(skill);
        });
        sourcePerson.setExpertisesAndSkills(sourceSkills);

        var targetPerson = new Person();
        targetPerson.setId(targetPersonId);
        var targetSkills = new HashSet<ExpertiseOrSkill>();
        targetPerson.setExpertisesAndSkills(targetSkills);

        when(personService.findOne(sourcePersonId)).thenReturn(sourcePerson);
        when(personService.findOne(targetPersonId)).thenReturn(targetPerson);
        skillIds.forEach(id -> {
            var skill = new ExpertiseOrSkill();
            skill.setId(id);
            when(expertiseOrSkillRepository.findById(id)).thenReturn(Optional.of(skill));
        });

        // When
        expertiseOrSkillService.switchSkills(skillIds, sourcePersonId, targetPersonId);

        // Then
        skillIds.forEach(id -> {
            var skill = new ExpertiseOrSkill();
            skill.setId(id);
            verify(expertiseOrSkillRepository).findById(id);
            assertFalse(sourcePerson.getExpertisesAndSkills().contains(skill));
            assertTrue(targetPerson.getExpertisesAndSkills().contains(skill));
        });
        verify(personService).save(sourcePerson);
        verify(personService).save(targetPerson);
    }
}
