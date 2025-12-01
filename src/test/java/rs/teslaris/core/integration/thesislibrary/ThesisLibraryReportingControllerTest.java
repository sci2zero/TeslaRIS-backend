package rs.teslaris.core.integration.thesislibrary;

import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.fasterxml.jackson.databind.ObjectMapper;
import java.time.LocalDate;
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
import rs.teslaris.thesislibrary.dto.NotAddedToPromotionThesesRequestDTO;
import rs.teslaris.thesislibrary.dto.ThesisReportRequestDTO;

@SpringBootTest
public class ThesisLibraryReportingControllerTest extends BaseTest {

    @Autowired
    private ObjectMapper objectMapper;


    private ThesisReportRequestDTO getTestPayload() {
        return new ThesisReportRequestDTO(LocalDate.of(2024, 1, 1), LocalDate.of(2024, 12, 31),
            List.of(1), ThesisType.PHD_ART_PROJECT);
    }

    @Test
    @WithMockUser(username = "test.admin@test.com", password = "testAdmin")
    public void testGetCountsReport() throws Exception {
        String jwtToken = authenticateAdminAndGetToken();

        var request = getTestPayload();

        String requestBody = objectMapper.writeValueAsString(request);
        mockMvc.perform(
                MockMvcRequestBuilders.post(
                        "http://localhost:8081/api/thesis-library/report/counts")
                    .content(requestBody)
                    .contentType(MediaType.APPLICATION_JSON)
                    .header(HttpHeaders.AUTHORIZATION, "Bearer " + jwtToken))
            .andExpect(status().isOk());
    }

    @Test
    @WithMockUser(username = "test.admin@test.com", password = "testAdmin")
    public void testGetDefendedPublications() throws Exception {
        String jwtToken = authenticateAdminAndGetToken();

        var request = getTestPayload();

        String requestBody = objectMapper.writeValueAsString(request);
        mockMvc.perform(
                MockMvcRequestBuilders.post(
                        "http://localhost:8081/api/thesis-library/report/defended")
                    .content(requestBody)
                    .contentType(MediaType.APPLICATION_JSON)
                    .header(HttpHeaders.AUTHORIZATION, "Bearer " + jwtToken))
            .andExpect(status().isOk());
    }

    @Test
    @WithMockUser(username = "test.admin@test.com", password = "testAdmin")
    public void testGetAcceptedPublications() throws Exception {
        String jwtToken = authenticateAdminAndGetToken();

        var request = getTestPayload();

        String requestBody = objectMapper.writeValueAsString(request);
        mockMvc.perform(
                MockMvcRequestBuilders.post(
                        "http://localhost:8081/api/thesis-library/report/accepted")
                    .content(requestBody)
                    .contentType(MediaType.APPLICATION_JSON)
                    .header(HttpHeaders.AUTHORIZATION, "Bearer " + jwtToken))
            .andExpect(status().isOk());
    }

    @Test
    @WithMockUser(username = "test.admin@test.com", password = "testAdmin")
    public void testGetPublicReviewPublications() throws Exception {
        String jwtToken = authenticateAdminAndGetToken();

        var request = getTestPayload();

        String requestBody = objectMapper.writeValueAsString(request);
        mockMvc.perform(
                MockMvcRequestBuilders.post(
                        "http://localhost:8081/api/thesis-library/report/public-review")
                    .content(requestBody)
                    .contentType(MediaType.APPLICATION_JSON)
                    .header(HttpHeaders.AUTHORIZATION, "Bearer " + jwtToken))
            .andExpect(status().isOk());
    }

    @Test
    @WithMockUser(username = "test.admin@test.com", password = "testAdmin")
    public void testGetPubliclyAvailablePublications() throws Exception {
        String jwtToken = authenticateAdminAndGetToken();

        var request = getTestPayload();

        String requestBody = objectMapper.writeValueAsString(request);
        mockMvc.perform(
                MockMvcRequestBuilders.post(
                        "http://localhost:8081/api/thesis-library/report/public-access")
                    .content(requestBody)
                    .contentType(MediaType.APPLICATION_JSON)
                    .header(HttpHeaders.AUTHORIZATION, "Bearer " + jwtToken))
            .andExpect(status().isOk());
    }

    @ParameterizedTest
    @ValueSource(strings = {"sr", "en"})
    @WithMockUser(username = "test.admin@test.com", password = "testAdmin")
    public void testDownloadReportDocument(String language) throws Exception {
        var authResponse = authenticateAdminAndGetTokenWithFingerprintCookie();

        var request = getTestPayload();

        String requestBody = objectMapper.writeValueAsString(request);
        mockMvc.perform(
                MockMvcRequestBuilders.post(
                        "http://localhost:8081/api/thesis-library/report/download/{lang}", language)
                    .content(requestBody)
                    .contentType(MediaType.APPLICATION_JSON)
                    .header(HttpHeaders.AUTHORIZATION, "Bearer " + authResponse.a)
                    .cookie(authResponse.b))
            .andExpect(status().isOk());
    }

    @Test
    @WithMockUser(username = "test.admin@test.com", password = "testAdmin")
    public void testGetThesesNotAddedToPromotion() throws Exception {
        String jwtToken = authenticateAdminAndGetToken();

        var request = new NotAddedToPromotionThesesRequestDTO(
            LocalDate.of(2024, 1, 1),
            LocalDate.of(2024, 12, 31),
            List.of(1),
            List.of(ThesisType.PHD, ThesisType.PHD_ART_PROJECT)
        );

        String requestBody = objectMapper.writeValueAsString(request);
        mockMvc.perform(
                MockMvcRequestBuilders.post(
                        "http://localhost:8081/api/thesis-library/report/not-added-to-registry-book")
                    .content(requestBody)
                    .contentType(MediaType.APPLICATION_JSON)
                    .header(HttpHeaders.AUTHORIZATION, "Bearer " + jwtToken))
            .andExpect(status().isOk());
    }
}
