package rs.teslaris.core.unit.migrator;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.fasterxml.jackson.databind.ObjectMapper;
import java.time.LocalDate;
import java.util.List;
import java.util.Set;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import rs.teslaris.core.model.person.PrizeType;
import rs.teslaris.core.service.interfaces.commontypes.LanguageService;
import rs.teslaris.core.service.interfaces.commontypes.LanguageTagService;
import rs.teslaris.core.service.interfaces.commontypes.ResearchAreaService;
import rs.teslaris.core.util.functional.Pair;
import rs.teslaris.migrator.converter.hydrator.HydratorConversionUtil;
import rs.teslaris.migrator.converter.hydrator.HydratorPrizeExtractor;
import rs.teslaris.migrator.model.hydrator.HydratorCVModel;
import rs.teslaris.migrator.util.MigrationLog;

@ExtendWith(MockitoExtension.class)
public class HydratorPrizeExtractorTest {

    private static final ObjectMapper MAPPER = new ObjectMapper();

    @Mock
    private LanguageTagService languageTagService;

    @Mock
    private LanguageService languageService;

    @Mock
    private ResearchAreaService researchAreaService;

    @Mock
    private MigrationLog migrationLog;

    private HydratorPrizeExtractor extractor;


    @BeforeEach
    void setUp() {
        extractor = new HydratorPrizeExtractor(
            new HydratorConversionUtil(languageTagService, languageService, researchAreaService),
            migrationLog);
    }

    private HydratorCVModel.Curriculum curriculum(String distinctions) throws Exception {
        return MAPPER.readValue("""
            {"id": "cv-1", "fullName": "John Doe",
             "curriculum": {"language": "en", "distinctions": %s}}
            """.formatted(distinctions), HydratorCVModel.Curriculum.class);
    }

    @Test
    void shouldMapDistinctionToPrize() throws Exception {
        var record = curriculum("""
            {"total": 1, "distinction": [{"id": "7", "privacyLevel": "publico",
              "distinctionType": {"code": "P", "type": "Award"},
              "name": "Test Award", "effectiveDate": "2015"}]}
            """);

        var result = extractor.extract(record);

        assertEquals(1, result.size());
        var item = result.getFirst();
        assertEquals("cv-1", item.personSourceKey());
        assertEquals("cv-1#distinction#7", extractor.keyOf(record, item));
        assertEquals("Test Award", item.prize().getTitle().getFirst().getContent());
        assertEquals(LocalDate.of(2015, 1, 1), item.prize().getDate());
        assertEquals(PrizeType.AWARD, item.prize().getPrizeType());
        assertTrue(item.prize().getResearchAreasId().isEmpty());
        verify(migrationLog, never()).valueDropped(anyString(), anyString(), anyString(),
            anyString(), anyString());
    }

    @Test
    void shouldMapAllDistinctionTypes() throws Exception {
        var result = extractor.extract(curriculum("""
            {"distinction": [
              {"id": "1", "distinctionType": {"code": "P"}, "name": "Test Award"},
              {"id": "2", "distinctionType": {"code": "T"}, "name": "Test Title"},
              {"id": "3", "distinctionType": {"code": "O"}, "name": "Test Other"}]}
            """));

        assertEquals(PrizeType.AWARD, result.get(0).prize().getPrizeType());
        assertEquals(PrizeType.TITLE, result.get(1).prize().getPrizeType());
        assertEquals(PrizeType.OTHER, result.get(2).prize().getPrizeType());
    }

    @Test
    void shouldDropUnknownType() throws Exception {
        var result = extractor.extract(curriculum("""
            {"distinction": [{"id": "1", "distinctionType": {"code": "X"},
              "name": "Test Award", "effectiveDate": "2015"}]}
            """));

        assertEquals(1, result.size());
        assertNull(result.getFirst().prize().getPrizeType());
        assertNull(result.getFirst().rejection());
        verify(migrationLog).valueDropped(anyString(), eq("PERSON_PRIZE"),
            eq("cv-1#distinction#1"), eq("MAP-040"), eq("unknown distinction type 'X'"));
    }

