package rs.teslaris.project.integration;

import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders;
import rs.teslaris.core.dto.identifier.EntityIdentifierResponseDTO;
import rs.teslaris.core.integration.BaseTest;
import rs.teslaris.project.dto.identifier.FundingIdentifierDTO;

@SpringBootTest
public class FundingIdentifierControllerTest extends BaseTest {

    @Autowired
    private ObjectMapper objectMapper;


    private FundingIdentifierDTO getTestPayload(String value) {
        return new FundingIdentifierDTO(value, 1, 1);
    }

    @Test
    public void testReadFundingIdentifiersForFunding() throws Exception {
        mockMvc.perform(
            MockMvcRequestBuilders.get(
                    "http://localhost:8081/api/funding-identifier/{fundingId}", 1)
                .contentType(MediaType.APPLICATION_JSON)).andExpect(status().isOk());
    }

    @Test
    @WithMockUser(username = "test.admin@test.com", password = "testAdmin")
    public void testCreateFundingIdentifierSuccess() throws Exception {
        String jwtToken = authenticateAdminAndGetToken();

        var fundingIdentifierDTO = getTestPayload("22221");

        String requestBody = objectMapper.writeValueAsString(fundingIdentifierDTO);
        mockMvc.perform(
                MockMvcRequestBuilders.post("http://localhost:8081/api/funding-identifier/{fundingId}",
                        fundingIdentifierDTO.getFundingId())
                    .content(requestBody)
                    .contentType(MediaType.APPLICATION_JSON)
                    .header(HttpHeaders.AUTHORIZATION, "Bearer " + jwtToken)
                    .header("Idempotency-Key", "MOCK_KEY_FUNDING_IDENTIFIER_1"))
            .andExpect(status().isCreated());
    }

    @Test
    @WithMockUser(username = "test.admin@test.com", password = "testAdmin")
    public void testCreateFundingIdentifierWrongFormat() throws Exception {
        String jwtToken = authenticateAdminAndGetToken();

        var fundingIdentifierDTO = getTestPayload("WRONG_FORMAT");

        String requestBody = objectMapper.writeValueAsString(fundingIdentifierDTO);
        mockMvc.perform(
                MockMvcRequestBuilders.post("http://localhost:8081/api/funding-identifier/{fundingId}",
                        fundingIdentifierDTO.getFundingId())
                    .content(requestBody)
                    .contentType(MediaType.APPLICATION_JSON)
                    .header(HttpHeaders.AUTHORIZATION, "Bearer " + jwtToken)
                    .header("Idempotency-Key", "MOCK_KEY_FUNDING_IDENTIFIER_2"))
            .andExpect(status().isConflict());
    }

    @Test
    @WithMockUser(username = "test.admin@test.com", password = "testAdmin")
    public void testUpdateFundingIdentifier() throws Exception {
        String jwtToken = authenticateAdminAndGetToken();

        var createdFundingIdentifierDTO = getTestPayload("22222");

        var createResponse = mockMvc.perform(
                MockMvcRequestBuilders.post("http://localhost:8081/api/funding-identifier/{fundingId}",
                        createdFundingIdentifierDTO.getFundingId())
                    .content(objectMapper.writeValueAsString(createdFundingIdentifierDTO))
                    .contentType(MediaType.APPLICATION_JSON)
                    .header(HttpHeaders.AUTHORIZATION, "Bearer " + jwtToken)
                    .header("Idempotency-Key", "MOCK_KEY_FUNDING_IDENTIFIER_3"))
            .andExpect(status().isCreated())
            .andReturn();

        var createdFundingIdentifier = objectMapper.readValue(
            createResponse.getResponse().getContentAsString(), EntityIdentifierResponseDTO.class);

        var fundingIdentifierDTO = getTestPayload("22223");

        String requestBody = objectMapper.writeValueAsString(fundingIdentifierDTO);
        mockMvc.perform(
                MockMvcRequestBuilders.put(
                        "http://localhost:8081/api/funding-identifier/{fundingId}/{identifierId}",
                        fundingIdentifierDTO.getFundingId(), createdFundingIdentifier.id())
                    .content(requestBody).contentType(MediaType.APPLICATION_JSON)
                    .header(HttpHeaders.AUTHORIZATION, "Bearer " + jwtToken))
            .andExpect(status().isNoContent());
    }
}
