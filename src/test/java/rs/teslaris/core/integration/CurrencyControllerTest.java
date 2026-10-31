package rs.teslaris.core.integration;

import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.fasterxml.jackson.databind.ObjectMapper;
import java.util.List;
import org.junit.jupiter.api.Order;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders;
import rs.teslaris.core.dto.commontypes.CurrencyDTO;
import rs.teslaris.core.dto.commontypes.MultilingualContentDTO;

@SpringBootTest
public class CurrencyControllerTest extends BaseTest {

    @Autowired
    private ObjectMapper objectMapper;


    private CurrencyDTO getTestPayload() {
        var dummyMC = List.of(new MultilingualContentDTO(1, "EN", "Content", 1));

        var currencyDTO = new CurrencyDTO();
        currencyDTO.setCurrencyName(dummyMC);
        currencyDTO.setSymbol("SYM");
        currencyDTO.setCode("CURRENCY_CODE_MOCK");

        return currencyDTO;
    }

    @Test
    public void testReadCurrency() throws Exception {
        mockMvc.perform(
            MockMvcRequestBuilders.get("http://localhost:8081/api/currency/{currencyId}", 1)
                .contentType(MediaType.APPLICATION_JSON)).andExpect(status().isOk());
    }

    @Test
    @WithMockUser(username = "test.admin@test.com", password = "testAdmin")
    public void testSearchCurrencies() throws Exception {
        String jwtToken = authenticateAdminAndGetToken();

        mockMvc.perform(
                MockMvcRequestBuilders.get(
                        "http://localhost:8081/api/currency/search?tokens=euro&lang=en")
                    .contentType(MediaType.APPLICATION_JSON)
                    .header(HttpHeaders.AUTHORIZATION, "Bearer " + jwtToken))
            .andExpect(status().isOk());
    }

    @Test
    @WithMockUser(username = "test.admin@test.com", password = "testAdmin")
    public void testCreateCurrency() throws Exception {
        String jwtToken = authenticateAdminAndGetToken();

        var currencyDTO = getTestPayload();

        String requestBody = objectMapper.writeValueAsString(currencyDTO);
        mockMvc.perform(MockMvcRequestBuilders.post("http://localhost:8081/api/currency")
                .content(requestBody).contentType(MediaType.APPLICATION_JSON)
                .header(HttpHeaders.AUTHORIZATION, "Bearer " + jwtToken)
                .header("Idempotency-Key", "MOCK_KEY_CURRENCY"))
            .andExpect(status().isCreated())
            .andExpect(jsonPath("$.code").value("CURRENCY_CODE_MOCK"));
    }

    @Test
    @WithMockUser(username = "test.admin@test.com", password = "testAdmin")
    public void testUpdateCurrency() throws Exception {
        String jwtToken = authenticateAdminAndGetToken();

        var currencyDTO = getTestPayload();
        currencyDTO.setCode("USD");
        currencyDTO.setSymbol("$");

        String requestBody = objectMapper.writeValueAsString(currencyDTO);
        mockMvc.perform(
                MockMvcRequestBuilders.put("http://localhost:8081/api/currency/{currencyId}", 2)
                    .content(requestBody).contentType(MediaType.APPLICATION_JSON)
                    .header(HttpHeaders.AUTHORIZATION, "Bearer " + jwtToken))
            .andExpect(status().isNoContent());
    }

    @Test
    @WithMockUser(username = "test.admin@test.com", password = "testAdmin")
    public void testDeleteCurrencyInUse() throws Exception {
        String jwtToken = authenticateAdminAndGetToken();

        mockMvc.perform(
                MockMvcRequestBuilders.delete("http://localhost:8081/api/currency/{currencyId}",
                        1).contentType(MediaType.APPLICATION_JSON)
                    .header(HttpHeaders.AUTHORIZATION, "Bearer " + jwtToken))
            .andExpect(status().isConflict());
    }

    @Test
    @Order(Integer.MAX_VALUE)
    @WithMockUser(username = "test.admin@test.com", password = "testAdmin")
    public void testDeleteCurrency() throws Exception {
        String jwtToken = authenticateAdminAndGetToken();

        mockMvc.perform(
                MockMvcRequestBuilders.delete("http://localhost:8081/api/currency/{currencyId}",
                        3).contentType(MediaType.APPLICATION_JSON)
                    .header(HttpHeaders.AUTHORIZATION, "Bearer " + jwtToken))
            .andExpect(status().isNoContent());
    }
}
