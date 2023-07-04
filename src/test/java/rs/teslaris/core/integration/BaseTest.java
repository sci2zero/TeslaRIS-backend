package rs.teslaris.core.integration;

import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders;
import rs.teslaris.core.dto.AuthenticationResponseDTO;

@SpringBootTest
@AutoConfigureMockMvc
public abstract class BaseTest {

    @Autowired
    protected MockMvc mockMvc;

    protected String authenticateAndGetToken() throws Exception {
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
