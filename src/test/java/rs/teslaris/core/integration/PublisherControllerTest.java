package rs.teslaris.core.integration;

import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
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
import rs.teslaris.core.dto.document.PublisherBasicAdditionDTO;
import rs.teslaris.core.dto.document.PublisherDTO;

@SpringBootTest
public class PublisherControllerTest extends BaseTest {

    @Autowired
    private ObjectMapper objectMapper;

    private PublisherDTO getTestPayload() {
        var publisherDTO = new PublisherDTO();
        publisherDTO.setName(List.of(new MultilingualContentDTO(1, "EN", "Name", 1)));
        publisherDTO.setPlace(List.of(new MultilingualContentDTO(1, "EN", "Place", 1)));
        publisherDTO.setCountryId(1);
        return publisherDTO;
    }

    @Test
    @WithMockUser(username = "test.admin@test.com", password = "testAdmin")
    public void testReadAllPublishers() throws Exception {
        mockMvc.perform(
                MockMvcRequestBuilders.get("http://localhost:8081/api/publisher")
                    .contentType(MediaType.APPLICATION_JSON))
            .andExpect(status().isOk());
    }

    @Test
    @WithMockUser(username = "test.admin@test.com", password = "testAdmin")
    public void testReadPublisher() throws Exception {
        mockMvc.perform(
                MockMvcRequestBuilders.get("http://localhost:8081/api/publisher/{publisherId}", 1)
                    .contentType(MediaType.APPLICATION_JSON))
            .andExpect(status().isOk());
    }

    @Test
    @WithMockUser(username = "test.admin@test.com", password = "testAdmin")
    public void testSearchPublishers() throws Exception {
        String jwtToken = authenticateAdminAndGetToken();

        mockMvc.perform(
                MockMvcRequestBuilders.get(
                        "http://localhost:8081/api/publisher/simple-search?tokens=name&tokens=place")
                    .contentType(MediaType.APPLICATION_JSON)
                    .header(HttpHeaders.AUTHORIZATION, "Bearer " + jwtToken))
            .andExpect(status().isOk());
    }

    @Test
    @WithMockUser(username = "test.admin@test.com", password = "testAdmin")
    public void testCreatePublisher() throws Exception {
        String jwtToken = authenticateAdminAndGetToken();

        var publisherDTO = getTestPayload();

        String requestBody = objectMapper.writeValueAsString(publisherDTO);
        mockMvc.perform(
                MockMvcRequestBuilders.post("http://localhost:8081/api/publisher").content(requestBody)
                    .contentType(MediaType.APPLICATION_JSON)
                    .header(HttpHeaders.AUTHORIZATION, "Bearer " + jwtToken)
                    .header("Idempotency-Key", "MOCK_KEY_PUBLISHER")).andExpect(status().isCreated())
            .andExpect(jsonPath("$.id").isNotEmpty());
    }

    @Test
    @WithMockUser(username = "test.admin@test.com", password = "testAdmin")
    public void testCreatePublisherBasic() throws Exception {
        String jwtToken = authenticateAdminAndGetToken();

        var publisherDTO = new PublisherBasicAdditionDTO();
        publisherDTO.setName(List.of(new MultilingualContentDTO(1, "EN", "Name", 1)));
        publisherDTO.setCountryId(null);

        String requestBody = objectMapper.writeValueAsString(publisherDTO);
        mockMvc.perform(
                MockMvcRequestBuilders.post("http://localhost:8081/api/publisher/basic")
                    .content(requestBody)
                    .contentType(MediaType.APPLICATION_JSON)
                    .header(HttpHeaders.AUTHORIZATION, "Bearer " + jwtToken)
                    .header("Idempotency-Key", "MOCK_KEY_PUBLISHER_BASIC"))
            .andExpect(status().isCreated())
            .andExpect(jsonPath("$.id").isNotEmpty());
    }

    @Test
    @WithMockUser(username = "test.admin@test.com", password = "testAdmin")
    public void testUpdatePublisher() throws Exception {
        String jwtToken = authenticateAdminAndGetToken();
        var publisherDTO = getTestPayload();

        String requestBody = objectMapper.writeValueAsString(publisherDTO);
        mockMvc.perform(
                MockMvcRequestBuilders.put("http://localhost:8081/api/publisher/{publisherId}", 1)
                    .content(requestBody).contentType(MediaType.APPLICATION_JSON)
                    .header(HttpHeaders.AUTHORIZATION, "Bearer " + jwtToken))
            .andExpect(status().isNoContent());
    }

    @Test
    @WithMockUser(username = "test.admin@test.com", password = "testAdmin")
    public void testDeletePublisher() throws Exception {
        String jwtToken = authenticateAdminAndGetToken();

        mockMvc.perform(
                MockMvcRequestBuilders.delete("http://localhost:8081/api/publisher/{publisherId}", 1)
                    .contentType(MediaType.APPLICATION_JSON)
                    .header(HttpHeaders.AUTHORIZATION, "Bearer " + jwtToken))
            .andExpect(status().isNoContent());
    }
}
