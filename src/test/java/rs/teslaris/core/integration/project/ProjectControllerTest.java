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

import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
public class ProjectControllerTest extends BaseTest {

    @Autowired
    private ObjectMapper objectMapper;


    public static ProjectDTO getTestPayload() {
        var dto = new ProjectDTO();

        var nameList = new ArrayList<MultilingualContentDTO>();
        MultilingualContentDTO englishName = new MultilingualContentDTO();
        englishName.setLanguageTagId(1);
        englishName.setLanguageTag(LanguageAbbreviations.ENGLISH);
        englishName.setContent("Test Project");
        englishName.setPriority(1);
        nameList.add(englishName);
        dto.setName(nameList);

        var descriptionList = new ArrayList<MultilingualContentDTO>();
        MultilingualContentDTO englishDesc = new MultilingualContentDTO();
        englishDesc.setLanguageTagId(1);
        englishDesc.setLanguageTag(LanguageAbbreviations.ENGLISH);
        englishDesc.setContent("This is a test project description.");
        englishDesc.setPriority(1);
        descriptionList.add(englishDesc);
        dto.setDescription(descriptionList);

        var abbreviationList = new ArrayList<MultilingualContentDTO>();
        MultilingualContentDTO englishAbbr = new MultilingualContentDTO();
        englishAbbr.setLanguageTagId(1);
        englishAbbr.setLanguageTag(LanguageAbbreviations.ENGLISH);
        englishAbbr.setContent("TFP");
        englishAbbr.setPriority(1);
        abbreviationList.add(englishAbbr);
        dto.setNameAbbreviation(abbreviationList);

        var keywordsList = new ArrayList<MultilingualContentDTO>();
        MultilingualContentDTO keyword1 = new MultilingualContentDTO();
        keyword1.setLanguageTagId(1);
        keyword1.setLanguageTag(LanguageAbbreviations.ENGLISH);
        keyword1.setContent("research");
        keyword1.setPriority(1);
        keywordsList.add(keyword1);

        var keyword2 = new MultilingualContentDTO();
        keyword2.setLanguageTagId(1);
        keyword2.setLanguageTag(LanguageAbbreviations.ENGLISH);
        keyword2.setContent("innovation");
        keyword2.setPriority(2);
        keywordsList.add(keyword2);
        dto.setKeywords(keywordsList);

        var researchAreas = new HashSet<Integer>();
        researchAreas.add(1);
        researchAreas.add(2);
        dto.setResearchAreasId(researchAreas);

        var consortium = new HashSet<Integer>();
        consortium.add(1);
        consortium.add(2);
        dto.setConsortiumIds(consortium);

        dto.setDateFrom(LocalDate.of(2025, 1, 1));
        dto.setDateTo(LocalDate.of(2026, 3, 1));

        var uris = new HashSet<String>();
        uris.add("https://example.com/project");
        uris.add("https://example.com/guidelines");
        dto.setUris(uris);

        dto.setStatus(ProjectStatus.SUBMITTED);
        dto.setCollaborationType(ProjectCollaborationType.INTERNATIONAL_BILATERAL);
        dto.setResearchType(ProjectResearchType.INNOVATION);
        dto.setNotFunded(true);
        dto.setCosts(new MonetaryAmountDTO(1, 50000));

        var teamMember = new PersonProjectContributionDTO();
        teamMember.setPersonId(1);
        teamMember.setOrderNumber(1);
        teamMember.setContributionType(PersonProjectContributionType.TEAM_MEMBER);
        teamMember.setInvestigationRole(PersonProjectInvestigationRole.RESEARCHER);

        var contribDesc = new MultilingualContentDTO();
        contribDesc.setLanguageTagId(1);
        contribDesc.setLanguageTag(LanguageAbbreviations.ENGLISH);
        contribDesc.setContent("Lead researcher");
        contribDesc.setPriority(1);
        teamMember.setContributionDescription(List.of(contribDesc));

        var displayAffiliation = new MultilingualContentDTO();
        displayAffiliation.setLanguageTagId(1);
        displayAffiliation.setLanguageTag(LanguageAbbreviations.ENGLISH);
        displayAffiliation.setContent("University of Novi Sad");
        displayAffiliation.setPriority(1);
        teamMember.setDisplayAffiliationStatement(List.of(displayAffiliation));

        teamMember.setInstitutionIds(List.of(1));

        dto.setTeam(List.of(teamMember));

        return dto;
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
