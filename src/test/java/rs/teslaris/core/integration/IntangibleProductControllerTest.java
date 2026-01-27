package rs.teslaris.core.integration;

import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.fasterxml.jackson.databind.ObjectMapper;
import java.util.HashSet;
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
import org.springframework.transaction.annotation.Transactional;
import rs.teslaris.core.dto.commontypes.MultilingualContentDTO;
import rs.teslaris.core.dto.document.IntangibleProductDTO;
import rs.teslaris.core.dto.document.PersonDocumentContributionDTO;
import rs.teslaris.core.dto.person.PersonNameDTO;
import rs.teslaris.core.model.document.DocumentContributionType;
import rs.teslaris.core.model.document.IntangibleProductType;

@SpringBootTest
@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
public class IntangibleProductControllerTest extends BaseTest {

    @Autowired
    private ObjectMapper objectMapper;

    private IntangibleProductDTO getTestPayload() {
        var dummyMC = List.of(new MultilingualContentDTO(1, "EN", "Content", 1));

        var intangibleProductDTO = new IntangibleProductDTO();
        intangibleProductDTO.setTitle(dummyMC);
        intangibleProductDTO.setSubTitle(dummyMC);
        intangibleProductDTO.setDescription(dummyMC);
        intangibleProductDTO.setKeywords(dummyMC);
        intangibleProductDTO.setDocumentDate("2004-11-06");
        intangibleProductDTO.setIntangibleProductType(IntangibleProductType.SOFTWARE);

        var contribution =
            new PersonDocumentContributionDTO(DocumentContributionType.AUTHOR, true, false, false,
                null, null);
        contribution.setOrderNumber(1);
        contribution.setPersonId(1);
        contribution.setContributionDescription(dummyMC);
        contribution.setDisplayAffiliationStatement(dummyMC);
        contribution.setPersonName(
            new PersonNameDTO(null, "Ime", "Srednje ime", "Prezime", null, null));
        intangibleProductDTO.setContributions(List.of(contribution));
        intangibleProductDTO.setUris(new HashSet<>());

        return intangibleProductDTO;
    }

    @Test
    @WithMockUser(username = "test.admin@test.com", password = "testAdmin")
    public void testReadIntangibleProduct() throws Exception {
        mockMvc.perform(
            MockMvcRequestBuilders.get(
                    "http://localhost:8081/api/intangible-product/{intangibleProductId}", 7)
                .contentType(MediaType.APPLICATION_JSON)).andExpect(status().isOk());
    }

    @Test
    @WithMockUser(username = "test.admin@test.com", password = "testAdmin")
    public void testCreateIntangibleProduct() throws Exception {
        String jwtToken = authenticateAdminAndGetToken();

        var intangibleProductDTO = getTestPayload();

        String requestBody = objectMapper.writeValueAsString(intangibleProductDTO);
        mockMvc.perform(MockMvcRequestBuilders.post("http://localhost:8081/api/intangible-product")
                .content(requestBody).contentType(MediaType.APPLICATION_JSON)
                .header(HttpHeaders.AUTHORIZATION, "Bearer " + jwtToken)
                .header("Idempotency-Key", "MOCK_KEY_INTANGIBLE"))
            .andExpect(status().isCreated())
            .andExpect(jsonPath("$.documentDate").value("2004-11-06"));
    }

    @Test
    @WithMockUser(username = "test.admin@test.com", password = "testAdmin")
    public void testUpdateIntangibleProduct() throws Exception {
        String jwtToken = authenticateAdminAndGetToken();

        var intangibleProductDTO = getTestPayload();
        intangibleProductDTO.setDoi("10.1109/tsmc.2014.2347265");

        String requestBody = objectMapper.writeValueAsString(intangibleProductDTO);
        mockMvc.perform(
                MockMvcRequestBuilders.put(
                        "http://localhost:8081/api/intangible-product/{intangibleProductId}", 7)
                    .content(requestBody).contentType(MediaType.APPLICATION_JSON)
                    .header(HttpHeaders.AUTHORIZATION, "Bearer " + jwtToken))
            .andExpect(status().isNoContent());
    }

    @Test
    @Order(Integer.MAX_VALUE)
    @WithMockUser(username = "test.admin@test.com", password = "testAdmin")
    @Transactional
    public void testDeleteIntangibleProduct() throws Exception {
        String jwtToken = authenticateAdminAndGetToken();

        mockMvc.perform(
                MockMvcRequestBuilders.delete(
                        "http://localhost:8081/api/intangible-product/{intangibleProductId}",
                        7).contentType(MediaType.APPLICATION_JSON)
                    .header(HttpHeaders.AUTHORIZATION, "Bearer " + jwtToken))
            .andExpect(status().isNoContent());
    }
}
