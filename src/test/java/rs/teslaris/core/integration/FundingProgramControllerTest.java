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
import rs.teslaris.core.dto.project.FundingProgramDTO;
import rs.teslaris.core.model.project.FundingType;
import rs.teslaris.core.util.language.LanguageAbbreviations;

@SpringBootTest
public class FundingProgramControllerTest extends BaseTest {

    @Autowired
    private ObjectMapper objectMapper;


    public static FundingProgramDTO getTestPayload() {
        var dto = new FundingProgramDTO();

        var nameList = new ArrayList<MultilingualContentDTO>();
        MultilingualContentDTO englishName = new MultilingualContentDTO();
        englishName.setLanguageTagId(1);
        englishName.setLanguageTag(LanguageAbbreviations.ENGLISH);
        englishName.setContent("Test Funding Program");
        englishName.setPriority(1);
        nameList.add(englishName);
        dto.setName(nameList);

        var descriptionList = new ArrayList<MultilingualContentDTO>();
        MultilingualContentDTO englishDesc = new MultilingualContentDTO();
        englishDesc.setLanguageTagId(1);
        englishDesc.setLanguageTag(LanguageAbbreviations.ENGLISH);
        englishDesc.setContent("This is a test funding program description.");
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

        dto.setFunderId(1);

        var fundingTypes = new HashSet<FundingType>();
        fundingTypes.add(FundingType.GRANT);
        fundingTypes.add(FundingType.CALL);
        dto.setFundingTypes(fundingTypes);

        var monetaryAmount = new MonetaryAmountDTO();
        monetaryAmount.setAmount(100);
        monetaryAmount.setCurrencyId(1);
        dto.setTotalAmount(monetaryAmount);

        dto.setDateFrom(LocalDate.now().plusMonths(1));
        dto.setDateTo(LocalDate.now().plusYears(1));

        var uris = new HashSet<String>();
        uris.add("https://example.com/funding-program");
        uris.add("https://example.com/guidelines");
        dto.setUris(uris);

        dto.setOaMandated(true);
        dto.setOaMandateUrl("https://example.com/oa-mandate");

        return dto;
    }

    @Test
    @WithMockUser(username = "test.admin@test.com", password = "testAdmin")
    public void testSearchFundingPrograms() throws Exception {
        String jwtToken = authenticateAdminAndGetToken();

        mockMvc.perform(
                MockMvcRequestBuilders.get(
                        "http://localhost:8081/api/funding-program/search?tokens=title&tokens=content")
                    .contentType(MediaType.APPLICATION_JSON)
                    .header(HttpHeaders.AUTHORIZATION, "Bearer " + jwtToken))
            .andExpect(status().isOk());
    }

    @Test
    @WithMockUser(username = "test.admin@test.com", password = "testAdmin")
    public void testReadFundingProgram() throws Exception {
        String jwtToken = authenticateAdminAndGetToken();

        mockMvc.perform(
                MockMvcRequestBuilders.get(
                        "http://localhost:8081/api/funding-program/{fundingProgramId}", 1)
                    .contentType(MediaType.APPLICATION_JSON)
                    .header(HttpHeaders.AUTHORIZATION, "Bearer " + jwtToken))
            .andExpect(status().isOk());
    }

    @Test
    @WithMockUser(username = "test.admin@test.com", password = "testAdmin")
    public void testCreateFundingProgram() throws Exception {
        String jwtToken = authenticateAdminAndGetToken();

        var geneticMaterialDTO = getTestPayload();

        String requestBody = objectMapper.writeValueAsString(geneticMaterialDTO);
        mockMvc.perform(MockMvcRequestBuilders.post("http://localhost:8081/api/funding-program")
                .content(requestBody).contentType(MediaType.APPLICATION_JSON)
                .header(HttpHeaders.AUTHORIZATION, "Bearer " + jwtToken)
                .header("Idempotency-Key", "MOCK_KEY_FUNDING_PROGRAM"))
            .andExpect(status().isCreated());
    }

    @Test
    @WithMockUser(username = "test.admin@test.com", password = "testAdmin")
    public void testUpdateFundingProgram() throws Exception {
        String jwtToken = authenticateAdminAndGetToken();

        var geneticMaterialDTO = getTestPayload();

        String requestBody = objectMapper.writeValueAsString(geneticMaterialDTO);
        mockMvc.perform(MockMvcRequestBuilders.put(
                    "http://localhost:8081/api/funding-program/{fundingProgramId}", 1)
                .content(requestBody).contentType(MediaType.APPLICATION_JSON)
                .header(HttpHeaders.AUTHORIZATION, "Bearer " + jwtToken))
            .andExpect(status().isNoContent());
    }

    @Test
    @WithMockUser(username = "test.admin@test.com", password = "testAdmin")
    public void testDeleteFundingProgram() throws Exception {
        String jwtToken = authenticateAdminAndGetToken();

        mockMvc.perform(
                MockMvcRequestBuilders.delete(
                        "http://localhost:8081/api/funding-program/{fundingProgramId}", 2)
                    .contentType(MediaType.APPLICATION_JSON)
                    .header(HttpHeaders.AUTHORIZATION, "Bearer " + jwtToken))
            .andExpect(status().isNoContent());
    }
}
