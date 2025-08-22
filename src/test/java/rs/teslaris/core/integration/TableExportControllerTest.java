package rs.teslaris.core.integration;

import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.fasterxml.jackson.databind.ObjectMapper;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders;
import rs.teslaris.core.dto.commontypes.ExportFileType;
import rs.teslaris.core.dto.commontypes.TableExportRequestDTO;

@SpringBootTest
public class TableExportControllerTest extends BaseTest {

    @Autowired
    private ObjectMapper objectMapper;


    @Test
    public void testExportCSVDocuments() throws Exception {
        var request =
            new TableExportRequestDTO(List.of("title_sr", "year"), List.of(), true, 0, "sr",
                ExportFileType.CSV, null, List.of());

        String requestBody = objectMapper.writeValueAsString(request);
        mockMvc.perform(
                MockMvcRequestBuilders.post("http://localhost:8081/api/table-export/documents")
                    .content(requestBody).contentType(MediaType.APPLICATION_JSON))
            .andExpect(status().isOk());
    }

    @Test
    public void testExportCSVPersons() throws Exception {
        var request = new TableExportRequestDTO(List.of("name", "orcid"), List.of(), true, 0, "sr",
            ExportFileType.CSV, null, List.of());

        String requestBody = objectMapper.writeValueAsString(request);
        mockMvc.perform(
                MockMvcRequestBuilders.post("http://localhost:8081/api/table-export/persons")
                    .content(requestBody).contentType(MediaType.APPLICATION_JSON))
            .andExpect(status().isOk());
    }

    @Test
    public void testExportCSVOrganisationUnits() throws Exception {
        var request = new TableExportRequestDTO(List.of("name_sr"), List.of(), true, 0, "sr",
            ExportFileType.CSV, null, List.of());

        String requestBody = objectMapper.writeValueAsString(request);
        mockMvc.perform(
                MockMvcRequestBuilders.post("http://localhost:8081/api/table-export/organisation-units")
                    .content(requestBody).contentType(MediaType.APPLICATION_JSON))
            .andExpect(status().isOk());
    }
}
