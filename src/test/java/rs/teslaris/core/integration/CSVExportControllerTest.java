package rs.teslaris.core.integration;

import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.fasterxml.jackson.databind.ObjectMapper;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders;
import rs.teslaris.core.dto.commontypes.CSVExportRequest;

@SpringBootTest
public class CSVExportControllerTest extends BaseTest {

    @Autowired
    private ObjectMapper objectMapper;

    private CSVExportRequest getTestPayload() {
        return new CSVExportRequest(List.of("title_sr", "year"), List.of(), true, 0, "sr");
    }

    @Test
    public void testExportCSVDocuments() throws Exception {
        var request = getTestPayload();

        String requestBody = objectMapper.writeValueAsString(request);
        mockMvc.perform(
                MockMvcRequestBuilders.post("http://localhost:8081/api/csv-export/documents")
                    .content(requestBody).contentType(MediaType.APPLICATION_JSON))
            .andExpect(status().isOk());
    }
}
