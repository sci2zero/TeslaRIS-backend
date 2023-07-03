package rs.teslaris.core;

import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders;
import rs.teslaris.core.dto.AuthenticationResponseDTO;

@SpringBootTest
@AutoConfigureMockMvc
public class ProceedingsControllerIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Test
    @WithMockUser(username = "admin@admin.com", password = "admin")
    public void testReadProceedings() throws Exception {
        mockMvc.perform(
                MockMvcRequestBuilders.get("http://localhost:8081/api/proceedings/{publicationId}", 32)
                    .contentType(MediaType.APPLICATION_JSON)).andExpect(status().isOk())
            .andExpect(jsonPath("$.eisbn").value("MOCK_eISBN"));
    }

//    @Test
//    @WithMockUser(username = "admin@admin.com", password = "admin")
//    public void testCreateProceedings() throws Exception {
//        String jwtToken = authenticateAndGetToken();
//
//        var proceedingsDTO = new ProceedingsDTO();
//        proceedingsDTO.setUris(new HashSet<>());
//        proceedingsDTO.setLanguageTagIds(new ArrayList<>());
//        String requestBody = objectMapper.writeValueAsString(proceedingsDTO);
//
//        mockMvc.perform(MockMvcRequestBuilders.post("http://localhost:8081/api/proceedings")
//                .content(requestBody).contentType(MediaType.APPLICATION_JSON)
//                .header(HttpHeaders.AUTHORIZATION,
//                    "Bearer " + jwtToken))
//            .andExpect(status().isCreated());
//    }

    @Test
    @WithMockUser(username = "admin@admin.com", password = "admin")
    public void testDeleteProceedings() throws Exception {
        String jwtToken = authenticateAndGetToken();

        mockMvc.perform(
                MockMvcRequestBuilders.delete("http://localhost:8081/api/proceedings/{publicationId}",
                        32).contentType(MediaType.APPLICATION_JSON)
                    .header(HttpHeaders.AUTHORIZATION, "Bearer " + jwtToken))
            .andExpect(status().isNoContent());
    }

    private String authenticateAndGetToken() throws Exception {
        String authResponse = mockMvc.perform(
                MockMvcRequestBuilders.post("http://localhost:8081/api/user/authenticate")
                    .content("{\"email\": \"admin@admin.com\", \"password\": \"admin\"}")
                    .contentType(MediaType.APPLICATION_JSON)).andExpect(status().isOk()).andReturn()
            .getResponse().getContentAsString();

        var objectMapper = new ObjectMapper();
        var authenticationResponseDTO =
            objectMapper.readValue(authResponse, AuthenticationResponseDTO.class);

        return authenticationResponseDTO.getToken();
    }
}
