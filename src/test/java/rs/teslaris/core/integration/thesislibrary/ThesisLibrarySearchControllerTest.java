package rs.teslaris.core.integration.thesislibrary;

import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.fasterxml.jackson.databind.ObjectMapper;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders;
import rs.teslaris.core.integration.BaseTest;
import rs.teslaris.core.model.document.ThesisType;
import rs.teslaris.thesislibrary.dto.ThesisSearchRequestDTO;

@SpringBootTest
public class ThesisLibrarySearchControllerTest extends BaseTest {

    @Autowired
    private ObjectMapper objectMapper;


    private ThesisSearchRequestDTO getTestPayload(boolean advanced) {
        return new ThesisSearchRequestDTO(
            List.of(advanced ? "title_sr:test" : "test"), List.of(1), List.of(2), List.of(3),
            List.of(4), List.of(5), List.of(ThesisType.MASTER), false,
            null, null
        );
    }

    @Test
    @WithMockUser(username = "test.admin@test.com", password = "testAdmin")
    public void testPerformSimpleSearch() throws Exception {
        String jwtToken = authenticateLibrarianAndGetToken();

        var request = getTestPayload(false);

        String requestBody = objectMapper.writeValueAsString(request);
        mockMvc.perform(
                MockMvcRequestBuilders.post(
                        "http://localhost:8081/api/thesis-library/search/simple")
                    .content(requestBody)
                    .contentType(MediaType.APPLICATION_JSON)
                    .header(HttpHeaders.AUTHORIZATION, "Bearer " + jwtToken))
            .andExpect(status().isOk());
    }

    @Test
    @WithMockUser(username = "test.admin@test.com", password = "testAdmin")
    public void testPerformAdvancedSearch() throws Exception {
        String jwtToken = authenticateLibrarianAndGetToken();

        var request = getTestPayload(true);

        String requestBody = objectMapper.writeValueAsString(request);
        mockMvc.perform(
                MockMvcRequestBuilders.post(
                        "http://localhost:8081/api/thesis-library/search/advanced")
                    .content(requestBody)
                    .contentType(MediaType.APPLICATION_JSON)
                    .header(HttpHeaders.AUTHORIZATION, "Bearer " + jwtToken))
            .andExpect(status().isOk());
    }

    @ParameterizedTest
    @ValueSource(booleans = {true, false})
    @WithMockUser(username = "test.researcher@test.com", password = "testResearcher")
    public void testGetSearchFields(Boolean onlyExportFields) throws Exception {
        String jwtToken = authenticateResearcherAndGetToken();

        mockMvc.perform(
                MockMvcRequestBuilders.get(
                        "http://localhost:8081/api/thesis-library/search/fields?export={export}", onlyExportFields)
                    .contentType(MediaType.APPLICATION_JSON)
                    .header(HttpHeaders.AUTHORIZATION, "Bearer " + jwtToken))
            .andExpect(status().isOk());
    }
}
