package rs.teslaris.core.integration.assessment;

import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.fasterxml.jackson.databind.ObjectMapper;
import java.util.List;
import org.junit.jupiter.api.MethodOrderer;
import org.junit.jupiter.api.Order;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestMethodOrder;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders;
import rs.teslaris.assessment.dto.CommissionRelationDTO;
import rs.teslaris.assessment.dto.ReorderCommissionRelationDTO;
import rs.teslaris.core.integration.BaseTest;
import rs.teslaris.core.model.institution.ResultCalculationMethod;

@SpringBootTest
@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
public class CommissionRelationControllerTest extends BaseTest {

    @Autowired
    private ObjectMapper objectMapper;

    private CommissionRelationDTO getTestPayload() {
        return new CommissionRelationDTO(2, List.of(1), 1, ResultCalculationMethod.BEST_VALUE);
    }

    @Test
    @WithMockUser(username = "test.admin@test.com", password = "testAdmin")
    public void testReadAllCommissionRelations() throws Exception {
        String jwtToken = authenticateAdminAndGetToken();

        mockMvc.perform(
            MockMvcRequestBuilders.get(
                    "http://localhost:8081/api/assessment/commission-relation/{commissionId}", 2)
                .contentType(MediaType.APPLICATION_JSON)
                .header(HttpHeaders.AUTHORIZATION, "Bearer " + jwtToken)
        ).andExpect(status().isOk());
    }

    @Test
    @WithMockUser(username = "test.admin@test.com", password = "testAdmin")
    public void testCreateCommissionRelation() throws Exception {
        String jwtToken = authenticateAdminAndGetToken();

        var commissionRelationDTO = getTestPayload();

        String requestBody = objectMapper.writeValueAsString(commissionRelationDTO);
        mockMvc.perform(
                MockMvcRequestBuilders.patch(
                        "http://localhost:8081/api/assessment/commission-relation/{commissionId}", 2)
                    .content(requestBody)
                    .contentType(MediaType.APPLICATION_JSON)
                    .header(HttpHeaders.AUTHORIZATION, "Bearer " + jwtToken)
                    .header("Idempotency-Key", "MOCK_KEY_COMMISSION_RELATION"))
            .andExpect(status().isCreated());
    }

    @Test
    @WithMockUser(username = "test.admin@test.com", password = "testAdmin")
    public void testUpdateCommissionRelation() throws Exception {
        String jwtToken = authenticateAdminAndGetToken();

        var commissionRelationDTO = getTestPayload();

        String requestBody = objectMapper.writeValueAsString(commissionRelationDTO);
        mockMvc.perform(
                MockMvcRequestBuilders.put(
                        "http://localhost:8081/api/assessment/commission-relation/{commissionId}/{commissionRelationId}",
                        2, 1)
                    .content(requestBody)
                    .contentType(MediaType.APPLICATION_JSON)
                    .header(HttpHeaders.AUTHORIZATION, "Bearer " + jwtToken))
            .andExpect(status().isNoContent());
    }

    @Test
    @WithMockUser(username = "test.admin@test.com", password = "testAdmin")
    @Order(Integer.MAX_VALUE)
    public void testDeleteCommissionRelation() throws Exception {
        String jwtToken = authenticateAdminAndGetToken();
        mockMvc.perform(
                MockMvcRequestBuilders.delete(
                        "http://localhost:8081/api/assessment/commission-relation/{commissionId}/{commissionRelationId}",
                        5, 1)
                    .header(HttpHeaders.AUTHORIZATION, "Bearer " + jwtToken))
            .andExpect(status().isNoContent());
    }

    @Test
    @WithMockUser(username = "test.admin@test.com", password = "testAdmin")
    public void testReorderCommissionRelations() throws Exception {
        String jwtToken = authenticateAdminAndGetToken();

        var reorderDTO = new ReorderCommissionRelationDTO();
        reorderDTO.setOldRelationPriority(2);
        reorderDTO.setNewRelationPriority(1);
        String requestBody = objectMapper.writeValueAsString(reorderDTO);

        mockMvc.perform(
                MockMvcRequestBuilders.patch(
                        "http://localhost:8081/api/assessment/commission-relation/{commissionId}/{commissionRelationId}",
                        5, 2)
                    .content(requestBody)
                    .contentType(MediaType.APPLICATION_JSON)
                    .header(HttpHeaders.AUTHORIZATION, "Bearer " + jwtToken)
                    .header("Idempotency-Key", "MOCK_KEY_COMMISSION_RELATION_REORDER"))
            .andExpect(status().isNoContent());
    }
}
