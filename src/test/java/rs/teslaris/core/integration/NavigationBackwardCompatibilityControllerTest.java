package rs.teslaris.core.integration;

import static org.hamcrest.Matchers.equalTo;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders;

@SpringBootTest
public class NavigationBackwardCompatibilityControllerTest extends BaseTest {

    @Test
    public void testGetBackwardCompatibleId() throws Exception {
        mockMvc.perform(MockMvcRequestBuilders.get(
                    "http://localhost:8081/api/legacy-navigation/entity-landing-page/{oldId}?source=testSource&language=testLanguage",
                    3)
                .contentType(MediaType.APPLICATION_JSON))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.entityType", equalTo("PERSON")));
    }

    @Test
    public void testGetBackwardCompatibleIdNotFound() throws Exception {
        mockMvc.perform(MockMvcRequestBuilders.get(
                    "http://localhost:8081/api/legacy-navigation/entity-landing-page/{oldId}?source=testSource&language=testLanguage",
                    999)
                .contentType(MediaType.APPLICATION_JSON))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.entityType", equalTo("NOT_FOUND")));
    }

    @Test
    public void testGetBackwardCompatibleFilename() throws Exception {
        mockMvc.perform(MockMvcRequestBuilders.get(
                    "http://localhost:8081/api/legacy-navigation/document-file/{oldServerFilename}?source=testSource&language=testLanguage",
                    "123.pdf")
                .contentType(MediaType.APPLICATION_JSON))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.serverFilename", equalTo("3333.pdf")));
    }

    @Test
    public void testGetBackwardCompatibleFilenameNotFound() throws Exception {
        mockMvc.perform(MockMvcRequestBuilders.get(
                    "http://localhost:8081/api/legacy-navigation/document-file/{oldServerFilename}?source=testSource&language=testLanguage",
                    "non_existing.pdf")
                .contentType(MediaType.APPLICATION_JSON))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.serverFilename", equalTo("NOT_FOUND")));
    }
}
