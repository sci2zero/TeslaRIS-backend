package rs.teslaris.core.integration;

import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
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
import rs.teslaris.core.dto.commontypes.GeoLocationDTO;
import rs.teslaris.core.dto.commontypes.MultilingualContentDTO;
import rs.teslaris.core.dto.person.PostalAddressDTO;

@SpringBootTest
public class BrandingInformationControllerTest extends BaseTest {

    @Autowired
    private ObjectMapper objectMapper;

    private BrandingInformationDTO getTestPayload() {
        var dummyMC = List.of(new MultilingualContentDTO(1, "EN", "Dummy MC", 1));
        return new BrandingInformationDTO(dummyMC, dummyMC, null, null, null);
    }

    private BrandingInformationDTO getTestPayloadWithAddress() {
        var dummyMC = List.of(new MultilingualContentDTO(1, "EN", "Dummy MC", 1));

        var postalAddress = new PostalAddressDTO();
        postalAddress.setStreetAndNumber(dummyMC);
        postalAddress.setCity(dummyMC);
        postalAddress.setState(dummyMC);
        postalAddress.setPostalNumber("21000");

        return new BrandingInformationDTO(dummyMC, dummyMC,
            new GeoLocationDTO(19.8335, 45.2671, null), postalAddress, "+381 21 000 000");
    }

    @Test
    public void testReadBrandingInformation() throws Exception {
        mockMvc.perform(
                MockMvcRequestBuilders.get("http://localhost:8081/api/branding")
                    .contentType(MediaType.APPLICATION_JSON))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.title").exists())
            .andExpect(jsonPath("$.description").exists());
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

    @Test
    @WithMockUser(username = "test.admin@test.com", password = "testAdmin")
    public void testUpdateBrandingInformationWithAddressAndLocation() throws Exception {
        String jwtToken = authenticateAdminAndGetToken();

        String requestBody = objectMapper.writeValueAsString(getTestPayloadWithAddress());
        mockMvc.perform(MockMvcRequestBuilders.put("http://localhost:8081/api/branding")
                .content(requestBody).contentType(MediaType.APPLICATION_JSON)
                .header(HttpHeaders.AUTHORIZATION, "Bearer " + jwtToken))
            .andExpect(status().isNoContent());
    }
}
