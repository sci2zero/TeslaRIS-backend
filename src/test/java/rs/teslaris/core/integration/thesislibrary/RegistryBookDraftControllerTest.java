package rs.teslaris.core.integration.thesislibrary;

import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.fasterxml.jackson.databind.ObjectMapper;
import java.time.LocalDate;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders;
import rs.teslaris.core.dto.person.ContactDTO;
import rs.teslaris.core.dto.person.PersonNameDTO;
import rs.teslaris.core.integration.BaseTest;
import rs.teslaris.thesislibrary.dto.DissertationInformationDTO;
import rs.teslaris.thesislibrary.dto.PreviousTitleInformationDTO;
import rs.teslaris.thesislibrary.dto.RegistryBookContactInformationDTO;
import rs.teslaris.thesislibrary.dto.RegistryBookEntryDTO;
import rs.teslaris.thesislibrary.dto.RegistryBookPersonalInformationDTO;
import rs.teslaris.thesislibrary.model.AcademicTitle;

@SpringBootTest
public class RegistryBookDraftControllerTest extends BaseTest {

    @Autowired
    private ObjectMapper objectMapper;


    private RegistryBookEntryDTO getTestPayload() {
        var dto = new RegistryBookEntryDTO();

        var dissertationInfo = new DissertationInformationDTO();
        dissertationInfo.setDissertationTitle("Dissertation title");
        dissertationInfo.setOrganisationUnitId(2);
        dissertationInfo.setMentor("Mentor");
        dissertationInfo.setCommission("Commission");
        dissertationInfo.setGrade("10");
        dissertationInfo.setDiplomaSupplementsNumber("456");
        dissertationInfo.setDiplomaSupplementsIssueDate(LocalDate.now());
        dto.setDissertationInformation(dissertationInfo);

        var personalInfo = new RegistryBookPersonalInformationDTO();
        var name = new PersonNameDTO(null, "John", "H", "Doe", null, null);
        personalInfo.setAuthorName(name);
        personalInfo.setMunicipalityOfBrith("Municipality");
        personalInfo.setCountryOfBirthId(3);
        personalInfo.setFatherName("Tyron");
        personalInfo.setGuardianNameAndSurname("Aunt May");
        dto.setPersonalInformation(personalInfo);

        var contactInfo = new RegistryBookContactInformationDTO();
        contactInfo.setMunicipality("Municipality");
        contactInfo.setPostalCode("10000");
        contactInfo.setContact(new ContactDTO("john@example.com", "+123456"));
        dto.setContactInformation(contactInfo);

        var prevTitle = new PreviousTitleInformationDTO();
        prevTitle.setInstitutionName("University");
        prevTitle.setGraduationDate(LocalDate.of(2015, 6, 15));
        prevTitle.setInstitutionPlace("Town");
        prevTitle.setAcademicTitle(AcademicTitle.MASTER_ACADEMIC_STUDIES);
        dto.setPreviousTitleInformation(prevTitle);

        return dto;
    }

    @Test
    @WithMockUser(username = "test.admin@test.com", password = "testAdmin")
    public void testCreateRegistryBookEntry() throws Exception {
        String jwtToken = authenticateAdminAndGetToken();
        var request = getTestPayload();

        String requestBody = objectMapper.writeValueAsString(request);
        mockMvc.perform(
                MockMvcRequestBuilders.patch("http://localhost:8081/api/registry-book-draft/{thesisId}",
                        17)
                    .content(requestBody).contentType(MediaType.APPLICATION_JSON)
                    .header(HttpHeaders.AUTHORIZATION, "Bearer " + jwtToken)
                    .header("Idempotency-Key", "MOCK_KEY_REGISTRY_BOOK_ENTRY_DRAFT"))
            .andExpect(status().isNoContent());
    }

    @Test
    @WithMockUser(username = "test.admin@test.com", password = "testAdmin")
    public void testGetEntryPrePopulatedData() throws Exception {
        String jwtToken = authenticateAdminAndGetToken();
        mockMvc.perform(
                MockMvcRequestBuilders.get("http://localhost:8081/api/registry-book-draft/{thesisId}",
                        17)
                    .header(HttpHeaders.AUTHORIZATION, "Bearer " + jwtToken)
                    .header("Idempotency-Key", "MOCK_KEY_REGISTRY_BOOK_ENTRY_DRAFT"))
            .andExpect(status().isOk());
    }
}
