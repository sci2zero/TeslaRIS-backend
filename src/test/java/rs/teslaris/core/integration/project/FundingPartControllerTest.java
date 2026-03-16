package rs.teslaris.core.integration.project;

import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.fasterxml.jackson.databind.ObjectMapper;
import java.util.ArrayList;
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
import rs.teslaris.project.dto.funding.FundingPartDTO;

@SpringBootTest
public class FundingPartControllerTest extends BaseTest {

    @Autowired
    private ObjectMapper objectMapper;


    public static FundingPartDTO getTestPayload() {
        var dto = new FundingPartDTO();

        var descriptionList = new ArrayList<MultilingualContentDTO>();
        MultilingualContentDTO englishDesc = new MultilingualContentDTO();
        englishDesc.setLanguageTagId(1);
        englishDesc.setLanguageTag(LanguageAbbreviations.ENGLISH);
        englishDesc.setContent("This is a test funding part description.");
        englishDesc.setPriority(1);
        descriptionList.add(englishDesc);
        dto.setDescription(descriptionList);

        var keywordsList = new ArrayList<MultilingualContentDTO>();
        MultilingualContentDTO keyword1 = new MultilingualContentDTO();
        keyword1.setLanguageTagId(1);
        keyword1.setLanguageTag(LanguageAbbreviations.ENGLISH);
        keyword1.setContent("research");
        keyword1.setPriority(1);
        keywordsList.add(keyword1);

        dto.setFundingId(3);
        dto.setForFundingId(1);

        var monetaryAmount = new MonetaryAmountDTO();
        monetaryAmount.setAmount(100);
        monetaryAmount.setCurrencyId(1);
        dto.setCosts(monetaryAmount);

        return dto;
    }

    @Test
    @WithMockUser(username = "test.admin@test.com", password = "testAdmin")
    public void testCreateFundingPart() throws Exception {
        String jwtToken = authenticateAdminAndGetToken();

        var geneticMaterialDTO = getTestPayload();

        String requestBody = objectMapper.writeValueAsString(geneticMaterialDTO);
        mockMvc.perform(MockMvcRequestBuilders.post("http://localhost:8081/api/funding-part")
                .content(requestBody).contentType(MediaType.APPLICATION_JSON)
                .header(HttpHeaders.AUTHORIZATION, "Bearer " + jwtToken)
                .header("Idempotency-Key", "MOCK_KEY_FUNDING_PART"))
            .andExpect(status().isCreated());
    }

    @Test
    @WithMockUser(username = "test.admin@test.com", password = "testAdmin")
    public void testUpdateFundingPart() throws Exception {
        String jwtToken = authenticateAdminAndGetToken();

        var geneticMaterialDTO = getTestPayload();

        String requestBody = objectMapper.writeValueAsString(geneticMaterialDTO);
        mockMvc.perform(MockMvcRequestBuilders.put(
                    "http://localhost:8081/api/funding-part/{fundingPartId}", 1)
                .content(requestBody).contentType(MediaType.APPLICATION_JSON)
                .header(HttpHeaders.AUTHORIZATION, "Bearer " + jwtToken))
            .andExpect(status().isNoContent());
    }

    @Test
    @WithMockUser(username = "test.admin@test.com", password = "testAdmin")
    public void testDeleteFundingPart() throws Exception {
        String jwtToken = authenticateAdminAndGetToken();

        mockMvc.perform(
                MockMvcRequestBuilders.delete(
                        "http://localhost:8081/api/funding-part/{fundingPartId}", 2)
                    .contentType(MediaType.APPLICATION_JSON)
                    .header(HttpHeaders.AUTHORIZATION, "Bearer " + jwtToken))
            .andExpect(status().isNoContent());
    }
}
