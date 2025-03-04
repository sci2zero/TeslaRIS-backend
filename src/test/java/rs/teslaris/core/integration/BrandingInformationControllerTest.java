package rs.teslaris.core.integration;

import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.fasterxml.jackson.databind.ObjectMapper;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders;
import rs.teslaris.core.dto.commontypes.BrandingInformationDTO;
import rs.teslaris.core.dto.commontypes.MultilingualContentDTO;

@SpringBootTest
public class BrandingInformationControllerTest extends BaseTest {

    @Autowired
    private ObjectMapper objectMapper;

    private BrandingInformationDTO getTestPayload() {
        var dummyMC = List.of(new MultilingualContentDTO(1, "EN", "Dummy MC", 1));
        return new BrandingInformationDTO(dummyMC, dummyMC);
    }

    @Test
    public void testReadBrandingInformation() throws Exception {
        mockMvc.perform(
            MockMvcRequestBuilders.get("http://localhost:8081/api/branding")
                .contentType(MediaType.APPLICATION_JSON)).andExpect(status().isOk());
    }

    @Test
    @WithMockUser(username = "test.admin@test.com", password = "testAdmin")
    public void testUpdateBrandingInformation() throws Exception {
        String jwtToken = authenticateAdminAndGetToken();

        var brandingInfoDTO = getTestPayload();

        String requestBody = objectMapper.writeValueAsString(brandingInfoDTO);
        mockMvc.perform(MockMvcRequestBuilders.put("http://localhost:8081/api/branding")
                .content(requestBody).contentType(MediaType.APPLICATION_JSON)
                .header(HttpHeaders.AUTHORIZATION, "Bearer " + jwtToken))
            .andExpect(status().isNoContent());
    }
}
