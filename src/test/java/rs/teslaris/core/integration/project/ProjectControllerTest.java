package rs.teslaris.core.integration.project;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders;
import rs.teslaris.core.dto.commontypes.MonetaryAmountDTO;
import rs.teslaris.core.dto.commontypes.MultilingualContentDTO;
import rs.teslaris.core.integration.BaseTest;
import rs.teslaris.core.util.language.LanguageAbbreviations;
import rs.teslaris.project.dto.project.PersonProjectContributionDTO;
import rs.teslaris.project.dto.project.ProjectDTO;
import rs.teslaris.project.model.project.*;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
public class ProjectControllerTest extends BaseTest {

    @Autowired
    private ObjectMapper objectMapper;


    public static ProjectDTO getTestPayload() {
        var dto = new ProjectDTO();

        dto.setName(List.of(buildMultilingualContent("Test Project")));
        dto.setDescription(List.of(buildMultilingualContent("This is a test project description.")));
        dto.setNameAbbreviation(List.of(buildMultilingualContent("TFP")));
        dto.setKeywords(List.of(
                buildMultilingualContent("research", 1),
                buildMultilingualContent("innovation", 2)
        ));

        dto.setResearchAreasId(new HashSet<>(Set.of(1, 2)));
        dto.setConsortiumIds(new HashSet<>(Set.of(1, 2)));

        dto.setDateFrom(LocalDate.of(2025, 1, 1));
        dto.setDateTo(LocalDate.of(2026, 3, 1));

        dto.setUris(new HashSet<>(Set.of(
                "https://example.com/project",
                "https://example.com/guidelines"
        )));

        dto.setStatus(ProjectStatus.SUBMITTED);
        dto.setCollaborationType(ProjectCollaborationType.INTERNATIONAL_BILATERAL);
        dto.setResearchType(ProjectResearchType.INNOVATION);
        dto.setNotFunded(true);
        dto.setCosts(new MonetaryAmountDTO(1, 50000));

        dto.setTeam(List.of(buildTeamMember(
                1, 1,
                PersonProjectContributionType.TEAM_MEMBER,
                PersonProjectInvestigationRole.RESEARCHER,
                "Lead researcher",
                "University of Novi Sad"
        )));

        return dto;
    }

    private static MultilingualContentDTO buildMultilingualContent(String content) {
        return buildMultilingualContent(content, 1);
    }

    private static MultilingualContentDTO buildMultilingualContent(String content, int priority) {
        var mlc = new MultilingualContentDTO();
        mlc.setLanguageTagId(1);
        mlc.setLanguageTag(LanguageAbbreviations.ENGLISH);
        mlc.setContent(content);
        mlc.setPriority(priority);
        return mlc;
    }

    private static PersonProjectContributionDTO buildTeamMember(
            Integer personId,
            Integer orderNumber,
            PersonProjectContributionType contributionType,
            PersonProjectInvestigationRole investigationRole,
            String contributionDescription,
            String affiliation) {

        var member = new PersonProjectContributionDTO();
        member.setPersonId(personId);
        member.setOrderNumber(orderNumber);
        member.setContributionType(contributionType);
        member.setInvestigationRole(investigationRole);
        member.setContributionDescription(List.of(buildMultilingualContent(contributionDescription)));
        member.setDisplayAffiliationStatement(List.of(buildMultilingualContent(affiliation)));
        member.setInstitutionIds(List.of(1));
        return member;
    }

    @Test
    @WithMockUser(username = "test.admin@test.com", password = "testAdmin")
    public void testSearchProjects() throws Exception {
        String jwtToken = authenticateAdminAndGetToken();

        mockMvc.perform(
                        MockMvcRequestBuilders.get(
                                "http://localhost:8081/api/project/search?tokens=Test*&dateFrom=2026-03-01&dateTo=2027-04-30")
                                .contentType(MediaType.APPLICATION_JSON)
                                .header(HttpHeaders.AUTHORIZATION, "Bearer " + jwtToken))
                .andExpect(status().isOk());
    }

