package rs.teslaris.core.unit.thesislibrary;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.time.LocalDate;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.springframework.boot.test.context.SpringBootTest;
import rs.teslaris.core.dto.person.ContactDTO;
import rs.teslaris.core.dto.person.PersonNameDTO;
import rs.teslaris.core.model.document.Thesis;
import rs.teslaris.core.service.interfaces.document.ThesisService;
import rs.teslaris.core.util.exceptionhandling.exception.LoadingException;
import rs.teslaris.core.util.exceptionhandling.exception.StorageException;
import rs.teslaris.thesislibrary.dto.DissertationInformationDTO;
import rs.teslaris.thesislibrary.dto.PreviousTitleInformationDTO;
import rs.teslaris.thesislibrary.dto.RegistryBookContactInformationDTO;
import rs.teslaris.thesislibrary.dto.RegistryBookEntryDTO;
import rs.teslaris.thesislibrary.dto.RegistryBookPersonalInformationDTO;
import rs.teslaris.thesislibrary.model.RegistryBookEntryDraft;
import rs.teslaris.thesislibrary.repository.RegistryBookEntryDraftRepository;
import rs.teslaris.thesislibrary.service.impl.RegistryBookDraftServiceImpl;

@SpringBootTest
public class RegistryBookDraftServiceTest {

    @Mock
    private RegistryBookEntryDraftRepository registryBookEntryDraftRepository;

    @Mock
    private ThesisService thesisService;

    @InjectMocks
    private RegistryBookDraftServiceImpl registryBookDraftService;

    private Thesis thesis;
    private RegistryBookEntryDTO draftDTO;
    private RegistryBookEntryDraft savedDraft;

    @BeforeEach
    void setUp() {
        thesis = new Thesis();
        thesis.setId(1);

        draftDTO = new RegistryBookEntryDTO();
        draftDTO.setId(1);
        draftDTO.setDissertationInformation(new DissertationInformationDTO());
        draftDTO.getDissertationInformation().setDefenceDate(LocalDate.of(2024, 5, 15));
        draftDTO.setPersonalInformation(new RegistryBookPersonalInformationDTO());
        draftDTO.setContactInformation(new RegistryBookContactInformationDTO());
        draftDTO.setPreviousTitleInformation(new PreviousTitleInformationDTO());

        savedDraft = new RegistryBookEntryDraft();
        savedDraft.setId(1);
        savedDraft.setThesis(thesis);
        savedDraft.setDraftData("{\"dissertationInformation\":{\"defenceDate\":\"2024-05-15\"}}");
    }

    @Test
    void shouldReturnNullWhenNoDraftExists() {
        // Given
        var thesisId = 1;
        when(registryBookEntryDraftRepository.findByThesisId(thesisId)).thenReturn(
            Optional.empty());

        // When
        var result = registryBookDraftService.fetchRegistryBookEntryDraft(thesisId);

        // Then
        verify(registryBookEntryDraftRepository).findByThesisId(eq(thesisId));
        assertNull(result);
    }

    @Test
    void shouldFetchExistingDraftSuccessfully() {
        // Given
        var thesisId = 1;
        var jsonDraft = """
            {
                "id": 1,
                "dissertationInformation": {
                    "defenceDate": "2024-05-15"
                },
                "personalInformation": {
                    "authorName": {
                        "firstname": "John",
                        "lastname": "Doe"
                    }
                },
                "contactInformation": {
                    "contact": {
                        "contactEmail": "john.doe@example.com"
                    }
                },
                "previousTitleInformation": {
                    "academicTitle": "MAGISTER_STUDIES"
                },
                "inPromotion": false,
                "promoted": false
            }
            """;

        savedDraft.setDraftData(jsonDraft);
        when(registryBookEntryDraftRepository.findByThesisId(thesisId)).thenReturn(
            Optional.of(savedDraft));

        // When
        var result = registryBookDraftService.fetchRegistryBookEntryDraft(thesisId);

        // Then
        verify(registryBookEntryDraftRepository).findByThesisId(eq(thesisId));
        assertNotNull(result);
        assertEquals(1, result.getId());
        assertNotNull(result.getDissertationInformation());
        assertEquals(LocalDate.of(2024, 5, 15),
            result.getDissertationInformation().getDefenceDate());
        assertNotNull(result.getPersonalInformation());
        assertEquals("John", result.getPersonalInformation().getAuthorName().getFirstname());
        assertEquals("Doe", result.getPersonalInformation().getAuthorName().getLastname());
        assertNotNull(result.getContactInformation());
        assertEquals("john.doe@example.com",
            result.getContactInformation().getContact().getContactEmail());
        assertNotNull(result.getPreviousTitleInformation());
        assertEquals("MAGISTER_STUDIES",
            result.getPreviousTitleInformation().getAcademicTitle().name());
        assertFalse(result.getInPromotion());
        assertFalse(result.getPromoted());
    }

