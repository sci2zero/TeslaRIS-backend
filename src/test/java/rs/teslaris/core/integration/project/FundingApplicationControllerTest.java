package rs.teslaris.core.integration.project;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders;
import rs.teslaris.core.dto.commontypes.MonetaryAmountDTO;
import rs.teslaris.core.dto.commontypes.MultilingualContentDTO;
import rs.teslaris.core.integration.BaseTest;
import rs.teslaris.core.util.language.LanguageAbbreviations;
import rs.teslaris.project.dto.funding.FundingApplicationDTO;
import rs.teslaris.project.dto.funding.FundingPartDTO;
import rs.teslaris.project.model.funding.FundingApplicationResult;

import java.time.LocalDate;
import java.util.ArrayList;

import static org.springframework.test.web.servlet.result.MockMvcResultHandlers.print;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
public class FundingApplicationControllerTest extends BaseTest {

    @Autowired
    private ObjectMapper objectMapper;

    public static FundingApplicationDTO getTestPayload() {
        var dto = new FundingApplicationDTO();

        dto.setFundingCallId(1);

        var descriptionList = new ArrayList<MultilingualContentDTO>();
        var englishDesc = new MultilingualContentDTO();
        englishDesc.setLanguageTagId(1);
        englishDesc.setLanguageTag(LanguageAbbreviations.ENGLISH);
        englishDesc.setContent("Test funding application description");
        englishDesc.setPriority(1);
        descriptionList.add(englishDesc);
        dto.setDescription(descriptionList);

        var responseSummaryList = new ArrayList<MultilingualContentDTO>();
        var englishSummary = new MultilingualContentDTO();
        englishSummary.setLanguageTagId(1);
        englishSummary.setLanguageTag(LanguageAbbreviations.ENGLISH);
        englishSummary.setContent("Test response summary");
        englishSummary.setPriority(1);
        responseSummaryList.add(englishSummary);
        dto.setResponseSummary(responseSummaryList);

        var requestedAmount = new MonetaryAmountDTO();
        requestedAmount.setAmount(50000);
        requestedAmount.setCurrencyId(1);
        dto.setRequestedAmount(requestedAmount);

        var fundingPartList = new ArrayList<FundingPartDTO>();
        var fundingPart = new FundingPartDTO();
        fundingPart.setFundingId(1);

        var partDescriptionList = new ArrayList<MultilingualContentDTO>();
        var partDesc = new MultilingualContentDTO();
        partDesc.setLanguageTagId(1);
        partDesc.setLanguageTag(LanguageAbbreviations.ENGLISH);
        partDesc.setContent("Additional funding source");
        partDesc.setPriority(1);
        partDescriptionList.add(partDesc);
        fundingPart.setDescription(partDescriptionList);

        var partAmount = new MonetaryAmountDTO();
        partAmount.setAmount(10000);
        partAmount.setCurrencyId(1);
        fundingPart.setAmount(partAmount);

        fundingPartList.add(fundingPart);
        dto.setOtherFundingSources(fundingPartList);

        dto.setSubmissionDate(LocalDate.of(2025, 6, 15));
        dto.setReviewDateFrom(LocalDate.of(2025, 7, 1));
        dto.setReviewDateTo(LocalDate.of(2025, 7, 31));
        dto.setDecisionDate(LocalDate.of(2025, 8, 15));
        dto.setResult(FundingApplicationResult.AWARDED);

        return dto;
    }

    @Test
    @WithMockUser(username = "test.admin@test.com", password = "testAdmin")
    public void testReadFundingApplication() throws Exception {
        String jwtToken = authenticateAdminAndGetToken();

        mockMvc.perform(
                        MockMvcRequestBuilders.get(
                                        "http://localhost:8081/api/funding-application/{fundingApplicationId}", 1)
                                .contentType(MediaType.APPLICATION_JSON)
                                .header(HttpHeaders.AUTHORIZATION, "Bearer " + jwtToken))
                .andExpect(status().isOk());
    }

    @Test
    @WithMockUser(username = "test.admin@test.com", password = "testAdmin")
    public void testCreateFundingApplication() throws Exception {
        String jwtToken = authenticateAdminAndGetToken();

        var fundingApplicationDTO = getTestPayload();

        String requestBody = objectMapper.writeValueAsString(fundingApplicationDTO);
        mockMvc.perform(
                        MockMvcRequestBuilders.post("http://localhost:8081/api/funding-application")
                                .content(requestBody).contentType(MediaType.APPLICATION_JSON)
                                .header(HttpHeaders.AUTHORIZATION, "Bearer " + jwtToken)
                                .header("Idempotency-Key", "MOCK_KEY_FUNDING_APPLICATION"))
                .andExpect(status().isCreated());
    }

    @Test
    @WithMockUser(username = "test.admin@test.com", password = "testAdmin")
    public void testUpdateFundingApplication() throws Exception {
        String jwtToken = authenticateAdminAndGetToken();

        var fundingApplicationDTO = getTestPayload();

        String requestBody = objectMapper.writeValueAsString(fundingApplicationDTO);
        mockMvc.perform(
                        MockMvcRequestBuilders.put(
                                        "http://localhost:8081/api/funding-application/{fundingApplicationId}", 1)
                                .content(requestBody).contentType(MediaType.APPLICATION_JSON)
                                .header(HttpHeaders.AUTHORIZATION, "Bearer " + jwtToken))
                .andExpect(status().isNoContent());
    }

    @Test
    @WithMockUser(username = "test.admin@test.com", password = "testAdmin")
    public void testDeleteFundingApplication() throws Exception {
        String jwtToken = authenticateAdminAndGetToken();

        mockMvc.perform(
                        MockMvcRequestBuilders.delete(
                                        "http://localhost:8081/api/funding-application/{fundingApplicationId}", 1)
                                .contentType(MediaType.APPLICATION_JSON)
                                .header(HttpHeaders.AUTHORIZATION, "Bearer " + jwtToken))
                .andExpect(status().isNoContent());
    }

    @Test
    @WithMockUser(username = "test.admin@test.com", password = "testAdmin")
    public void testSearchFundingApplications() throws Exception {
        String jwtToken = authenticateAdminAndGetToken();

        mockMvc.perform(
                        MockMvcRequestBuilders.get(
                                        "http://localhost:8081/api/funding-application/search?fundingCallId=1&result=AWARDED")
                                .contentType(MediaType.APPLICATION_JSON)
                                .header(HttpHeaders.AUTHORIZATION, "Bearer " + jwtToken))
                .andExpect(status().isOk());
    }
}
