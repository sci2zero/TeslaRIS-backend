package rs.teslaris.core.integration;

import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.fasterxml.jackson.databind.ObjectMapper;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders;
import rs.teslaris.core.dto.commontypes.MultilingualContentDTO;
import rs.teslaris.core.dto.document.BookSeriesDTO;
import rs.teslaris.core.dto.document.PersonPublicationSeriesContributionDTO;
import rs.teslaris.core.dto.person.PersonNameDTO;
import rs.teslaris.core.model.document.PublicationSeriesContributionType;

@SpringBootTest
public class BookSeriesControllerTest extends BaseTest {

    @Autowired
    private ObjectMapper objectMapper;

    private BookSeriesDTO getTestPayload() {
        var dummyMC = List.of(new MultilingualContentDTO(1, "EN", "Content", 1));

        var bookSeriesDTO = new BookSeriesDTO();
        bookSeriesDTO.setTitle(dummyMC);
        bookSeriesDTO.setNameAbbreviation(dummyMC);
        bookSeriesDTO.setEissn("1234-5678");
        bookSeriesDTO.setPrintISSN("8765-4321");

        var contribution =
            new PersonPublicationSeriesContributionDTO(
                PublicationSeriesContributionType.SCIENTIFIC_BOARD_MEMBER,
                LocalDate.now(), LocalDate.now());
        contribution.setOrderNumber(1);
        contribution.setPersonId(1);
        contribution.setContributionDescription(dummyMC);
        contribution.setDisplayAffiliationStatement(dummyMC);
        contribution.setPersonName(
            new PersonNameDTO(null, "Ime", "Srednje ime", "Prezime", null, null));
        bookSeriesDTO.setContributions(List.of(contribution));
        bookSeriesDTO.setLanguageTagIds(new ArrayList<>());

        return bookSeriesDTO;
    }

    @Test
    @WithMockUser(username = "test.admin@test.com", password = "testAdmin")
    public void testReadAllBookSeriess() throws Exception {
        mockMvc.perform(
            MockMvcRequestBuilders.get("http://localhost:8081/api/book-series?page=0&size=5")
                .contentType(MediaType.APPLICATION_JSON)).andExpect(status().isOk());
    }

    @Test
    @WithMockUser(username = "test.admin@test.com", password = "testAdmin")
    public void testReadBookSeries() throws Exception {
        mockMvc.perform(
            MockMvcRequestBuilders.get("http://localhost:8081/api/book-series/{bookSeriesId}", 3)
                .contentType(MediaType.APPLICATION_JSON)).andExpect(status().isOk());
    }

    @Test
    @WithMockUser(username = "test.admin@test.com", password = "testAdmin")
    public void testCreateBookSeries() throws Exception {
        String jwtToken = authenticateAdminAndGetToken();

        var bookSeriesDTO = getTestPayload();

        String requestBody = objectMapper.writeValueAsString(bookSeriesDTO);
        mockMvc.perform(
                MockMvcRequestBuilders.post("http://localhost:8081/api/book-series")
                    .content(requestBody)
                    .contentType(MediaType.APPLICATION_JSON)
                    .header(HttpHeaders.AUTHORIZATION, "Bearer " + jwtToken)
                    .header("Idempotency-Key", "MOCK_KEY_BOOK_SERIES")).andExpect(status().isCreated())
            .andExpect(jsonPath("$.printISSN").value("8765-4321"))
            .andExpect(jsonPath("$.eissn").value("1234-5678"));
    }

    @Test
    @WithMockUser(username = "test.admin@test.com", password = "testAdmin")
    public void testUpdateBookSeries() throws Exception {
        String jwtToken = authenticateAdminAndGetToken();

        var bookSeriesDTO = getTestPayload();
        bookSeriesDTO.setEissn("1234-5677");
        bookSeriesDTO.setPrintISSN("8765-4322");

        String requestBody = objectMapper.writeValueAsString(bookSeriesDTO);
        mockMvc.perform(
                MockMvcRequestBuilders.put("http://localhost:8081/api/book-series/{bookSeriesId}", 3)
                    .content(requestBody).contentType(MediaType.APPLICATION_JSON)
                    .header(HttpHeaders.AUTHORIZATION, "Bearer " + jwtToken))
            .andExpect(status().isNoContent());
    }

    @Test
    @WithMockUser(username = "test.admin@test.com", password = "testAdmin")
    public void testDeleteBookSeries() throws Exception {
        String jwtToken = authenticateAdminAndGetToken();

        mockMvc.perform(
                MockMvcRequestBuilders.delete("http://localhost:8081/api/book-series/{bookSeriesId}",
                        4)
                    .contentType(MediaType.APPLICATION_JSON)
                    .header(HttpHeaders.AUTHORIZATION, "Bearer " + jwtToken))
            .andExpect(status().isNoContent());
    }

    @Test
    @WithMockUser(username = "test.admin@test.com", password = "testAdmin")
    public void testSearchJournals() throws Exception {
        String jwtToken = authenticateAdminAndGetToken();

        mockMvc.perform(
                MockMvcRequestBuilders.get(
                        "http://localhost:8081/api/book-series/simple-search?tokens=eISSN&tokens=content")
                    .contentType(MediaType.APPLICATION_JSON)
                    .header(HttpHeaders.AUTHORIZATION, "Bearer " + jwtToken))
            .andExpect(status().isOk());
    }
}