    @Test
    void shouldThrowLoadingExceptionWhenDraftJsonIsInvalid() {
        // Given
        var thesisId = 1;
        savedDraft.setDraftData("{invalid json}");
        when(registryBookEntryDraftRepository.findByThesisId(thesisId)).thenReturn(
            Optional.of(savedDraft));

        // When & Then
        var exception = assertThrows(LoadingException.class,
            () -> registryBookDraftService.fetchRegistryBookEntryDraft(thesisId));

        verify(registryBookEntryDraftRepository).findByThesisId(eq(thesisId));
        assertEquals("failedToParseDraftMessage", exception.getMessage());
    }

    @Test
    void shouldSaveNewDraftWhenNoExistingDraft() {
        // Given
        var thesisId = 1;

        when(thesisService.getThesisById(thesisId)).thenReturn(thesis);
        when(registryBookEntryDraftRepository.save(any(RegistryBookEntryDraft.class)))
            .thenAnswer(invocation -> {
                var draft = invocation.getArgument(0, RegistryBookEntryDraft.class);
                draft.setId(1);
                return draft;
            });

        // When
        registryBookDraftService.saveRegistryBookEntryDraft(draftDTO, thesisId);

        // Then
        verify(thesisService).getThesisById(eq(thesisId));
        verify(registryBookEntryDraftRepository).deleteByThesisId(eq(thesisId));
    }

    @Test
    void shouldDeleteExistingDraftAndSaveNewOne() {
        // Given
        var thesisId = 1;
        var existingDraft = new RegistryBookEntryDraft();
        existingDraft.setId(99);
        existingDraft.setThesis(thesis);
        existingDraft.setDraftData("old draft data");

        when(thesisService.getThesisById(thesisId)).thenReturn(thesis);
        when(registryBookEntryDraftRepository.findByThesisId(thesisId))
            .thenReturn(Optional.of(existingDraft));
        when(registryBookEntryDraftRepository.save(any(RegistryBookEntryDraft.class)))
            .thenAnswer(invocation -> invocation.getArgument(0));

        // When
        registryBookDraftService.saveRegistryBookEntryDraft(draftDTO, thesisId);

        // Then
        verify(registryBookEntryDraftRepository).deleteByThesisId(eq(thesisId));
    }

    @Test
    void shouldHandlePartialDraftDataSuccessfully() {
        // Given
        var thesisId = 1;
        var partialDraftDTO = new RegistryBookEntryDTO();
        partialDraftDTO.setDissertationInformation(new DissertationInformationDTO());
        partialDraftDTO.getDissertationInformation().setDefenceDate(LocalDate.of(2024, 5, 15));

        when(thesisService.getThesisById(thesisId)).thenReturn(thesis);
        when(registryBookEntryDraftRepository.save(any(RegistryBookEntryDraft.class)))
            .thenAnswer(invocation -> invocation.getArgument(0));

        // When
        registryBookDraftService.saveRegistryBookEntryDraft(partialDraftDTO, thesisId);

        // Then
        verify(registryBookEntryDraftRepository).save(argThat(draft -> {
            var json = draft.getDraftData();
            assertTrue(json.contains("defenceDate"));
            assertTrue(json.contains("2024,5,15"));
            return true;
        }));
    }

