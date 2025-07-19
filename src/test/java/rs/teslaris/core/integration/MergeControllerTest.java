package rs.teslaris.core.integration;

import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders;

@SpringBootTest
public class MergeControllerTest extends BaseTest {

    @Autowired
    private ObjectMapper objectMapper;

    @Test
    @WithMockUser(username = "test.admin@test.com", password = "testAdmin")
    public void testSwitchJournalPublicationToOtherJournalWhenPublicationDoesNotExist()
        throws Exception {
        String jwtToken = authenticateAdminAndGetToken();

        mockMvc.perform(
            MockMvcRequestBuilders.patch(
                    "http://localhost:8081/api/merge/journal/{targetJournalId}/publication/{publicationId}",
                    1, 999)
                .header(HttpHeaders.AUTHORIZATION, "Bearer " + jwtToken)
                .contentType(MediaType.APPLICATION_JSON)).andExpect(status().isNotFound());
    }

    @Test
    @WithMockUser(username = "test.admin@test.com", password = "testAdmin")
    public void testSwitchPublisherPublicationToOtherJournalWhenPublisherDoesNotExist()
        throws Exception {
        String jwtToken = authenticateAdminAndGetToken();

        mockMvc.perform(
            MockMvcRequestBuilders.patch(
                    "http://localhost:8081/api/merge/publisher/{targetPublisherId}/publication/{publicationId}",
                    1, 999)
                .header(HttpHeaders.AUTHORIZATION, "Bearer " + jwtToken)
                .contentType(MediaType.APPLICATION_JSON)).andExpect(status().isNotFound());
    }

    @Test
    @WithMockUser(username = "test.admin@test.com", password = "testAdmin")
    public void testSwitchProceedingsPublicationToOtherProceedingsWhenPublicationDoesNotExist()
        throws Exception {
        String jwtToken = authenticateAdminAndGetToken();

        mockMvc.perform(
            MockMvcRequestBuilders.patch(
                    "http://localhost:8081/api/merge/proceedings/{sourceProceedingsId}/publication/{publicationId}",
                    1, 999)
                .header(HttpHeaders.AUTHORIZATION, "Bearer " + jwtToken)
                .contentType(MediaType.APPLICATION_JSON)).andExpect(status().isNotFound());
    }

    @Test
    @WithMockUser(username = "test.admin@test.com", password = "testAdmin")
    public void testMigratePersonIdentifierHistory()
        throws Exception {
        String jwtToken = authenticateAdminAndGetToken();

        mockMvc.perform(
            MockMvcRequestBuilders.patch(
                    "http://localhost:8081/api/merge/migrate-identifier-history/person/{sourcePersonId}/{targetPersonId}",
                    2, 1)
                .header(HttpHeaders.AUTHORIZATION, "Bearer " + jwtToken)
                .contentType(MediaType.APPLICATION_JSON)).andExpect(status().isNoContent());
    }

    @Test
    @WithMockUser(username = "test.admin@test.com", password = "testAdmin")
    public void testMigrateOrganisationUnitIdentifierHistory()
        throws Exception {
        String jwtToken = authenticateAdminAndGetToken();

        mockMvc.perform(
            MockMvcRequestBuilders.patch(
                    "http://localhost:8081/api/merge/migrate-identifier-history/organisation-unit/{leftOrganisationUnitId}/{rightOrganisationUnitId}",
                    2, 1)
                .header(HttpHeaders.AUTHORIZATION, "Bearer " + jwtToken)
                .contentType(MediaType.APPLICATION_JSON)).andExpect(status().isNoContent());
    }

    @ParameterizedTest
    @ValueSource(strings = {"monograph", "proceedings"})
    @WithMockUser(username = "test.admin@test.com", password = "testAdmin")
    public void testMigratePublicationIdentifierHistory(String publicationType)
        throws Exception {
        String jwtToken = authenticateAdminAndGetToken();

        var deletedId = 5;
        var mergedId = 6;
        if (publicationType.equals("proceedings")) {
            deletedId = 2;
            mergedId = 1;
        }

        mockMvc.perform(
            MockMvcRequestBuilders.patch(
                    "http://localhost:8081/api/merge/migrate-identifier-history/publication/{publicationType}/{leftDocumentId}/{rightDocumentId}",
                    publicationType, deletedId, mergedId)
                .header(HttpHeaders.AUTHORIZATION, "Bearer " + jwtToken)
                .contentType(MediaType.APPLICATION_JSON)).andExpect(status().isNoContent());
    }
}
