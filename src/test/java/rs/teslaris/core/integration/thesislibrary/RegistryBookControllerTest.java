package rs.teslaris.core.integration.thesislibrary;

import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.fasterxml.jackson.databind.ObjectMapper;
import java.time.LocalDate;
import org.junit.jupiter.api.MethodOrderer;
import org.junit.jupiter.api.Order;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestMethodOrder;
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

@SpringBootTest
@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
public class RegistryBookControllerTest extends BaseTest {

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
        dissertationInfo.setAcquiredTitle("PhD");
        dissertationInfo.setDefenceDate(LocalDate.now());
        dissertationInfo.setDiplomaNumber("123");
        dissertationInfo.setDiplomaIssueDate(LocalDate.now());
        dissertationInfo.setDiplomaSupplementsNumber("456");
        dissertationInfo.setDiplomaSupplementsIssueDate(LocalDate.now());
        dto.setDissertationInformation(dissertationInfo);

        var personalInfo = new RegistryBookPersonalInformationDTO();
        var name = new PersonNameDTO(null, "John", "H", "Doe", null, null);
        personalInfo.setAuthorName(name);
        personalInfo.setLocalBirthDate(LocalDate.of(1990, 1, 1));
        personalInfo.setPlaceOfBrith("City");
        personalInfo.setMunicipalityOfBrith("Municipality");
        personalInfo.setCountryOfBirthId(3);
        personalInfo.setFatherName("Father");
        personalInfo.setFatherSurname("Doe Sr.");
        personalInfo.setMotherName("Mother");
        personalInfo.setMotherSurname("Smith");
        personalInfo.setGuardianNameAndSurname("Uncle Joe");
        dto.setPersonalInformation(personalInfo);

        var contactInfo = new RegistryBookContactInformationDTO();
        contactInfo.setResidenceCountryId(4);
        contactInfo.setStreetAndNumber("Main 1");
        contactInfo.setPlace("Place");
        contactInfo.setMunicipality("Municipality");
        contactInfo.setPostalCode("10000");
        contactInfo.setContact(new ContactDTO("john@example.com", "+123456"));
        dto.setContactInformation(contactInfo);

        var prevTitle = new PreviousTitleInformationDTO();
        prevTitle.setInstitutionName("University");
        prevTitle.setGraduationDate(LocalDate.of(2015, 6, 15));
        prevTitle.setInstitutionPlace("Town");
        prevTitle.setSchoolYear("2014/2015");
        dto.setPreviousTitleInformation(prevTitle);

