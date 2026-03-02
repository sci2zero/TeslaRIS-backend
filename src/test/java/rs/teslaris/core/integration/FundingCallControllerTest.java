package rs.teslaris.core.integration;

import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.fasterxml.jackson.databind.ObjectMapper;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.HashSet;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders;
import rs.teslaris.core.dto.commontypes.MonetaryAmountDTO;
import rs.teslaris.core.dto.commontypes.MultilingualContentDTO;
import rs.teslaris.core.dto.project.FundingCallDTO;
import rs.teslaris.core.model.project.FundingType;
import rs.teslaris.core.util.language.LanguageAbbreviations;

@SpringBootTest
public class FundingCallControllerTest extends BaseTest {

    @Autowired
    private ObjectMapper objectMapper;


    public static FundingCallDTO getTestPayload() {
        var dto = new FundingCallDTO();

        var nameList = new ArrayList<MultilingualContentDTO>();
        MultilingualContentDTO englishName = new MultilingualContentDTO();
        englishName.setLanguageTagId(1);
        englishName.setLanguageTag(LanguageAbbreviations.ENGLISH);
        englishName.setContent("Test Funding Call");
        englishName.setPriority(1);
        nameList.add(englishName);
        dto.setName(nameList);

        var descriptionList = new ArrayList<MultilingualContentDTO>();
        MultilingualContentDTO englishDesc = new MultilingualContentDTO();
        englishDesc.setLanguageTagId(1);
        englishDesc.setLanguageTag(LanguageAbbreviations.ENGLISH);
        englishDesc.setContent("This is a test funding call description.");
        englishDesc.setPriority(1);
        descriptionList.add(englishDesc);
        dto.setDescription(descriptionList);

        var objectivesList = new ArrayList<MultilingualContentDTO>();
        MultilingualContentDTO englishObj = new MultilingualContentDTO();
        englishObj.setLanguageTagId(1);
        englishObj.setLanguageTag(LanguageAbbreviations.ENGLISH);
        englishObj.setContent("Test objective 1");
        englishObj.setPriority(1);
        objectivesList.add(englishObj);
        dto.setObjectives(objectivesList);

        var abbreviationList = new ArrayList<MultilingualContentDTO>();
        MultilingualContentDTO englishAbbr = new MultilingualContentDTO();
        englishAbbr.setLanguageTagId(1);
        englishAbbr.setLanguageTag(LanguageAbbreviations.ENGLISH);
        englishAbbr.setContent("TFP");
        englishAbbr.setPriority(1);
        abbreviationList.add(englishAbbr);
        dto.setNameAbbreviation(abbreviationList);

        var keywordsList = new ArrayList<MultilingualContentDTO>();
        MultilingualContentDTO keyword1 = new MultilingualContentDTO();
        keyword1.setLanguageTagId(1);
        keyword1.setLanguageTag(LanguageAbbreviations.ENGLISH);
        keyword1.setContent("research");
        keyword1.setPriority(1);
        keywordsList.add(keyword1);

        var keyword2 = new MultilingualContentDTO();
        keyword2.setLanguageTagId(1);
        keyword2.setLanguageTag(LanguageAbbreviations.ENGLISH);
        keyword2.setContent("innovation");
        keyword2.setPriority(2);
        keywordsList.add(keyword2);
        dto.setKeywords(keywordsList);

        var researchAreas = new HashSet<Integer>();
        researchAreas.add(1);
        researchAreas.add(2);
        dto.setResearchAreasId(researchAreas);

        dto.setFundingProgramId(1);

        var fundingTypes = new HashSet<FundingType>();
        fundingTypes.add(FundingType.GRANT);
        fundingTypes.add(FundingType.CALL);
        dto.setFundingTypes(fundingTypes);

        var monetaryAmount = new MonetaryAmountDTO();
        monetaryAmount.setAmount(100);
        monetaryAmount.setCurrencyId(1);
        dto.setMonetaryAmount(monetaryAmount);

        dto.setCallOpens(LocalDate.of(2025, 1, 1));
        dto.setCallCloses(LocalDate.of(2026, 3, 1));

        var uris = new HashSet<String>();
        uris.add("https://example.com/funding-call");
        uris.add("https://example.com/guidelines");
        dto.setUris(uris);

        dto.setOaMandated(true);
        dto.setOaMandateUrl("https://example.com/oa-mandate");

        return dto;
    }

    @Test
    @WithMockUser(username = "test.admin@test.com", password = "testAdmin")
    public void testSearchFundingCalls() throws Exception {
        String jwtToken = authenticateAdminAndGetToken();

        mockMvc.perform(
                MockMvcRequestBuilders.get(
                        "http://localhost:8081/api/funding-call/search?tokens=title&tokens=content&programId=1")
                    .contentType(MediaType.APPLICATION_JSON)
                    .header(HttpHeaders.AUTHORIZATION, "Bearer " + jwtToken))
            .andExpect(status().isOk());
    }

    @Test
    @WithMockUser(username = "test.admin@test.com", password = "testAdmin")
    public void testReadFundingCall() throws Exception {
        String jwtToken = authenticateAdminAndGetToken();

        mockMvc.perform(
                MockMvcRequestBuilders.get(
                        "http://localhost:8081/api/funding-call/{fundingCallId}", 1)
                    .contentType(MediaType.APPLICATION_JSON)
                    .header(HttpHeaders.AUTHORIZATION, "Bearer " + jwtToken))
            .andExpect(status().isOk());
    }

    @Test
    @WithMockUser(username = "test.admin@test.com", password = "testAdmin")
    public void testCreateFundingCall() throws Exception {
        String jwtToken = authenticateAdminAndGetToken();

        var geneticMaterialDTO = getTestPayload();

        String requestBody = objectMapper.writeValueAsString(geneticMaterialDTO);
        mockMvc.perform(MockMvcRequestBuilders.post("http://localhost:8081/api/funding-call")
                .content(requestBody).contentType(MediaType.APPLICATION_JSON)
                .header(HttpHeaders.AUTHORIZATION, "Bearer " + jwtToken)
                .header("Idempotency-Key", "MOCK_KEY_FUNDING_CALL"))
            .andExpect(status().isCreated());
    }

    @Test
    @WithMockUser(username = "test.admin@test.com", password = "testAdmin")
    public void testUpdateFundingCall() throws Exception {
        String jwtToken = authenticateAdminAndGetToken();

        var geneticMaterialDTO = getTestPayload();

        String requestBody = objectMapper.writeValueAsString(geneticMaterialDTO);
        mockMvc.perform(MockMvcRequestBuilders.put(
                    "http://localhost:8081/api/funding-call/{fundingCallId}", 1)
                .content(requestBody).contentType(MediaType.APPLICATION_JSON)
                .header(HttpHeaders.AUTHORIZATION, "Bearer " + jwtToken))
            .andExpect(status().isNoContent());
    }

    @Test
    @WithMockUser(username = "test.admin@test.com", password = "testAdmin")
    public void testDeleteFundingCall() throws Exception {
        String jwtToken = authenticateAdminAndGetToken();

        mockMvc.perform(
                MockMvcRequestBuilders.delete(
                        "http://localhost:8081/api/funding-call/{fundingCallId}", 2)
                    .contentType(MediaType.APPLICATION_JSON)
                    .header(HttpHeaders.AUTHORIZATION, "Bearer " + jwtToken))
            .andExpect(status().isNoContent());
    }
}
