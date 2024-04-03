package rs.teslaris.core.integration;

import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders;

@SpringBootTest
public class OrganisationUnitsRelationControllerTest extends BaseTest {

    @Test
    public void testGetOUChain() throws Exception {
        mockMvc.perform(MockMvcRequestBuilders.get(
                    "http://localhost:8081/api/organisation-unit-relation/{leafId}", 27)
                .contentType(MediaType.APPLICATION_JSON))
            .andExpect(status().isOk());
    }
}
