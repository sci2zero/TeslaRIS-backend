package rs.teslaris.core.integration;

import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.fasterxml.jackson.databind.ObjectMapper;
import java.time.LocalDate;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders;
import rs.teslaris.core.dto.commontypes.MultilingualContentDTO;
import rs.teslaris.core.dto.document.CourseDTO;
import rs.teslaris.core.dto.document.PersonEventContributionDTO;
import rs.teslaris.core.dto.person.PersonNameDTO;
import rs.teslaris.core.model.document.EventContributionType;
import rs.teslaris.core.model.person.PersonNameType;

@SpringBootTest
public class CourseControllerTest extends BaseTest {

    @Autowired
    private ObjectMapper objectMapper;


    private CourseDTO getTestPayload() {
        var dummyMC = List.of(new MultilingualContentDTO(1, "EN", "Content", 1));

        var courseDTO = new CourseDTO();
        courseDTO.setName(dummyMC);
        courseDTO.setNameAbbreviation(dummyMC);
        courseDTO.setCountryId(1);
        courseDTO.setPlace(dummyMC);
        courseDTO.setDescription(dummyMC);
        courseDTO.setKeywords(dummyMC);
        courseDTO.setDateFrom(LocalDate.now());
        courseDTO.setDateTo(LocalDate.now());
        courseDTO.setSerialEvent(false);
        courseDTO.setCourseLevel("Advanced");
        courseDTO.setCourseCode("MOCK CODE");

        var contribution =
            new PersonEventContributionDTO(EventContributionType.ORGANIZATION_BOARD_CHAIR);
        contribution.setOrderNumber(1);
        contribution.setPersonId(1);
        contribution.setContributionDescription(dummyMC);
        contribution.setDisplayAffiliationStatement(dummyMC);
        contribution.setPersonName(
            new PersonNameDTO(null, "Ime", "Srednje ime", "Prezime", null, null,
                PersonNameType.DISPLAY_NAME));
        courseDTO.setContributions(List.of(contribution));

        return courseDTO;
    }

    @Test
    @WithMockUser(username = "test.admin@test.com", password = "testAdmin")
    public void testReadAllCourses() throws Exception {
        mockMvc.perform(
            MockMvcRequestBuilders.get("http://localhost:8081/api/course?page=0&size=5")
                .contentType(MediaType.APPLICATION_JSON)).andExpect(status().isOk());
    }

    @Test
    @WithMockUser(username = "test.admin@test.com", password = "testAdmin")
    public void testReadCourse() throws Exception {
        mockMvc.perform(
            MockMvcRequestBuilders.get("http://localhost:8081/api/course/{courseId}", 7)
                .contentType(MediaType.APPLICATION_JSON)).andExpect(status().isOk());
    }

    @Test
    @WithMockUser(username = "test.admin@test.com", password = "testAdmin")
    public void testCreateCourse() throws Exception {
        String jwtToken = authenticateAdminAndGetToken();

        var courseDTO = getTestPayload();

        String requestBody = objectMapper.writeValueAsString(courseDTO);
        mockMvc.perform(MockMvcRequestBuilders.post("http://localhost:8081/api/course")
                .content(requestBody).contentType(MediaType.APPLICATION_JSON)
                .header(HttpHeaders.AUTHORIZATION, "Bearer " + jwtToken)
                .header("Idempotency-Key", "MOCK_KEY_COURSE"))
            .andExpect(status().isCreated())
            .andExpect(jsonPath("$.courseLevel").value("Advanced"))
            .andExpect(jsonPath("$.courseCode").value("MOCK CODE"));
    }

    @Test
    @WithMockUser(username = "test.admin@test.com", password = "testAdmin")
    public void testUpdateCourse() throws Exception {
        String jwtToken = authenticateAdminAndGetToken();

        var courseDTO = getTestPayload();

        String requestBody = objectMapper.writeValueAsString(courseDTO);
        mockMvc.perform(
                MockMvcRequestBuilders.put("http://localhost:8081/api/course/{courseId}", 7)
                    .content(requestBody).contentType(MediaType.APPLICATION_JSON)
                    .header(HttpHeaders.AUTHORIZATION, "Bearer " + jwtToken))
            .andExpect(status().isNoContent());
    }

    @Test
    @WithMockUser(username = "test.admin@test.com", password = "testAdmin")
    public void testDeleteCourse() throws Exception {
        String jwtToken = authenticateAdminAndGetToken();

        mockMvc.perform(
                MockMvcRequestBuilders.delete("http://localhost:8081/api/course/{courseId}",
                        8).contentType(MediaType.APPLICATION_JSON)
                    .header(HttpHeaders.AUTHORIZATION, "Bearer " + jwtToken))
            .andExpect(status().isNoContent());
    }
}
