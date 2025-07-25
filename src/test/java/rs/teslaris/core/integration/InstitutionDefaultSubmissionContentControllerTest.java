package rs.teslaris.core.integration;

import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.fasterxml.jackson.databind.ObjectMapper;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders;
import rs.teslaris.core.dto.commontypes.MultilingualContentDTO;
import rs.teslaris.core.dto.institution.InstitutionDefaultSubmissionContentDTO;

@SpringBootTest
public class InstitutionDefaultSubmissionContentControllerTest extends BaseTest {

    @Autowired
    private ObjectMapper objectMapper;


    @Test
    @WithMockUser(username = "test.admin@test.com", password = "testAdmin")
    public void testReadConfigurationForInstitution() throws Exception {
        String jwtToken = authenticateAdminAndGetToken();

        mockMvc.perform(
                MockMvcRequestBuilders.get(
                        "http://localhost:8081/api/institution-default-submission-content/institution/{organisationUnitId}",
                        1)
                    .contentType(MediaType.APPLICATION_JSON)
                    .header(HttpHeaders.AUTHORIZATION, "Bearer " + jwtToken))
            .andExpect(status().isOk());
    }

    @Test
    @WithMockUser(username = "test.researcher@test.com", password = "testResearcher")
    public void testReadConfigurationForUser() throws Exception {
        String jwtToken = authenticateResearcherAndGetToken();

        mockMvc.perform(
                MockMvcRequestBuilders.get(
                        "http://localhost:8081/api/institution-default-submission-content/for-user",
                        1)
                    .contentType(MediaType.APPLICATION_JSON)
                    .header(HttpHeaders.AUTHORIZATION, "Bearer " + jwtToken))
            .andExpect(status().isOk());
    }

    @Test
    @WithMockUser(username = "test.admin@test.com", password = "testAdmin")
    public void testSaveConfiguration() throws Exception {
        String jwtToken = authenticateAdminAndGetToken();

        var dummyMC = List.of(new MultilingualContentDTO(1, "EN", "Content", 1));
        var contentDTO = new InstitutionDefaultSubmissionContentDTO(dummyMC, dummyMC);
        String requestBody = objectMapper.writeValueAsString(contentDTO);
        mockMvc.perform(
                MockMvcRequestBuilders.patch(
                        "http://localhost:8081/api/institution-default-submission-content/{organisationUnitId}",
                        1)
                    .content(requestBody)
                    .contentType(MediaType.APPLICATION_JSON)
                    .header(HttpHeaders.AUTHORIZATION, "Bearer " + jwtToken))
            .andExpect(status().isNoContent());
    }
}
