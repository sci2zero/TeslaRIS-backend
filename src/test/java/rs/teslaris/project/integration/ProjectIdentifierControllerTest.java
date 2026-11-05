package rs.teslaris.project.integration;

import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders;
import rs.teslaris.core.dto.identifier.EntityIdentifierResponseDTO;
import rs.teslaris.core.integration.BaseTest;
import rs.teslaris.project.dto.identifier.ProjectIdentifierDTO;

@SpringBootTest
public class ProjectIdentifierControllerTest extends BaseTest {

    @Autowired
    private ObjectMapper objectMapper;


    private ProjectIdentifierDTO getTestPayload(String value) {
        return new ProjectIdentifierDTO(value, 1, 1);
    }

    @Test
    public void testReadProjectIdentifiersForProject() throws Exception {
        mockMvc.perform(
            MockMvcRequestBuilders.get(
                    "http://localhost:8081/api/project-identifier/{projectId}", 1)
                .contentType(MediaType.APPLICATION_JSON)).andExpect(status().isOk());
    }

    @Test
    @WithMockUser(username = "test.admin@test.com", password = "testAdmin")
    public void testCreateProjectIdentifierSuccess() throws Exception {
        String jwtToken = authenticateAdminAndGetToken();

        var projectIdentifierDTO = getTestPayload("11111");

        String requestBody = objectMapper.writeValueAsString(projectIdentifierDTO);
        mockMvc.perform(
                MockMvcRequestBuilders.post("http://localhost:8081/api/project-identifier/{projectId}",
                        projectIdentifierDTO.getProjectId())
                    .content(requestBody)
                    .contentType(MediaType.APPLICATION_JSON)
                    .header(HttpHeaders.AUTHORIZATION, "Bearer " + jwtToken)
                    .header("Idempotency-Key", "MOCK_KEY_PROJECT_IDENTIFIER_1"))
            .andExpect(status().isCreated());
    }

    @Test
    @WithMockUser(username = "test.admin@test.com", password = "testAdmin")
    public void testCreateProjectIdentifierWrongFormat() throws Exception {
        String jwtToken = authenticateAdminAndGetToken();

        var projectIdentifierDTO = getTestPayload("WRONG_FORMAT");

        String requestBody = objectMapper.writeValueAsString(projectIdentifierDTO);
        mockMvc.perform(
                MockMvcRequestBuilders.post("http://localhost:8081/api/project-identifier/{projectId}",
                        projectIdentifierDTO.getProjectId())
                    .content(requestBody)
                    .contentType(MediaType.APPLICATION_JSON)
                    .header(HttpHeaders.AUTHORIZATION, "Bearer " + jwtToken)
                    .header("Idempotency-Key", "MOCK_KEY_PROJECT_IDENTIFIER_2"))
            .andExpect(status().isConflict());
    }

    @Test
    @WithMockUser(username = "test.admin@test.com", password = "testAdmin")
    public void testUpdateProjectIdentifier() throws Exception {
        String jwtToken = authenticateAdminAndGetToken();

        var createdProjectIdentifierDTO = getTestPayload("11112");

        var createResponse = mockMvc.perform(
                MockMvcRequestBuilders.post("http://localhost:8081/api/project-identifier/{projectId}",
                        createdProjectIdentifierDTO.getProjectId())
                    .content(objectMapper.writeValueAsString(createdProjectIdentifierDTO))
                    .contentType(MediaType.APPLICATION_JSON)
                    .header(HttpHeaders.AUTHORIZATION, "Bearer " + jwtToken)
                    .header("Idempotency-Key", "MOCK_KEY_PROJECT_IDENTIFIER_3"))
            .andExpect(status().isCreated())
            .andReturn();

        var createdProjectIdentifier = objectMapper.readValue(
            createResponse.getResponse().getContentAsString(), EntityIdentifierResponseDTO.class);

        var projectIdentifierDTO = getTestPayload("11113");

        String requestBody = objectMapper.writeValueAsString(projectIdentifierDTO);
        mockMvc.perform(
                MockMvcRequestBuilders.put(
                        "http://localhost:8081/api/project-identifier/{projectId}/{identifierId}",
                        projectIdentifierDTO.getProjectId(), createdProjectIdentifier.id())
                    .content(requestBody).contentType(MediaType.APPLICATION_JSON)
                    .header(HttpHeaders.AUTHORIZATION, "Bearer " + jwtToken))
            .andExpect(status().isNoContent());
    }
}
