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
import rs.teslaris.core.dto.document.PublisherDTO;

@SpringBootTest
public class PublisherControllerTest extends BaseTest {

    @Autowired
    private ObjectMapper objectMapper;

    private PublisherDTO getTestPayload() {
        var publisherDTO = new PublisherDTO();
        publisherDTO.setName(List.of(new MultilingualContentDTO(25, "Name", 1)));
        publisherDTO.setPlace(List.of(new MultilingualContentDTO(25, "Place", 1)));
        publisherDTO.setState(List.of(new MultilingualContentDTO(25, "State", 1)));
        return publisherDTO;
    }

    @Test
    @WithMockUser(username = "admin@admin.com", password = "admin")
    public void testReadAllPublishers() throws Exception {
        mockMvc.perform(
                MockMvcRequestBuilders.get("http://localhost:8081/api/publisher")
                    .contentType(MediaType.APPLICATION_JSON))
            .andExpect(status().isOk());
    }

    @Test
    @WithMockUser(username = "admin@admin.com", password = "admin")
    public void testCreatePublisher() throws Exception {
        String jwtToken = authenticateAndGetToken();
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
    @WithMockUser(username = "admin@admin.com", password = "admin")
    public void testUpdatePublisher() throws Exception {
        String jwtToken = authenticateAndGetToken();
        var publisherDTO = getTestPayload();

        String requestBody = objectMapper.writeValueAsString(publisherDTO);
        mockMvc.perform(
                MockMvcRequestBuilders.put("http://localhost:8081/api/publisher/{publisherId}", 42)
                    .content(requestBody).contentType(MediaType.APPLICATION_JSON)
                    .header(HttpHeaders.AUTHORIZATION, "Bearer " + jwtToken))
            .andExpect(status().isNoContent());
    }

    @Test
    @WithMockUser(username = "admin@admin.com", password = "admin")
    public void testDeletePublisher() throws Exception {
        String jwtToken = authenticateAndGetToken();

        mockMvc.perform(
                MockMvcRequestBuilders.delete("http://localhost:8081/api/publisher/{publisherId}", 42)
                    .contentType(MediaType.APPLICATION_JSON)
                    .header(HttpHeaders.AUTHORIZATION, "Bearer " + jwtToken))
            .andExpect(status().isNoContent());
    }
}
