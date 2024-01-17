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
import rs.teslaris.core.dto.person.ContactDTO;
import rs.teslaris.core.dto.person.PersonNameDTO;
import rs.teslaris.core.dto.person.PostalAddressDTO;
import rs.teslaris.core.model.document.PublicationSeriesContributionType;

@SpringBootTest
public class BookSeriesControllerTest extends BaseTest {

    @Autowired
    private ObjectMapper objectMapper;

    private BookSeriesDTO getTestPayload() {
        var dummyMC = List.of(new MultilingualContentDTO(25, "EN", "Content", 1));

        var bookSeriesDTO = new BookSeriesDTO();
        bookSeriesDTO.setTitle(dummyMC);
        bookSeriesDTO.setNameAbbreviation(dummyMC);
        bookSeriesDTO.setEISSN("eISSN");
        bookSeriesDTO.setPrintISSN("printISSN");

        var contribution =
            new PersonPublicationSeriesContributionDTO(
                PublicationSeriesContributionType.SCIENTIFIC_BOARD_MEMBER,
                LocalDate.now(), LocalDate.now());
        contribution.setOrderNumber(1);
        contribution.setInstitutionIds(new ArrayList<>());
        contribution.setPersonName(new PersonNameDTO());
        contribution.setContact(new ContactDTO());
        contribution.setContributionDescription(dummyMC);
        contribution.setPostalAddress(new PostalAddressDTO(21, dummyMC, dummyMC));
        contribution.setDisplayAffiliationStatement(dummyMC);
        bookSeriesDTO.setContributions(List.of(contribution));
        bookSeriesDTO.setLanguageTagIds(new ArrayList<>());

        return bookSeriesDTO;
    }

    @Test
    @WithMockUser(username = "admin@admin.com", password = "admin")
    public void testReadAllBookSeriess() throws Exception {
        mockMvc.perform(
            MockMvcRequestBuilders.get("http://localhost:8081/api/book-series?page=0&size=5")
                .contentType(MediaType.APPLICATION_JSON)).andExpect(status().isOk());
    }

    @Test
    @WithMockUser(username = "admin@admin.com", password = "admin")
    public void testReadBookSeries() throws Exception {
        mockMvc.perform(
            MockMvcRequestBuilders.get("http://localhost:8081/api/book-series/{bookSeriesId}", 51)
                .contentType(MediaType.APPLICATION_JSON)).andExpect(status().isOk());
    }

    @Test
    @WithMockUser(username = "admin@admin.com", password = "admin")
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
            .andExpect(jsonPath("$.printISSN").value("printISSN"))
            .andExpect(jsonPath("$.eissn").value("eISSN"));
    }

    @Test
    @WithMockUser(username = "admin@admin.com", password = "admin")
    public void testUpdateBookSeries() throws Exception {
        String jwtToken = authenticateAdminAndGetToken();

        var bookSeriesDTO = getTestPayload();
        bookSeriesDTO.setEISSN("TEST_E_ISSN");
        bookSeriesDTO.setPrintISSN("TEST_PRINT_ISSN");

        String requestBody = objectMapper.writeValueAsString(bookSeriesDTO);
        mockMvc.perform(
                MockMvcRequestBuilders.put("http://localhost:8081/api/book-series/{bookSeriesId}", 51)
                    .content(requestBody).contentType(MediaType.APPLICATION_JSON)
                    .header(HttpHeaders.AUTHORIZATION, "Bearer " + jwtToken))
            .andExpect(status().isNoContent());
    }

    @Test
    @WithMockUser(username = "admin@admin.com", password = "admin")
    public void testDeleteBookSeries() throws Exception {
        String jwtToken = authenticateAdminAndGetToken();

        mockMvc.perform(
                MockMvcRequestBuilders.delete("http://localhost:8081/api/book-series/{bookSeriesId}",
                        54)
                    .contentType(MediaType.APPLICATION_JSON)
                    .header(HttpHeaders.AUTHORIZATION, "Bearer " + jwtToken))
            .andExpect(status().isNoContent());
    }
}
