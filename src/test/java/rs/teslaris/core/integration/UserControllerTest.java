package rs.teslaris.core.integration;

import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders;
import rs.teslaris.core.dto.user.ForgotPasswordRequestDTO;
import rs.teslaris.core.dto.user.ResetPasswordRequestDTO;

@SpringBootTest
public class UserControllerTest extends BaseTest {

    @Autowired
    private ObjectMapper objectMapper;

    @Test
    public void testForgotPassword() throws Exception {
        var forgotPasswordRequest = new ForgotPasswordRequestDTO("author@author.com");
        String requestBody = objectMapper.writeValueAsString(forgotPasswordRequest);
        mockMvc.perform(
                MockMvcRequestBuilders.post("http://localhost:8081/api/user/forgot-password")
                    .content(requestBody)
                    .contentType(MediaType.APPLICATION_JSON)
                    .header("Idempotency-Key", "MOCK_KEY_USER_1"))
            .andExpect(status().isCreated());
    }

    @Test
    public void testForgotPasswordUserDoesNotExist() throws Exception {
        var forgotPasswordRequest = new ForgotPasswordRequestDTO("non.existing@author.com");
        String requestBody = objectMapper.writeValueAsString(forgotPasswordRequest);
        mockMvc.perform(
                MockMvcRequestBuilders.post("http://localhost:8081/api/user/forgot-password")
                    .content(requestBody)
                    .contentType(MediaType.APPLICATION_JSON)
                    .header("Idempotency-Key", "MOCK_KEY_USER_2"))
            .andExpect(status().isCreated()); // No information disclosure
    }

    @Test
    public void testResetPassword() throws Exception {
        var resetPasswordRequest = new ResetPasswordRequestDTO("TOKEN", "newPassword");
        String requestBody = objectMapper.writeValueAsString(resetPasswordRequest);
        mockMvc.perform(
                MockMvcRequestBuilders.patch("http://localhost:8081/api/user/reset-password")
                    .content(requestBody)
                    .contentType(MediaType.APPLICATION_JSON)
                    .header("Idempotency-Key", "MOCK_KEY_USER_3"))
            .andExpect(status().isOk());
    }

    @Test
    public void testResetPasswordWrongToken() throws Exception {
        var resetPasswordRequest = new ResetPasswordRequestDTO("WRONG_TOKEN", "newPassword");
        String requestBody = objectMapper.writeValueAsString(resetPasswordRequest);
        mockMvc.perform(
                MockMvcRequestBuilders.patch("http://localhost:8081/api/user/reset-password")
                    .content(requestBody)
                    .contentType(MediaType.APPLICATION_JSON)
                    .header("Idempotency-Key", "MOCK_KEY_USER_4"))
            .andExpect(status().isNotFound());
    }
}
