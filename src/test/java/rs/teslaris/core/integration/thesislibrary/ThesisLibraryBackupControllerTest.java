package rs.teslaris.core.integration.thesislibrary;

import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders;
import rs.teslaris.core.integration.BaseTest;

@SpringBootTest
public class ThesisLibraryBackupControllerTest extends BaseTest {

    @Test
    @WithMockUser(username = "test.admin@test.com", password = "testAdmin")
    public void testGenerateBackup() throws Exception {
        String jwtToken = authenticateHeadOfLibraryAndGetToken();

        mockMvc.perform(
                MockMvcRequestBuilders.post(
                        "http://localhost:8081/api/thesis-library/backup/schedule-generation?from=2022-03-03&to=2023-04-04&institutionId=1&types=PHD&sections=FILE_ITEMS&defended=true&putOnReview=true&lang=sr&metadataFormat=CSV")
                    .contentType(MediaType.APPLICATION_JSON)
                    .header(HttpHeaders.AUTHORIZATION, "Bearer " + jwtToken))
            .andExpect(status().isAccepted());
    }

    @Test
    @WithMockUser(username = "test.admin@test.com", password = "testAdmin")
    public void testListBackups() throws Exception {
        String jwtToken = authenticateHeadOfLibraryAndGetToken();

        mockMvc.perform(
                MockMvcRequestBuilders.get(
                        "http://localhost:8081/api/thesis-library/backup/list-backups")
                    .contentType(MediaType.APPLICATION_JSON)
                    .header(HttpHeaders.AUTHORIZATION, "Bearer " + jwtToken))
            .andExpect(status().isOk());
    }
}
