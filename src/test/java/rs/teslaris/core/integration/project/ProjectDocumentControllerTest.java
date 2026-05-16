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
import rs.teslaris.project.dto.project.ProjectDocumentDTO;
import rs.teslaris.project.model.project.ProjectDocumentType;

import java.util.ArrayList;

import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
public class ProjectDocumentControllerTest extends BaseTest {

    @Autowired
    private ObjectMapper objectMapper;

    public static ProjectDocumentDTO getTestPayload() {
        var dto = new ProjectDocumentDTO();

        dto.setProjectId(1);
        dto.setDocumentId(5);

        var textualDescriptionList = new ArrayList<MultilingualContentDTO>();
        MultilingualContentDTO englishDesc = new MultilingualContentDTO();
        englishDesc.setLanguageTagId(1);
        englishDesc.setLanguageTag(LanguageAbbreviations.ENGLISH);
        englishDesc.setContent("This is a test project document description.");
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
        fundingPart.setProjectDocumentId(5);
        fundingParts.add(fundingPart);
        dto.setFundingParts(fundingParts);

        dto.setRelationType(ProjectDocumentType.RESULT);

        return dto;
    }

    @Test
    @WithMockUser(username = "test.admin@test.com", password = "testAdmin")
    public void testAddProjectDocument() throws Exception {
        String jwtToken = authenticateAdminAndGetToken();

        var geneticMaterialDTO = getTestPayload();

        String requestBody = objectMapper.writeValueAsString(geneticMaterialDTO);
        mockMvc.perform(MockMvcRequestBuilders.post("http://localhost:8081/api/project/add-document")
                        .content(requestBody).contentType(MediaType.APPLICATION_JSON)
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + jwtToken)
                        .header("Idempotency-Key", "MOCK_KEY_PROJECT_DOCUMENT"))
                .andExpect(status().isCreated());
    }

}
