package rs.teslaris.core.integration;

import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders;

@SpringBootTest
public class PublicConfigurationControllerTest extends BaseTest {

    @Test
    public void testReadPublicConfiguration() throws Exception {
        mockMvc.perform(
                MockMvcRequestBuilders.get("http://localhost:8081/api/public-configuration")
                    .contentType(MediaType.APPLICATION_JSON))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.schemaVersion").value(1))
            .andExpect(jsonPath("$.updatedAt").exists())
            .andExpect(jsonPath("$.branding.title").exists())
            .andExpect(jsonPath("$.branding.description").exists());
    }
}
