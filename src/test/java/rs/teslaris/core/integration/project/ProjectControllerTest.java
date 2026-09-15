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
import rs.teslaris.project.dto.project.OrganisationUnitProjectContributionDTO;
import rs.teslaris.project.dto.project.PersonProjectContributionDTO;
import rs.teslaris.project.dto.project.ProjectDTO;
import rs.teslaris.project.dto.project.ProjectsRelationDTO;
import rs.teslaris.project.model.project.*;

import java.time.LocalDate;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
public class ProjectControllerTest extends BaseTest {

    @Autowired
    private ObjectMapper objectMapper;


    public static ProjectDTO getTestPayload() {
        var dto = new ProjectDTO();

        dto.setName(List.of(buildMultilingualContent("Test Project")));
        dto.setDescription(
            List.of(buildMultilingualContent("This is a test project description.")));
        dto.setNameAbbreviation(List.of(buildMultilingualContent("TFP")));
        dto.setKeywords(List.of(
            buildMultilingualContent("research", 1),
            buildMultilingualContent("innovation", 2)
        ));

        dto.setResearchAreasId(new HashSet<>(Set.of(1, 2)));

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

        dto.setPersons(List.of(buildPerson(
                1, 1,
                PersonProjectContributionType.TEAM_MEMBER,
                PersonProjectInvestigationRole.RESEARCHER,
                "Lead researcher",
                "University of Novi Sad"
        )));

        dto.setOrganisations(List.of(buildOrganisation(
                1, 1,
                OrganisationUnitProjectContributionType.COORDINATOR,
                "Coordinating institution"
        )));

        dto.setRelations(List.of(buildRelation(
                2,
                ProjectsRelationType.PART_OF,
                "This project is part of the parent project",
                "Parent project"
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

    private static PersonProjectContributionDTO buildPerson(
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
        member.setContributionDescription(
            List.of(buildMultilingualContent(contributionDescription)));
        member.setDisplayAffiliationStatement(List.of(buildMultilingualContent(affiliation)));
        member.setInstitutionIds(List.of(1));

        member.setFavorite(true);
        member.setKeywords(List.of(buildMultilingualContent("machine learning")));
        member.setDateFrom(LocalDate.of(2025, 1, 1));
        member.setDateTo(LocalDate.of(2026, 3, 1));
        member.setUris(Set.of("https://example.com/contribution-proof"));
        member.setIsMainContributor(true);
        member.setIsInvitedContributor(false);
        member.setDisplayProject(List.of(buildMultilingualContent("Test Project Display")));

        return member;
    }

    private static OrganisationUnitProjectContributionDTO buildOrganisation(
            Integer organisationUnitId,
            Integer orderNumber,
            OrganisationUnitProjectContributionType contributionType,
            String contributionDescription) {

        var member = new OrganisationUnitProjectContributionDTO();
        member.setOrganisationUnitId(organisationUnitId);
        member.setOrderNumber(orderNumber);
        member.setContributionType(contributionType);
        member.setContributionDescription(
                List.of(buildMultilingualContent(contributionDescription)));
        member.setDateFrom(LocalDate.of(2025, 1, 1));
        member.setDateTo(LocalDate.of(2026, 3, 1));
        member.setUris(Set.of("https://example.com/consortium-proof"));
        member.setIsMainContributor(true);
        member.setFavorite(false);
        member.setContactPersonId(1);
        member.setDisplayProject(List.of(buildMultilingualContent("Test Project Display")));

        return member;
    }

    private static ProjectsRelationDTO buildRelation(
            Integer targetProjectId,
            ProjectsRelationType relationType,
            String sourceDescription,
            String targetDescription) {

        var relation = new ProjectsRelationDTO();
        relation.setTargetProjectId(targetProjectId);
        relation.setRelationType(relationType);
        relation.setDateFrom(LocalDate.of(2025, 1, 1));
        relation.setDateTo(LocalDate.of(2026, 3, 1));
        relation.setSourceProjectDescription(List.of(buildMultilingualContent(sourceDescription)));
        relation.setTargetProjectDescription(List.of(buildMultilingualContent(targetDescription)));
        return relation;
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
        var secondMember = buildPerson(
            2, 2,
            PersonProjectContributionType.PRINCIPLE_INVESTIGATOR,
            PersonProjectInvestigationRole.SUPERVISOR,
            "Project supervisor",
            "Faculty of Technical Sciences"
        );
        payload.setPersons(List.of(payload.getPersons().getFirst(), secondMember));

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
        payload.setPersons(List.of());

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

    @Test
    @WithMockUser(username = "test.admin@test.com", password = "testAdmin")
    public void testAddProjectPerson() throws Exception {
        String jwtToken = authenticateAdminAndGetToken();

        var payload = buildPerson(
                2, 3,
                PersonProjectContributionType.PRINCIPLE_INVESTIGATOR,
                PersonProjectInvestigationRole.SUPERVISOR,
                "Added via add-person endpoint",
                "Faculty of Technical Sciences");

        mockMvc.perform(MockMvcRequestBuilders.post(
                                "http://localhost:8081/api/project/{projectId}/add-person", 1)
                        .content(objectMapper.writeValueAsString(payload))
                        .contentType(MediaType.APPLICATION_JSON)
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + jwtToken)
                        .header("Idempotency-Key", "MOCK_KEY_PROJECT_ADD_PERSON"))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id").isNumber())
                .andExpect(jsonPath("$.personId").value(2))
                .andExpect(jsonPath("$.contributionType").value("PRINCIPLE_INVESTIGATOR"))
                .andExpect(jsonPath("$.investigationRole").value("SUPERVISOR"));
    }

    @Test
    @WithMockUser(username = "test.admin@test.com", password = "testAdmin")
    public void testRemoveProjectPerson() throws Exception {
        String jwtToken = authenticateAdminAndGetToken();

        var payload = buildPerson(
                1, 4,
                PersonProjectContributionType.TEAM_MEMBER,
                PersonProjectInvestigationRole.RESEARCHER,
                "To be removed",
                "University of Novi Sad");

        var addResponse = mockMvc.perform(MockMvcRequestBuilders.post(
                        "http://localhost:8081/api/project/{projectId}/add-person", 1)
                        .content(objectMapper.writeValueAsString(payload))
                        .contentType(MediaType.APPLICATION_JSON)
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + jwtToken)
                        .header("Idempotency-Key", "MOCK_KEY_PROJECT_REMOVE_PERSON"))
                .andReturn().getResponse().getContentAsString();

        var contributionId = objectMapper.readTree(addResponse).get("id").asInt();

        mockMvc.perform(MockMvcRequestBuilders.delete(
                                "http://localhost:8081/api/project/{projectId}/remove-person/{personContributionId}",
                                1, contributionId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + jwtToken))
                .andExpect(status().isNoContent());
    }

    @Test
    @WithMockUser(username = "test.admin@test.com", password = "testAdmin")
    public void testAddProjectOrganisation() throws Exception {
        String jwtToken = authenticateAdminAndGetToken();

        var payload = buildOrganisation(
                1, 1, OrganisationUnitProjectContributionType.PARTNER, "Partner institution");

        mockMvc.perform(MockMvcRequestBuilders.post(
                                "http://localhost:8081/api/project/{projectId}/add-organisation", 1)
                        .content(objectMapper.writeValueAsString(payload))
                        .contentType(MediaType.APPLICATION_JSON)
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + jwtToken)
                        .header("Idempotency-Key", "MOCK_KEY_PROJECT_ADD_ORGANISATION"))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id").isNumber())
                .andExpect(jsonPath("$.organisationUnitId").value(1))
                .andExpect(jsonPath("$.contributionType").value("PARTNER"))
                .andExpect(jsonPath("$.orderNumber").value(1))
                .andExpect(jsonPath("$.contactPersonId").value(1));
    }

    @Test
    @WithMockUser(username = "test.admin@test.com", password = "testAdmin")
    public void testRemoveProjectOrganisation() throws Exception {
        String jwtToken = authenticateAdminAndGetToken();

        var payload = buildOrganisation(
                1, 2, OrganisationUnitProjectContributionType.CONSORTIUM_MEMBER, "To be removed");

        var addResponse = mockMvc.perform(MockMvcRequestBuilders.post(
                                "http://localhost:8081/api/project/{projectId}/add-organisation", 1)
                        .content(objectMapper.writeValueAsString(payload))
                        .contentType(MediaType.APPLICATION_JSON)
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + jwtToken)
                        .header("Idempotency-Key", "MOCK_KEY_PROJECT_REMOVE_ORGANISATION"))
                .andReturn().getResponse().getContentAsString();

        var contributionId = objectMapper.readTree(addResponse).get("id").asInt();

        mockMvc.perform(MockMvcRequestBuilders.delete(
                                "http://localhost:8081/api/project/{projectId}/remove-organisation/{organisationContributionId}",
                                1, contributionId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + jwtToken))
                .andExpect(status().isNoContent());
    }

    @Test
    @WithMockUser(username = "test.admin@test.com", password = "testAdmin")
    public void testAddProjectRelation() throws Exception {
        String jwtToken = authenticateAdminAndGetToken();

        var payload = buildRelation(
                2, ProjectsRelationType.PREDECESSOR,
                "Source project description", "Target project description");

        mockMvc.perform(MockMvcRequestBuilders.post(
                                "http://localhost:8081/api/project/{projectId}/add-relation", 1)
                        .content(objectMapper.writeValueAsString(payload))
                        .contentType(MediaType.APPLICATION_JSON)
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + jwtToken)
                        .header("Idempotency-Key", "MOCK_KEY_PROJECT_ADD_RELATION"))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id").isNumber())
                .andExpect(jsonPath("$.sourceProjectId").value(1))
                .andExpect(jsonPath("$.targetProjectId").value(2))
                .andExpect(jsonPath("$.relationType").value("PREDECESSOR"));
    }

    @Test
    @WithMockUser(username = "test.admin@test.com", password = "testAdmin")
    public void testRemoveProjectRelation() throws Exception {
        String jwtToken = authenticateAdminAndGetToken();

        var payload = buildRelation(
                2, ProjectsRelationType.PART_OF, "To be removed", "Target of removed relation");

        var addResponse = mockMvc.perform(MockMvcRequestBuilders.post(
                                "http://localhost:8081/api/project/{projectId}/add-relation", 1)
                        .content(objectMapper.writeValueAsString(payload))
                        .contentType(MediaType.APPLICATION_JSON)
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + jwtToken)
                        .header("Idempotency-Key", "MOCK_KEY_PROJECT_REMOVE_RELATION"))
                .andReturn().getResponse().getContentAsString();

        var relationId = objectMapper.readTree(addResponse).get("id").asInt();

        mockMvc.perform(MockMvcRequestBuilders.delete(
                                "http://localhost:8081/api/project/{projectId}/remove-relation/{relationId}",
                                1, relationId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + jwtToken))
                .andExpect(status().isNoContent());
    }

}
