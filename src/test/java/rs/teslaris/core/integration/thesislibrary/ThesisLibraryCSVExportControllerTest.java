package rs.teslaris.core.integration.thesislibrary;

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
import rs.teslaris.core.dto.commontypes.CSVExportRequest;
import rs.teslaris.core.dto.commontypes.ExportFileType;
import rs.teslaris.core.integration.BaseTest;
import rs.teslaris.core.model.commontypes.ExportableEndpointType;
import rs.teslaris.thesislibrary.dto.ThesisCSVExportRequestDTO;
import rs.teslaris.thesislibrary.dto.ThesisSearchRequestDTO;

@SpringBootTest
public class ThesisLibraryCSVExportControllerTest extends BaseTest {

    @Autowired
    private ObjectMapper objectMapper;

    private CSVExportRequest getTestPayload() {
        var request = new ThesisCSVExportRequestDTO();
        request.setColumns(List.of("title_sr", "author_names"));
        request.setExportLanguage("en");
        request.setExportMaxPossibleAmount(true);
        request.setBulkExportOffset(0);
        request.setExportFileType(ExportFileType.CSV);
        request.setThesisSearchRequest(
            new ThesisSearchRequestDTO(List.of(), List.of(), List.of(), List.of(), List.of(),
                List.of(), List.of(), false, null, null));
        request.setEndpointType(ExportableEndpointType.THESIS_SIMPLE_SEARCH);

        return request;
    }

    @Test
    @WithMockUser(username = "test.admin@test.com", password = "testAdmin")
    public void testExportCSVTheses() throws Exception {
        String jwtToken = authenticateAdminAndGetToken();
        var request = getTestPayload();

        String requestBody = objectMapper.writeValueAsString(request);
        mockMvc.perform(
                MockMvcRequestBuilders.post("http://localhost:8081/api/thesis-library/csv-export")
                    .content(requestBody).contentType(MediaType.APPLICATION_JSON)
                    .header(HttpHeaders.AUTHORIZATION, "Bearer " + jwtToken))
            .andExpect(status().isOk());
    }
}
