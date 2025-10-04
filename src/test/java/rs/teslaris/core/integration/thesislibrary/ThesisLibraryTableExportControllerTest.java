package rs.teslaris.core.integration.thesislibrary;

import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.fasterxml.jackson.databind.ObjectMapper;
import java.util.ArrayList;
import java.util.List;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders;
import rs.teslaris.core.dto.commontypes.ExportFileType;
import rs.teslaris.core.dto.commontypes.TableExportRequestDTO;
import rs.teslaris.core.integration.BaseTest;
import rs.teslaris.core.model.commontypes.ExportableEndpointType;
import rs.teslaris.thesislibrary.dto.ThesisSearchRequestDTO;
import rs.teslaris.thesislibrary.dto.ThesisTableExportRequestDTO;

@SpringBootTest
public class ThesisLibraryTableExportControllerTest extends BaseTest {

    @Autowired
    private ObjectMapper objectMapper;

    private TableExportRequestDTO getTestPayload(boolean bibliographicFormat,
                                                 ExportFileType exportFileType) {
        var request = new ThesisTableExportRequestDTO();
        if (!bibliographicFormat) {
            request.setColumns(List.of("title_sr", "author_names"));
        }

        request.setExportLanguage("en");
        request.setExportMaxPossibleAmount(true);
        request.setBulkExportOffset(0);
        request.setExportFileType(exportFileType);

        request.setThesisSearchRequest(
            new ThesisSearchRequestDTO(List.of(), List.of(), List.of(), List.of(), List.of(),
                List.of(), List.of(), false, null, null));
        request.setEndpointType(ExportableEndpointType.THESIS_SIMPLE_SEARCH);

        return request;
    }

    @ParameterizedTest
    @ValueSource(booleans = {true, false})
    @WithMockUser(username = "test.admin@test.com", password = "testAdmin")
    public void testExportCSVTheses(boolean bibliographicFormat) throws Exception {
        String jwtToken = authenticateAdminAndGetToken();
        var types = new ArrayList<ExportFileType>();
        if (bibliographicFormat) {
            types.addAll(List.of(ExportFileType.BIB, ExportFileType.RIS, ExportFileType.ENW));
        } else {
            types.addAll(List.of(ExportFileType.CSV, ExportFileType.XLSX));
        }

        for (var exportFileType : types) {
            var request = getTestPayload(bibliographicFormat, exportFileType);

            String requestBody = objectMapper.writeValueAsString(request);
            mockMvc.perform(
                    MockMvcRequestBuilders.post("http://localhost:8081/api/thesis-library/table-export")
                        .content(requestBody).contentType(MediaType.APPLICATION_JSON)
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + jwtToken))
                .andExpect(status().isOk());
        }
    }
}