        return dto;
    }

    @Test
    @WithMockUser(username = "test.admin@test.com", password = "testAdmin")
    public void testGetRegistryBookEntriesForPromotion() throws Exception {
        String jwtToken = authenticateAdminAndGetToken();
        var request = getTestPayload();

        String requestBody = objectMapper.writeValueAsString(request);
        mockMvc.perform(
                MockMvcRequestBuilders.get(
                        "http://localhost:8081/api/registry-book/for-promotion/{promotionId}?page=0&size=10",
                        1)
                    .content(requestBody).contentType(MediaType.APPLICATION_JSON)
                    .header(HttpHeaders.AUTHORIZATION, "Bearer " + jwtToken))
            .andExpect(status().isOk());
    }

    @Test
    @WithMockUser(username = "test.admin@test.com", password = "testAdmin")
    public void testGetCanAdd() throws Exception {
        String jwtToken = authenticateAdminAndGetToken();
        var request = getTestPayload();

        String requestBody = objectMapper.writeValueAsString(request);
        mockMvc.perform(
                MockMvcRequestBuilders.get(
                        "http://localhost:8081/api/registry-book/can-add/{thesisId}", 10)
                    .content(requestBody).contentType(MediaType.APPLICATION_JSON)
                    .header(HttpHeaders.AUTHORIZATION, "Bearer " + jwtToken))
            .andExpect(status().isOk());
    }

    @Test
    @WithMockUser(username = "test.admin@test.com", password = "testAdmin")
    public void testGetNonPromotedRegistryBookEntries() throws Exception {
        String jwtToken = authenticateAdminAndGetToken();
        var request = getTestPayload();

        String requestBody = objectMapper.writeValueAsString(request);
        mockMvc.perform(
                MockMvcRequestBuilders.get(
                        "http://localhost:8081/api/registry-book/non-promoted?page=0&size=10")
                    .content(requestBody).contentType(MediaType.APPLICATION_JSON)
                    .header(HttpHeaders.AUTHORIZATION, "Bearer " + jwtToken))
            .andExpect(status().isOk());
    }

    @Test
    @Order(1)
    @WithMockUser(username = "test.admin@test.com", password = "testAdmin")
    public void testCreateRegistryBookEntry() throws Exception {
        String jwtToken = authenticateAdminAndGetToken();
        var request = getTestPayload();

        String requestBody = objectMapper.writeValueAsString(request);
        mockMvc.perform(
                MockMvcRequestBuilders.post("http://localhost:8081/api/registry-book/{thesisId}", 10)
                    .content(requestBody).contentType(MediaType.APPLICATION_JSON)
                    .header(HttpHeaders.AUTHORIZATION, "Bearer " + jwtToken)
                    .header("Idempotency-Key", "MOCK_KEY_REGISTRY_BOOK_ENTRY"))
            .andExpect(status().isCreated());
    }

    @Test
    @Order(2)
    @WithMockUser(username = "test.admin@test.com", password = "testAdmin")
    public void testUpdateRegistryBookEntry() throws Exception {
        String jwtToken = authenticateAdminAndGetToken();
        var request = getTestPayload();

        String requestBody = objectMapper.writeValueAsString(request);
        mockMvc.perform(
                MockMvcRequestBuilders.put(
                        "http://localhost:8081/api/registry-book/{registryBookEntryId}", 1)
                    .content(requestBody).contentType(MediaType.APPLICATION_JSON)
                    .header(HttpHeaders.AUTHORIZATION, "Bearer " + jwtToken))
            .andExpect(status().isNoContent());
    }

    @Test
    @Order(5)
    @WithMockUser(username = "test.admin@test.com", password = "testAdmin")
    public void testDeleteRegistryBookEntry() throws Exception {
        String jwtToken = authenticateAdminAndGetToken();

        mockMvc.perform(
                MockMvcRequestBuilders.delete(
                        "http://localhost:8081/api/registry-book/{registryBookEntryId}", 1)
                    .contentType(MediaType.APPLICATION_JSON)
                    .header(HttpHeaders.AUTHORIZATION, "Bearer " + jwtToken))
            .andExpect(status().isNoContent());
    }

    @Test
    @WithMockUser(username = "test.admin@test.com", password = "testAdmin")
    public void testPrePopulatedDataForEntry() throws Exception {
        String jwtToken = authenticateAdminAndGetToken();

        mockMvc.perform(
                MockMvcRequestBuilders.get(
                        "http://localhost:8081/api/registry-book/pre-populate/{thesisId}", 10)
                    .contentType(MediaType.APPLICATION_JSON)
                    .header(HttpHeaders.AUTHORIZATION, "Bearer " + jwtToken))
            .andExpect(status().isOk());
    }

    @Test
    @Order(3)
    @WithMockUser(username = "test.admin@test.com", password = "testAdmin")
    public void testAddToPromotion() throws Exception {
        String jwtToken = authenticateAdminAndGetToken();

        mockMvc.perform(
                MockMvcRequestBuilders.patch(
                        "http://localhost:8081/api/registry-book/add/{registryBookEntryId}/{promotionId}",
                        1, 1)
                    .contentType(MediaType.APPLICATION_JSON)
                    .header(HttpHeaders.AUTHORIZATION, "Bearer " + jwtToken))
            .andExpect(status().isNoContent());
    }

    @Test
    @Order(4)
    @WithMockUser(username = "test.admin@test.com", password = "testAdmin")
    public void testRemoveFromPromotion() throws Exception {
        String jwtToken = authenticateAdminAndGetToken();

        mockMvc.perform(
                MockMvcRequestBuilders.patch(
                        "http://localhost:8081/api/registry-book/remove/{registryBookEntryId}", 1)
                    .contentType(MediaType.APPLICATION_JSON)
                    .header(HttpHeaders.AUTHORIZATION, "Bearer " + jwtToken))
            .andExpect(status().isNoContent());
    }

    @Test
    @Order(5)
    @WithMockUser(username = "test.admin@test.com", password = "testAdmin")
    public void testPromoteAll() throws Exception {
        String jwtToken = authenticateAdminAndGetToken();

        mockMvc.perform(
                MockMvcRequestBuilders.patch(
                        "http://localhost:8081/api/registry-book/promote-all/{promotionId}", 1)
                    .contentType(MediaType.APPLICATION_JSON)
                    .header(HttpHeaders.AUTHORIZATION, "Bearer " + jwtToken))
            .andExpect(status().isNoContent());
    }
}
