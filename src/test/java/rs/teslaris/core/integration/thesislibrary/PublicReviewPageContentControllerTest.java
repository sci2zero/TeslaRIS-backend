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
import rs.teslaris.core.dto.commontypes.MultilingualContentDTO;
import rs.teslaris.core.integration.BaseTest;
import rs.teslaris.core.model.document.ThesisType;
import rs.teslaris.thesislibrary.dto.PublicReviewPageContentDTO;
import rs.teslaris.thesislibrary.model.PageContentType;
import rs.teslaris.thesislibrary.model.PageType;

@SpringBootTest
public class PublicReviewPageContentControllerTest extends BaseTest {

    @Autowired
    private ObjectMapper objectMapper;


    private List<PublicReviewPageContentDTO> getTestPayload() {
        return List.of(
            new PublicReviewPageContentDTO(null, PageContentType.TEXT, PageType.ALL,
                List.of(ThesisType.PHD),
                List.of(new MultilingualContentDTO(1, "EN", "Content", 1))));
    }

    @Test
    @WithMockUser(username = "test.admin@test.com", password = "testAdmin")
    public void testGetAllForInstitution() throws Exception {
        String jwtToken = authenticateAdminAndGetToken();
        mockMvc.perform(
                MockMvcRequestBuilders.get(
                        "http://localhost:8081/api/public-review-page-content/for-institution/{organisationUnitId}",
                        1)
                    .contentType(MediaType.APPLICATION_JSON)
                    .header(HttpHeaders.AUTHORIZATION, "Bearer " + jwtToken))
            .andExpect(status().isOk());
    }

    @Test
    public void testGetAllForInstitutionAndType() throws Exception {
        mockMvc.perform(
                MockMvcRequestBuilders.get(
                        "http://localhost:8081/api/public-review-page-content/for-institution-and-type/{organisationUnitId}?thesisTypes={thesisType}",
                        1, "PHD")
                    .contentType(MediaType.APPLICATION_JSON))
            .andExpect(status().isOk());
    }

    @Test
    @WithMockUser(username = "test.admin@test.com", password = "testAdmin")
    public void testSaveConfiguration() throws Exception {
        String jwtToken = authenticateAdminAndGetToken();
        var request = getTestPayload();

        String requestBody = objectMapper.writeValueAsString(request);
        mockMvc.perform(
                MockMvcRequestBuilders.patch(
                        "http://localhost:8081/api/public-review-page-content/{organisationUnitId}", 1)
                    .content(requestBody).contentType(MediaType.APPLICATION_JSON)
                    .header(HttpHeaders.AUTHORIZATION, "Bearer " + jwtToken))
            .andExpect(status().isAccepted());
    }
}
