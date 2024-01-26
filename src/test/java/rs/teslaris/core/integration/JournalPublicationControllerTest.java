package rs.teslaris.core.integration;

import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders;

@SpringBootTest
public class JournalPublicationControllerTest extends BaseTest {

    @Autowired
    private ObjectMapper objectMapper;

    @Test
    @WithMockUser(username = "admin@admin.com", password = "admin")
    public void testFindMyPublicationsInJournal() throws Exception {
        String jwtToken = authenticateAdminAndGetToken();

        mockMvc.perform(MockMvcRequestBuilders.get(
                    "http://localhost:8081/api/journal-publication/journal/{journalId}/my-publications", 30)
                .contentType(MediaType.APPLICATION_JSON)
                .header(HttpHeaders.AUTHORIZATION, "Bearer " + jwtToken))
            .andExpect(status().isOk());
    }

    @Test
    @WithMockUser(username = "admin@admin.com", password = "admin")
    public void testFindPublicationsInJournal() throws Exception {
        String jwtToken = authenticateAdminAndGetToken();

        mockMvc.perform(MockMvcRequestBuilders.get(
                    "http://localhost:8081/api/journal-publication/journal/{journalId}/", 30)
                .contentType(MediaType.APPLICATION_JSON)
                .header(HttpHeaders.AUTHORIZATION, "Bearer " + jwtToken))
            .andExpect(status().isOk());
    }
}