    @Test
    void shouldThrowStorageExceptionWhenJsonSerializationFails() {
        // Given
        var thesisId = 1;

        var problematicDraftDTO = new RegistryBookEntryDTO() {
            @Override
            public DissertationInformationDTO getDissertationInformation() {
                return new DissertationInformationDTO() {
                    @Override
                    public LocalDate getDefenceDate() {
                        throw new RuntimeException("Simulated serialization error");
                    }
                };
            }
        };

        when(thesisService.getThesisById(thesisId)).thenReturn(thesis);

        // When & Then
        var exception = assertThrows(StorageException.class,
            () -> registryBookDraftService.saveRegistryBookEntryDraft(problematicDraftDTO,
                thesisId));

        assertEquals("failedToStoreDraftMessage", exception.getMessage());
        verify(registryBookEntryDraftRepository, never()).save(any());
    }

    @Test
    void shouldHandleNullFieldsInDraftDTO() {
        // Given
        var thesisId = 1;
        var nullDraftDTO = new RegistryBookEntryDTO();
        nullDraftDTO.setId(null);
        nullDraftDTO.setDissertationInformation(null);
        nullDraftDTO.setPersonalInformation(null);
        nullDraftDTO.setContactInformation(null);
        nullDraftDTO.setPreviousTitleInformation(null);

        when(thesisService.getThesisById(thesisId)).thenReturn(thesis);
        when(registryBookEntryDraftRepository.save(any(RegistryBookEntryDraft.class)))
            .thenAnswer(invocation -> invocation.getArgument(0));

        // When
        registryBookDraftService.saveRegistryBookEntryDraft(nullDraftDTO, thesisId);

        // Then
        verify(registryBookEntryDraftRepository).save(argThat(draft -> {
            var json = draft.getDraftData();
            assertNotNull(json);
            assertFalse(json.contains("dissertationInformation"));
            assertFalse(json.contains("personalInformation"));
            return true;
        }));
    }

    @Test
    void shouldSaveDraftWithNestedObjectsSuccessfully() {
        // Given
        var thesisId = 1;
        var complexDraftDTO = new RegistryBookEntryDTO();

        var dissertationInfo = new DissertationInformationDTO();
        dissertationInfo.setDefenceDate(LocalDate.of(2024, 5, 15));
        dissertationInfo.setDissertationTitle("Advanced AI Research");
        complexDraftDTO.setDissertationInformation(dissertationInfo);

        var personalInfo = new RegistryBookPersonalInformationDTO();
        var name = new PersonNameDTO();
        name.setFirstname("John");
        name.setLastname("Doe");
        personalInfo.setAuthorName(name);
        complexDraftDTO.setPersonalInformation(personalInfo);

        var contactInfo = new RegistryBookContactInformationDTO();
        var contact = new ContactDTO();
        contact.setContactEmail("john.doe@example.com");
        contact.setPhoneNumber("+381641234567");
        contactInfo.setContact(contact);
        complexDraftDTO.setContactInformation(contactInfo);

        var previousTitleInfo = new PreviousTitleInformationDTO();
        previousTitleInfo.setInstitutionName("University of Belgrade");
        complexDraftDTO.setPreviousTitleInformation(previousTitleInfo);

        complexDraftDTO.setInPromotion(true);
        complexDraftDTO.setPromoted(false);
        complexDraftDTO.setPromotionSchoolYear("2023/2024");
        complexDraftDTO.setRegistryBookNumber(123);

        when(thesisService.getThesisById(thesisId)).thenReturn(thesis);
        when(registryBookEntryDraftRepository.save(any(RegistryBookEntryDraft.class)))
            .thenAnswer(invocation -> invocation.getArgument(0));

        // When
        registryBookDraftService.saveRegistryBookEntryDraft(complexDraftDTO, thesisId);

        // Then
        verify(registryBookEntryDraftRepository).save(argThat(draft -> {
            var json = draft.getDraftData();
            assertTrue(json.contains("Advanced AI Research"));
            assertTrue(json.contains("John"));
            assertTrue(json.contains("Doe"));
            assertTrue(json.contains("john.doe@example.com"));
            assertTrue(json.contains("2023/2024"));
            assertTrue(json.contains("123"));
            assertTrue(json.contains("inPromotion\":true"));
            assertTrue(json.contains("promoted\":false"));
            return true;
        }));
    }
}
