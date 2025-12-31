package rs.teslaris.core.unit;

import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.fasterxml.jackson.databind.ObjectMapper;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
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
import rs.teslaris.core.dto.document.MaterialProductDTO;
import rs.teslaris.core.dto.document.PersonDocumentContributionDTO;
import rs.teslaris.core.dto.person.PersonNameDTO;
import rs.teslaris.core.integration.BaseTest;
import rs.teslaris.core.model.document.DocumentContributionType;
import rs.teslaris.core.model.document.MaterialProductType;

@SpringBootTest
@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
public class MaterialProductControllerTest extends BaseTest {

    @Autowired
    private ObjectMapper objectMapper;


    private MaterialProductDTO getTestPayload() {
        var dummyMC = List.of(new MultilingualContentDTO(1, "EN", "Content", 1));

        var materialProductDTO = new MaterialProductDTO();
        materialProductDTO.setTitle(dummyMC);
        materialProductDTO.setSubTitle(dummyMC);
        materialProductDTO.setDescription(dummyMC);
        materialProductDTO.setKeywords(dummyMC);
        materialProductDTO.setDocumentDate("2014-10-24");
        materialProductDTO.setMaterialProductType(MaterialProductType.PROTOTYPE);
        materialProductDTO.setProductUsers(dummyMC);
        materialProductDTO.setResearchAreasId(Set.of(2));

        var contribution =
            new PersonDocumentContributionDTO(DocumentContributionType.AUTHOR, true, false, false,
                null, null);
        contribution.setOrderNumber(1);
        contribution.setPersonId(1);
        contribution.setContributionDescription(dummyMC);
        contribution.setDisplayAffiliationStatement(dummyMC);
        contribution.setPersonName(
            new PersonNameDTO(null, "Ime", "Srednje ime", "Prezime", null, null));
        materialProductDTO.setContributions(List.of(contribution));
        materialProductDTO.setUris(new HashSet<>());

        return materialProductDTO;
    }

    @Test
    @WithMockUser(username = "test.admin@test.com", password = "testAdmin")
    public void testReadMaterialProduct() throws Exception {
        mockMvc.perform(
            MockMvcRequestBuilders.get(
                    "http://localhost:8081/api/material-product/{materialProductId}", 19)
                .contentType(MediaType.APPLICATION_JSON)).andExpect(status().isOk());
    }

    @Test
    @WithMockUser(username = "test.admin@test.com", password = "testAdmin")
    public void testCreateMaterialProduct() throws Exception {
        String jwtToken = authenticateAdminAndGetToken();

        var materialProductDTO = getTestPayload();

        String requestBody = objectMapper.writeValueAsString(materialProductDTO);
        mockMvc.perform(MockMvcRequestBuilders.post("http://localhost:8081/api/material-product")
                .content(requestBody).contentType(MediaType.APPLICATION_JSON)
                .header(HttpHeaders.AUTHORIZATION, "Bearer " + jwtToken)
                .header("Idempotency-Key", "MOCK_KEY_SOFTWARE"))
            .andExpect(status().isCreated())
            .andExpect(jsonPath("$.documentDate").value("2014-10-24"));
    }

    @Test
    @WithMockUser(username = "test.admin@test.com", password = "testAdmin")
    public void testUpdateMaterialProduct() throws Exception {
        String jwtToken = authenticateAdminAndGetToken();

        var materialProductDTO = getTestPayload();
        materialProductDTO.setDoi("10.1109/tsmc.2014.2347277");

        String requestBody = objectMapper.writeValueAsString(materialProductDTO);
        mockMvc.perform(
                MockMvcRequestBuilders.put(
                        "http://localhost:8081/api/material-product/{materialProductId}", 19)
                    .content(requestBody).contentType(MediaType.APPLICATION_JSON)
                    .header(HttpHeaders.AUTHORIZATION, "Bearer " + jwtToken))
            .andExpect(status().isNoContent());
    }

    @Test
    @Order(Integer.MAX_VALUE)
    @WithMockUser(username = "test.admin@test.com", password = "testAdmin")
    @Transactional
    public void testDeleteMaterialProduct() throws Exception {
        String jwtToken = authenticateAdminAndGetToken();

        mockMvc.perform(
                MockMvcRequestBuilders.delete(
                        "http://localhost:8081/api/material-product/{materialProductId}",
                        19).contentType(MediaType.APPLICATION_JSON)
                    .header(HttpHeaders.AUTHORIZATION, "Bearer " + jwtToken))
            .andExpect(status().isNoContent());
    }
}
