package rs.teslaris.core;

import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.fasterxml.jackson.databind.ObjectMapper;
import java.time.LocalDate;
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
import rs.teslaris.core.dto.commontypes.MultilingualContentDTO;
import rs.teslaris.core.dto.person.PrizeDTO;
import rs.teslaris.core.integration.BaseTest;

@SpringBootTest
@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
public class PrizeControllerTest extends BaseTest {

    @Autowired
    private ObjectMapper objectMapper;


    private PrizeDTO getTestPayload() {
        var dummyMC = List.of(new MultilingualContentDTO(1, "EN", "Content", 1));

        var prizeDTO = new PrizeDTO();
        prizeDTO.setTitle(dummyMC);
        prizeDTO.setDescription(dummyMC);
        prizeDTO.setDate(LocalDate.of(2020, 4, 12));

        return prizeDTO;
    }

    @Test
    @WithMockUser(username = "test.admin@test.com", password = "testAdmin")
    public void testSearchPrizes() throws Exception {
        String jwtToken = authenticateAdminAndGetToken();

        mockMvc.perform(
                MockMvcRequestBuilders.get(
                        "http://localhost:8081/api/prize/simple-search?tokens=title&tokens=content")
                    .contentType(MediaType.APPLICATION_JSON)
                    .header(HttpHeaders.AUTHORIZATION, "Bearer " + jwtToken))
            .andExpect(status().isOk());
    }

    @Test
    @WithMockUser(username = "test.admin@test.com", password = "testAdmin")
    public void testAddPrize() throws Exception {
        String jwtToken = authenticateAdminAndGetToken();

        var prizeDTO = getTestPayload();

        String requestBody = objectMapper.writeValueAsString(prizeDTO);
        mockMvc.perform(
                MockMvcRequestBuilders.post("http://localhost:8081/api/prize/{personId}", 1)
                    .content(requestBody)
                    .contentType(MediaType.APPLICATION_JSON)
                    .header(HttpHeaders.AUTHORIZATION, "Bearer " + jwtToken)
                    .header("Idempotency-Key", "MOCK_KEY_JOURNAL"))
            .andExpect(status().isCreated());
    }

    @Test
    @WithMockUser(username = "test.admin@test.com", password = "testAdmin")
    public void testUpdatePrize() throws Exception {
        String jwtToken = authenticateAdminAndGetToken();

        var prizeDTO = getTestPayload();

        String requestBody = objectMapper.writeValueAsString(prizeDTO);
        mockMvc.perform(
                MockMvcRequestBuilders.put("http://localhost:8081/api/prize/{personId}/{prizeId}",
                        1, 1)
                    .content(requestBody).contentType(MediaType.APPLICATION_JSON)
                    .header(HttpHeaders.AUTHORIZATION, "Bearer " + jwtToken))
            .andExpect(status().isNoContent());
    }

    @Test
    @Order(Integer.MAX_VALUE)
    @WithMockUser(username = "test.admin@test.com", password = "testAdmin")
    public void testDeletePrize() throws Exception {
        String jwtToken = authenticateAdminAndGetToken();

        mockMvc.perform(
                MockMvcRequestBuilders.delete("http://localhost:8081/api/prize/{personId}/{prizeId}",
                        1, 2)
                    .contentType(MediaType.APPLICATION_JSON)
                    .header(HttpHeaders.AUTHORIZATION, "Bearer " + jwtToken))
            .andExpect(status().isNoContent());
    }
}
