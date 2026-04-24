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
import rs.teslaris.project.dto.funding.FundingDTO;
import rs.teslaris.project.model.funding.FundingType;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.Set;

import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
public class FundingControllerTest extends BaseTest {

    @Autowired
    private ObjectMapper objectMapper;

    public static FundingDTO getTestPayload() {
        var dto = new FundingDTO();

        var nameList = new ArrayList<MultilingualContentDTO>();
        MultilingualContentDTO englishName = new MultilingualContentDTO();
        englishName.setLanguageTagId(1);
        englishName.setLanguageTag(LanguageAbbreviations.ENGLISH);
        englishName.setContent("Test Funding");
        englishName.setPriority(1);
        nameList.add(englishName);
        dto.setName(nameList);

        var descriptionList = new ArrayList<MultilingualContentDTO>();
        MultilingualContentDTO englishDesc = new MultilingualContentDTO();
        englishDesc.setLanguageTagId(1);
        englishDesc.setLanguageTag(LanguageAbbreviations.ENGLISH);
        englishDesc.setContent("This is a test funding description.");
        englishDesc.setPriority(1);
        descriptionList.add(englishDesc);
        dto.setDescription(descriptionList);

        var abbreviationList = new ArrayList<MultilingualContentDTO>();
        MultilingualContentDTO englishAbbr = new MultilingualContentDTO();
        englishAbbr.setLanguageTagId(1);
        englishAbbr.setLanguageTag(LanguageAbbreviations.ENGLISH);
        englishAbbr.setContent("TF");
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

        var displayCallList = new ArrayList<MultilingualContentDTO>();
        MultilingualContentDTO englishDisplayCall = new MultilingualContentDTO();
        englishDisplayCall.setLanguageTagId(1);
        englishDisplayCall.setLanguageTag(LanguageAbbreviations.ENGLISH);
        englishDisplayCall.setContent("Horizon Europe Call");
        englishDisplayCall.setPriority(1);
        displayCallList.add(englishDisplayCall);
        dto.setDisplayCall(displayCallList);

        var displayProgramList = new ArrayList<MultilingualContentDTO>();
        MultilingualContentDTO englishDisplayProgram = new MultilingualContentDTO();
        englishDisplayProgram.setLanguageTagId(1);
        englishDisplayProgram.setLanguageTag(LanguageAbbreviations.ENGLISH);
        englishDisplayProgram.setContent("Horizon Europe");
        englishDisplayProgram.setPriority(1);
        displayProgramList.add(englishDisplayProgram);
        dto.setDisplayProgram(displayProgramList);

        var displayFunderList = new ArrayList<MultilingualContentDTO>();
        MultilingualContentDTO englishDisplayFunder = new MultilingualContentDTO();
        englishDisplayFunder.setLanguageTagId(1);
        englishDisplayFunder.setLanguageTag(LanguageAbbreviations.ENGLISH);
        englishDisplayFunder.setContent("European Commission");
        englishDisplayFunder.setPriority(1);
        displayFunderList.add(englishDisplayFunder);
        dto.setDisplayFunder(displayFunderList);

        var researchAreas = new HashSet<Integer>();
        researchAreas.add(1);
        researchAreas.add(2);
        dto.setResearchAreasId(researchAreas);

        dto.setFunderId(1);
        dto.setProjectId(1);
        dto.setFundingCallId(1);

        var fundingTypes = new HashSet<FundingType>();
        fundingTypes.add(FundingType.GRANT);
        dto.setFundingTypes(fundingTypes);

        var monetaryAmount = new MonetaryAmountDTO();
        monetaryAmount.setAmount(250000.0);
        monetaryAmount.setCurrencyId(1);
        dto.setAmount(monetaryAmount);

        dto.setCompetitive(true);
        dto.setRenewable(false);
        dto.setDateSubmitted(LocalDate.of(2024, 11, 1));
        dto.setDateAwarded(LocalDate.of(2025, 1, 1));
        dto.setDateFrom(LocalDate.of(2025, 1, 1));
        dto.setDateTo(LocalDate.of(2027, 12, 31));

        dto.setDoi("10.1234/test.funding");
        dto.setGrantAgreementId("GA-101234567");

        var uris = new HashSet<String>();
        uris.add("https://example.com/funding");
        uris.add("https://example.com/guidelines");
        dto.setUris(uris);

        dto.setInternalIdentifiers(Set.of("INT-2024-001"));
        dto.setOaMandated(true);
        dto.setOaMandateUrl("https://example.com/oa-mandate");

        return dto;
    }

    @Test
    @WithMockUser(username = "test.admin@test.com", password = "testAdmin")
    public void testSearchFunding() throws Exception {
        String jwtToken = authenticateAdminAndGetToken();

        mockMvc.perform(
                        MockMvcRequestBuilders.get(
                                        "http://localhost:8081/api/funding/search?tokens=funding&projectId=1")
                                .contentType(MediaType.APPLICATION_JSON)
                                .header(HttpHeaders.AUTHORIZATION, "Bearer " + jwtToken))
                .andExpect(status().isOk());
    }

    @Test
    @WithMockUser(username = "test.admin@test.com", password = "testAdmin")
    public void testReadFunding() throws Exception {
        String jwtToken = authenticateAdminAndGetToken();

        mockMvc.perform(
                        MockMvcRequestBuilders.get(
                                        "http://localhost:8081/api/funding/{fundingId}", 1)
                                .contentType(MediaType.APPLICATION_JSON)
                                .header(HttpHeaders.AUTHORIZATION, "Bearer " + jwtToken))
                .andExpect(status().isOk());
    }

    @Test
    @WithMockUser(username = "test.admin@test.com", password = "testAdmin")
    public void testCreateFunding() throws Exception {
        String jwtToken = authenticateAdminAndGetToken();

        var geneticMaterialDTO = getTestPayload();

        String requestBody = objectMapper.writeValueAsString(geneticMaterialDTO);
        mockMvc.perform(MockMvcRequestBuilders.post("http://localhost:8081/api/funding")
                        .content(requestBody).contentType(MediaType.APPLICATION_JSON)
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + jwtToken)
                        .header("Idempotency-Key", "MOCK_KEY_FUNDING"))
                .andExpect(status().isCreated());
    }

    @Test
    @WithMockUser(username = "test.admin@test.com", password = "testAdmin")
    public void testUpdateFunding() throws Exception {
        String jwtToken = authenticateAdminAndGetToken();

        var geneticMaterialDTO = getTestPayload();

        String requestBody = objectMapper.writeValueAsString(geneticMaterialDTO);
        mockMvc.perform(MockMvcRequestBuilders.put(
                                "http://localhost:8081/api/funding/{fundingCallId}", 1)
                        .content(requestBody).contentType(MediaType.APPLICATION_JSON)
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + jwtToken))
                .andExpect(status().isNoContent());
    }

    @Test
    @WithMockUser(username = "test.admin@test.com", password = "testAdmin")
    public void testDeleteFunding() throws Exception {
        String jwtToken = authenticateAdminAndGetToken();

        mockMvc.perform(
                        MockMvcRequestBuilders.delete(
                                        "http://localhost:8081/api/funding/{fundingId}", 2)
                                .contentType(MediaType.APPLICATION_JSON)
                                .header(HttpHeaders.AUTHORIZATION, "Bearer " + jwtToken))
                .andExpect(status().isNoContent());
    }
}
