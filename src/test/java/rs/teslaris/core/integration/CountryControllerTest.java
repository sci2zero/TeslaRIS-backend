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
import rs.teslaris.core.dto.commontypes.CountryDTO;
import rs.teslaris.core.dto.commontypes.MultilingualContentDTO;

@SpringBootTest
public class CountryControllerTest extends BaseTest {

    @Autowired
    private ObjectMapper objectMapper;


    private CountryDTO getTestPayload() {
        var dummyMC = List.of(new MultilingualContentDTO(1, "EN", "Content", 1));

        var countryDTO = new CountryDTO();
        countryDTO.setName(dummyMC);
        countryDTO.setCode("COUNTRY_CODE_MOCK");

        return countryDTO;
    }

    @Test
    public void testReadCountry() throws Exception {
        mockMvc.perform(
            MockMvcRequestBuilders.get("http://localhost:8081/api/country/{countryId}", 1)
                .contentType(MediaType.APPLICATION_JSON)).andExpect(status().isOk());
    }

    @Test
    @WithMockUser(username = "test.admin@test.com", password = "testAdmin")
    public void testCreateCountry() throws Exception {
        String jwtToken = authenticateAdminAndGetToken();

        var countryDTO = getTestPayload();

        String requestBody = objectMapper.writeValueAsString(countryDTO);
        mockMvc.perform(MockMvcRequestBuilders.post("http://localhost:8081/api/country")
                .content(requestBody).contentType(MediaType.APPLICATION_JSON)
                .header(HttpHeaders.AUTHORIZATION, "Bearer " + jwtToken)
                .header("Idempotency-Key", "MOCK_KEY_COUNTRY"))
            .andExpect(status().isCreated())
            .andExpect(jsonPath("$.code").value("COUNTRY_CODE_MOCK"));
    }

    @Test
    @WithMockUser(username = "test.admin@test.com", password = "testAdmin")
    public void testUpdateCountry() throws Exception {
        String jwtToken = authenticateAdminAndGetToken();

        var countryDTO = getTestPayload();
        countryDTO.setCode("NEW_CODE_MOCK");

        String requestBody = objectMapper.writeValueAsString(countryDTO);
        mockMvc.perform(
                MockMvcRequestBuilders.put("http://localhost:8081/api/country/{countryId}", 1)
                    .content(requestBody).contentType(MediaType.APPLICATION_JSON)
                    .header(HttpHeaders.AUTHORIZATION, "Bearer " + jwtToken))
            .andExpect(status().isNoContent());
    }

    @Test
    @Order(Integer.MAX_VALUE)
    @WithMockUser(username = "test.admin@test.com", password = "testAdmin")
    public void testDeleteCountry() throws Exception {
        String jwtToken = authenticateAdminAndGetToken();

        mockMvc.perform(
                MockMvcRequestBuilders.delete("http://localhost:8081/api/country/{countryId}",
                        2).contentType(MediaType.APPLICATION_JSON)
                    .header(HttpHeaders.AUTHORIZATION, "Bearer " + jwtToken))
            .andExpect(status().isNoContent());
    }
}
