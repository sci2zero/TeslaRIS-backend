package rs.teslaris.core.integration;

import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders;
import rs.teslaris.core.dto.commontypes.ContactFormContentDTO;

@SpringBootTest
public class FeedbackControllerTest extends BaseTest {

    @Autowired
    private ObjectMapper objectMapper;


    @Test
    public void testSubmitFeedback() throws Exception {
        String requestBody = objectMapper.writeValueAsString(
            new ContactFormContentDTO("John Doe", "email@email.com", "Subject", "Content",
                "everythingPasses"));
        mockMvc.perform(
                MockMvcRequestBuilders.post("http://localhost:8081/api/feedback")
                    .content(requestBody)
                    .contentType(MediaType.APPLICATION_JSON)
                    .header("Idempotency-Key", "MOCK_KEY_FEEDBACK"))
            .andExpect(status().isOk());
    }
}