    @Test
    @WithMockUser(username = "test.admin@test.com", password = "testAdmin")
    public void testReadProject() throws Exception {
        String jwtToken = authenticateAdminAndGetToken();

        mockMvc.perform(
                        MockMvcRequestBuilders.get(
                                        "http://localhost:8081/api/project/{projectId}", 1)
                                .contentType(MediaType.APPLICATION_JSON)
                                .header(HttpHeaders.AUTHORIZATION, "Bearer " + jwtToken))
                .andExpect(status().isOk());
    }

    @Test
    @WithMockUser(username = "test.admin@test.com", password = "testAdmin")
    public void testCreateProject() throws Exception {
        String jwtToken = authenticateAdminAndGetToken();

        var geneticMaterialDTO = getTestPayload();

        String requestBody = objectMapper.writeValueAsString(geneticMaterialDTO);
        mockMvc.perform(MockMvcRequestBuilders.post("http://localhost:8081/api/project")
                        .content(requestBody).contentType(MediaType.APPLICATION_JSON)
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + jwtToken)
                        .header("Idempotency-Key", "MOCK_KEY_PROJECT"))
                .andExpect(status().isCreated());
    }

    @Test
    @WithMockUser(username = "test.admin@test.com", password = "testAdmin")
    public void testUpdateProject() throws Exception {
        String jwtToken = authenticateAdminAndGetToken();

        var geneticMaterialDTO = getTestPayload();

        String requestBody = objectMapper.writeValueAsString(geneticMaterialDTO);
        mockMvc.perform(MockMvcRequestBuilders.put(
                                "http://localhost:8081/api/project/{project}", 1)
                        .content(requestBody).contentType(MediaType.APPLICATION_JSON)
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + jwtToken))
                .andExpect(status().isNoContent());
    }

    @Test
    @WithMockUser(username = "test.admin@test.com", password = "testAdmin")
    public void testUpdateProjectAddingTeamMember() throws Exception {
        String jwtToken = authenticateAdminAndGetToken();

        var payload = getTestPayload();
        var secondMember = buildTeamMember(
                2, 2,
                PersonProjectContributionType.PRINCIPLE_INVESTIGATOR,
                PersonProjectInvestigationRole.SUPERVISOR,
                "Project supervisor",
                "Faculty of Technical Sciences"
        );
        payload.setTeam(List.of(payload.getTeam().getFirst(), secondMember));

        String requestBody = objectMapper.writeValueAsString(payload);
        mockMvc.perform(MockMvcRequestBuilders.put(
                                "http://localhost:8081/api/project/{projectId}", 1)
                        .content(requestBody)
                        .contentType(MediaType.APPLICATION_JSON)
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + jwtToken))
                .andExpect(status().isNoContent());
    }

    @Test
    @WithMockUser(username = "test.admin@test.com", password = "testAdmin")
    public void testUpdateProjectRemovingTeamMembers() throws Exception {
        String jwtToken = authenticateAdminAndGetToken();

        var payload = getTestPayload();
        payload.setTeam(List.of());

        String requestBody = objectMapper.writeValueAsString(payload);
        mockMvc.perform(MockMvcRequestBuilders.put(
                                "http://localhost:8081/api/project/{projectId}", 1)
                        .content(requestBody)
                        .contentType(MediaType.APPLICATION_JSON)
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + jwtToken))
                .andExpect(status().isNoContent());
    }

    @Test
    @WithMockUser(username = "test.admin@test.com", password = "testAdmin")
    public void testDeleteProject() throws Exception {
        String jwtToken = authenticateAdminAndGetToken();

        // project id set to 3 to avoid dependencies between test cases
        mockMvc.perform(MockMvcRequestBuilders.delete(
                                    "http://localhost:8081/api/project/{projectId}", 3)
                        .contentType(MediaType.APPLICATION_JSON)
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + jwtToken))
                .andExpect(status().isNoContent());
    }

}