    @Test
    void shouldRejectInvalidDates() throws Exception {
        var result = extractor.extract(curriculum("""
            {"distinction": [
              {"id": "1", "name": "Test Award", "effectiveDate": "unknown"},
              {"id": "2", "name": "Test Title", "effectiveDate": "2015",
               "endDate": {"year": "2020", "month": "2", "day": "30"}},
              {"id": "3", "name": "Test Other", "endDate": {"year": "2020", "day": "5"}}]}
            """));

        assertEquals(3, result.size());
        assertEquals("invalid effective date 'unknown'", result.get(0).rejection());
        assertEquals("invalid date (year='2020', month='2', day='30')",
            result.get(1).rejection());
        assertEquals("invalid date (year='2020', month='null', day='5')",
            result.get(2).rejection());
    }

    @Test
    void shouldTreatDateWithoutYearAsAbsent() throws Exception {
        var result = extractor.extract(curriculum("""
            {"distinction": [{"id": "1", "name": "Test Award",
              "endDate": {"month": "6", "day": "15"}}]}
            """));

        assertNull(result.getFirst().prize().getDate());
        assertNull(result.getFirst().prize().getEndDate());
        assertNull(result.getFirst().rejection());
    }

    @Test
    void shouldSkipDistinctionWithoutIdOrName() throws Exception {
        var result = extractor.extract(curriculum("""
            {"distinction": [{"name": "Test Award"}, {"id": "2", "name": " "}]}
            """));

        assertTrue(result.isEmpty());
        verify(migrationLog).valueDropped(anyString(), eq("PERSON_PRIZE"), eq("cv-1"),
            eq("MAP-040"), eq("distinction without id"));
        verify(migrationLog).valueDropped(anyString(), eq("PERSON_PRIZE"),
            eq("cv-1#distinction#2"), eq("MAP-040"), eq("distinction without name"));
    }

    @Test
    void shouldReturnNothingWithoutDistinctions() throws Exception {
        var record = MAPPER.readValue("""
            {"id": "cv-2", "curriculum": {"language": "en"}}
            """, HydratorCVModel.Curriculum.class);

        assertTrue(extractor.extract(record).isEmpty());
    }

    @Test
    void shouldMapDescriptionEndDateAndKeywords() throws Exception {
        var result = extractor.extract(curriculum("""
            {"distinction": [
              {"id": "1", "name": "Test Award", "description": " Awarded for testing. ",
               "endDate": {"year": "2020", "month": "6", "day": "15"},
               "keywords": {"total": 3, "keyword": ["testing", " ", "testing", "quality"]}},
              {"id": "2", "name": "Test Title", "endDate": {"year": "2021"}}]}
            """));

        var full = result.get(0).prize();
        assertEquals("Awarded for testing.", full.getDescription().getFirst().getContent());
        assertEquals(LocalDate.of(2020, 6, 15), full.getEndDate());
        assertEquals("testing\nquality", full.getKeywords().getFirst().getContent());

        var bare = result.get(1).prize();
        assertTrue(bare.getDescription().isEmpty());
        assertTrue(bare.getKeywords().isEmpty());
        assertEquals(LocalDate.of(2021, 1, 1), bare.getEndDate());
    }

    @Test
    void shouldMapResearchClassificationsToResearchAreas() throws Exception {
        when(researchAreaService.getResearchAreaNames("EN")).thenReturn(List.of(
            new Pair<>(10, "social sciences"),
            new Pair<>(11, "psychology")));

        var result = extractor.extract(curriculum("""
            {"distinction": [{"id": "1", "name": "Test Award",
              "researchClassifications": {"total": 3, "researchClassification": [
                {"code": "501", "value": "Social Sciences - Psychology"},
                {"code": "502", "value": "Social Sciences - Unknown Field"},
                {"code": "999", "value": "Imaginary Sciences"}]}}]}
            """));

        assertEquals(Set.of(11, 10), result.getFirst().prize().getResearchAreasId());
        verify(migrationLog).valueDropped(anyString(), eq("PERSON_PRIZE"),
            eq("cv-1#distinction#1"), eq("MAP-040"),
            eq("research classification 'Social Sciences - Unknown Field' mapped to " +
                "broader area 'Social Sciences'"));
        verify(migrationLog).valueDropped(anyString(), eq("PERSON_PRIZE"),
            eq("cv-1#distinction#1"), eq("MAP-040"),
            eq("research classification '999' (Imaginary Sciences) has no research area"));
    }
}
