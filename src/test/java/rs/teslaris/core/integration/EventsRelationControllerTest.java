package rs.teslaris.core.integration;

import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.fasterxml.jackson.databind.ObjectMapper;
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
import rs.teslaris.core.dto.document.EventsRelationDTO;
import rs.teslaris.core.model.document.EventsRelationType;

@SpringBootTest
@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
public class EventsRelationControllerTest extends BaseTest {

    @Autowired
    private ObjectMapper objectMapper;

    private EventsRelationDTO getTestPayload() {
        var eventsRelationDTO = new EventsRelationDTO();

        eventsRelationDTO.setEventsRelationType(EventsRelationType.COLLOCATED_WITH);
        eventsRelationDTO.setSourceId(2);
        eventsRelationDTO.setTargetId(1);

        return eventsRelationDTO;
    }

    @Test
    @Order(1)
    @WithMockUser(username = "test.admin@test.com", password = "testAdmin")
    public void testAddNewRelation() throws Exception {
        String jwtToken = authenticateAdminAndGetToken();

        var relationDTO = getTestPayload();

        String requestBody = objectMapper.writeValueAsString(relationDTO);
        mockMvc.perform(
                MockMvcRequestBuilders.patch("http://localhost:8081/api/events-relation")
                    .content(requestBody)
                    .contentType(MediaType.APPLICATION_JSON)
                    .header(HttpHeaders.AUTHORIZATION, "Bearer " + jwtToken)
                    .header("Idempotency-Key", "MOCK_KEY_EVENTS_RELATION"))
            .andExpect(status().isNoContent());
    }

    @Test
    @Order(2)
    @WithMockUser(username = "test.admin@test.com", password = "testAdmin")
    public void testDeleteRelation() throws Exception {
        String jwtToken = authenticateAdminAndGetToken();

        mockMvc.perform(
                MockMvcRequestBuilders.delete("http://localhost:8081/api/events-relation/{relationId}",
                        1)
                    .contentType(MediaType.APPLICATION_JSON)
                    .header(HttpHeaders.AUTHORIZATION, "Bearer " + jwtToken))
            .andExpect(status().isNoContent());
    }

    @Test
    @WithMockUser(username = "test.admin@test.com", password = "testAdmin")
    public void testReadEventsRelationsForOneTimeEvent() throws Exception {
        mockMvc.perform(
            MockMvcRequestBuilders.get("http://localhost:8081/api/events-relation/{eventId}", 1)
                .contentType(MediaType.APPLICATION_JSON)).andExpect(status().isOk());
    }

    @Test
    @WithMockUser(username = "test.admin@test.com", password = "testAdmin")
    public void testReadEventsRelationsForSerialEvent() throws Exception {
        mockMvc.perform(
            MockMvcRequestBuilders.get(
                    "http://localhost:8081/api/events-relation/serial-event/{serialEventId}", 3)
                .contentType(MediaType.APPLICATION_JSON)).andExpect(status().isOk());
    }
}
