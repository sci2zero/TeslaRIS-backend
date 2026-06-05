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
import rs.teslaris.project.dto.funding.FundingPartDTO;
import rs.teslaris.project.dto.project.ProjectEventDTO;
import rs.teslaris.project.model.project.ProjectEventType;

import java.util.ArrayList;

import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
public class ProjectEventControllerTest extends BaseTest {

    @Autowired
    private ObjectMapper objectMapper;

    public static ProjectEventDTO getTestPayload() {
        var dto = new ProjectEventDTO();

        dto.setProjectId(1);
        dto.setEventId(5);

        var textualDescriptionList = new ArrayList<MultilingualContentDTO>();
        MultilingualContentDTO englishDesc = new MultilingualContentDTO();
        englishDesc.setLanguageTagId(1);
        englishDesc.setLanguageTag(LanguageAbbreviations.ENGLISH);
        englishDesc.setContent("This is a test project event description.");
        englishDesc.setPriority(1);
        textualDescriptionList.add(englishDesc);
        dto.setTextualDescription(textualDescriptionList);

        var fundingParts = new ArrayList<FundingPartDTO>();
        FundingPartDTO fundingPart = new FundingPartDTO();
        fundingPart.setFundingId(1);

        var fundingDescList = new ArrayList<MultilingualContentDTO>();
        MultilingualContentDTO fundingDesc = new MultilingualContentDTO();
        fundingDesc.setLanguageTagId(1);
        fundingDesc.setLanguageTag(LanguageAbbreviations.ENGLISH);
        fundingDesc.setContent("Test funding part description.");
        fundingDesc.setPriority(1);
        fundingDescList.add(fundingDesc);
        fundingPart.setDescription(fundingDescList);

        fundingPart.setAmount(new MonetaryAmountDTO(1, 50000));
        fundingPart.setProjectEventId(5);
        fundingParts.add(fundingPart);
        dto.setFundingParts(fundingParts);

        dto.setRelationType(ProjectEventType.MEETING);

        return dto;
    }

    @Test
    @WithMockUser(username = "test.admin@test.com", password = "testAdmin")
    public void testAddProjectEvent() throws Exception {
        String jwtToken = authenticateAdminAndGetToken();

        var geneticMaterialDTO = getTestPayload();

        String requestBody = objectMapper.writeValueAsString(geneticMaterialDTO);
        mockMvc.perform(MockMvcRequestBuilders.post("http://localhost:8081/api/project/add-event")
                        .content(requestBody).contentType(MediaType.APPLICATION_JSON)
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + jwtToken)
                        .header("Idempotency-Key", "MOCK_KEY_PROJECT_EVENT"))
                .andExpect(status().isCreated());
    }

    @Test
    @WithMockUser(username = "test.admin@test.com", password = "testAdmin")
    public void testRemoveProjectEvent() throws Exception {
        String jwtToken = authenticateAdminAndGetToken();

        mockMvc.perform(
                        MockMvcRequestBuilders.delete(
                                        "http://localhost:8081/api/project/remove-event/{projectEventId}", 1)
                                .contentType(MediaType.APPLICATION_JSON)
                                .header(HttpHeaders.AUTHORIZATION, "Bearer " + jwtToken))
                .andExpect(status().isNoContent());
    }

}
