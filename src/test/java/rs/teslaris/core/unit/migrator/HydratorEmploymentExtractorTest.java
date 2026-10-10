package rs.teslaris.core.unit.migrator;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;

import com.fasterxml.jackson.databind.ObjectMapper;
import java.time.LocalDate;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import rs.teslaris.core.model.person.InvolvementType;
import rs.teslaris.core.service.interfaces.commontypes.LanguageService;
import rs.teslaris.core.service.interfaces.commontypes.LanguageTagService;
import rs.teslaris.core.service.interfaces.commontypes.ResearchAreaService;
import rs.teslaris.migrator.converter.hydrator.HydratorConversionUtil;
import rs.teslaris.migrator.converter.hydrator.HydratorEmploymentExtractor;
import rs.teslaris.migrator.model.hydrator.HydratorCVModel;
import rs.teslaris.migrator.util.MigrationLog;

@ExtendWith(MockitoExtension.class)
public class HydratorEmploymentExtractorTest {

    private static final ObjectMapper MAPPER = new ObjectMapper();

    @Mock
    private LanguageTagService languageTagService;

    @Mock
    private LanguageService languageService;

    @Mock
    private ResearchAreaService researchAreaService;

    @Mock
    private MigrationLog migrationLog;

    private HydratorEmploymentExtractor extractor;


    @BeforeEach
    void setUp() {
        extractor = new HydratorEmploymentExtractor(
            new HydratorConversionUtil(languageTagService, languageService, researchAreaService),
            migrationLog);
    }

    private HydratorCVModel.Curriculum curriculum(String employments) throws Exception {
        return MAPPER.readValue("""
            {"id": "cv-1", "fullName": "John Doe",
             "curriculum": {"language": "en", "employments": %s}}
            """.formatted(employments), HydratorCVModel.Curriculum.class);
    }

    @Test
    void shouldMapEmploymentKeyedBySourceId() throws Exception {
        var record = curriculum("""
            {"employment": [{"id": "42", "institution": [{"name": "Test University"}],
              "positionTitle": {"code": "1", "title": "Test Researcher"},
              "startDate": {"year": "2010", "month": "3"},
              "endDate": {"year": "2015", "month": "6", "day": "30"}}]}
            """);

        var result = extractor.extract(record);

        assertEquals(1, result.size());
        var item = result.getFirst();
        assertEquals("cv-1#employment#42", extractor.keyOf(record, item));
        assertEquals("cv-1", item.personSourceKey());
        assertEquals("Test University", item.institution().name());
        assertNull(item.rejection());
        assertEquals(InvolvementType.EMPLOYED_AT, item.employment().getInvolvementType());
        assertEquals(LocalDate.of(2010, 3, 1), item.employment().getDateFrom());
        assertEquals(LocalDate.of(2015, 6, 30), item.employment().getDateTo());
        assertEquals("Test University",
            item.employment().getDisplayOrganisationUnit().getFirst().getContent());
        assertEquals("Test Researcher", item.employment().getRole().getFirst().getContent());
    }

    @Test
    void shouldSkipEmploymentWithoutId() throws Exception {
        var result = extractor.extract(curriculum("""
            {"employment": [{"institution": [{"name": "Test University"}]}]}
            """));

        assertTrue(result.isEmpty());
        verify(migrationLog).valueDropped(anyString(), eq("PERSON_EMPLOYMENT"), eq("cv-1"),
            eq("MAP-000039"), eq("employment without id"));
    }

    @Test
    void shouldSkipEmploymentWithoutInstitution() throws Exception {
        var result = extractor.extract(curriculum("""
            {"employment": [{"id": "1"}]}
            """));

        assertTrue(result.isEmpty());
        verify(migrationLog).valueDropped(anyString(), eq("PERSON_EMPLOYMENT"),
            eq("cv-1#employment#1"), eq("MAP-000039"), eq("employment without institution"));
    }

    @Test
    void shouldRejectInvalidDate() throws Exception {
        var result = extractor.extract(curriculum("""
            {"employment": [{"id": "1", "institution": [{"name": "Test University"}],
              "startDate": {"year": "2010"},
              "endDate": {"year": "2015", "month": "2", "day": "30"}}]}
            """));

        assertEquals(1, result.size());
        assertEquals("invalid date (year='2015', month='2', day='30')",
            result.getFirst().rejection());
    }
}
