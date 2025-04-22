package rs.teslaris.core.integration;

import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.cookie;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders;

@SpringBootTest
public class CookieControllerTest extends BaseTest {

    @Test
    public void testOptFor() throws Exception {
        mockMvc.perform(
                MockMvcRequestBuilders.patch("http://localhost:8081/api/cookie?optOut=false")
                    .contentType(MediaType.APPLICATION_JSON)).andExpect(status().isNoContent())
            .andExpect(cookie().value("tracking_opt_out", "false"));
    }

    @Test
    public void testOptOut() throws Exception {
        mockMvc.perform(
                MockMvcRequestBuilders.patch("http://localhost:8081/api/cookie?optOut=true")
                    .contentType(MediaType.APPLICATION_JSON)).andExpect(status().isNoContent())
            .andExpect(cookie().value("tracking_opt_out", "true"));
    }
}
