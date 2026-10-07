package rs.teslaris.core.unit.migrator;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
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
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;
import rs.teslaris.core.service.interfaces.commontypes.LanguageService;
import rs.teslaris.core.service.interfaces.commontypes.LanguageTagService;
import rs.teslaris.core.service.interfaces.commontypes.ResearchAreaService;
import rs.teslaris.core.util.functional.Pair;
import rs.teslaris.migrator.converter.hydrator.HydratorConversionUtil;
import rs.teslaris.migrator.converter.hydrator.HydratorProjectConverter;
import rs.teslaris.migrator.model.hydrator.HydratorProjectModel;
import rs.teslaris.migrator.util.MigrationLog;
import rs.teslaris.project.model.project.ProjectCollaborationType;
import rs.teslaris.project.model.project.ProjectResearchType;
import rs.teslaris.project.model.project.ProjectStatus;

@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
public class HydratorProjectConverterTest {

    private static final ObjectMapper MAPPER = new ObjectMapper();

    @Mock
    private LanguageTagService languageTagService;

    @Mock
    private LanguageService languageService;

    @Mock
    private ResearchAreaService researchAreaService;

    @Mock
    private MigrationLog migrationLog;

    private HydratorProjectConverter converter;


    @BeforeEach
    void setUp() {
        when(researchAreaService.getResearchAreaNames("EN")).thenReturn(List.of(
            new Pair<>(20, "natural sciences"),
            new Pair<>(21, "mechanical engineering")));

        converter = new HydratorProjectConverter(
            new HydratorConversionUtil(languageTagService, languageService, researchAreaService),
            migrationLog);
    }

    private HydratorProjectModel.ProjectDocument document(String project) throws Exception {
        return MAPPER.readValue("""
            {"id": "doc-1", "record": {"metadata": {"project": %s}}}
            """.formatted(project), HydratorProjectModel.ProjectDocument.class);
    }

    private static final String FULL_PROJECT = """
        {"projectId": "TEST001",
         "acronym": "TP",
         "titles": [{"language": "en", "value": "Test Project"},
                    {"language": "pt", "value": "Projeto de Teste"}],
         "abstracts": [{"language": "en", "value": "not applicable"}],
         "keywords": [{"language": "en", "value": "testing,quality"}],
         "identifiers": [
           {"type": "https://w3id.org/cerif/vocab/IdentifierTypes#ProjectReference",
            "value": "TEST/0001/2020"},
           {"type": "https://w3id.org/cerif/vocab/IdentifierTypes#QREN", "value": "QREN-1"},
           {"type": "https://w3id.org/cerif/vocab/IdentifierTypes#FundingProgram",
            "value": "Test Programme"}],
         "startDate": "2018-01-01", "endDate": "2020-12-31",
         "subjects": [{"language": "en", "value": "Natural sciences"},
                      {"language": "pt", "value": "Ciências naturais"}]}
        """;

    @Test
    void shouldMapFlatAttributes() throws Exception {
        var result = converter.toDTO(document(FULL_PROJECT));

        assertEquals("TEST001", result.sourceId());
        assertNull(result.rejection());
        var dto = result.project();
        assertEquals(2, dto.getName().size());
        assertEquals("Test Project", dto.getName().get(0).getContent());
        assertEquals(1, dto.getName().get(0).getPriority());
        assertEquals("PT", dto.getName().get(1).getLanguageTag());
        assertEquals(2, dto.getName().get(1).getPriority());
        assertEquals("TP", dto.getNameAbbreviation().getFirst().getContent());
        assertEquals("not applicable", dto.getDescription().getFirst().getContent());
        assertEquals("testing,quality", dto.getKeywords().getFirst().getContent());
        assertEquals(LocalDate.of(2018, 1, 1), dto.getDateFrom());
        assertEquals(LocalDate.of(2020, 12, 31), dto.getDateTo());
        assertEquals(Set.of(20), dto.getResearchAreasId());
        assertTrue(dto.getPersons().isEmpty());
        assertTrue(dto.getOrganisations().isEmpty());
    }

    @Test
    void shouldKeepProjectIdentifiersOnly() throws Exception {
        var dto = converter.toDTO(document(FULL_PROJECT)).project();

        assertEquals(Set.of("TEST/0001/2020", "QREN-1"), dto.getInternalIdentifiers());
        verify(migrationLog).valueDropped(anyString(), eq("PROJECT"), eq("TEST001"),
            eq("MAP-019"), eq("identifier type 'FundingProgram' not mapped"));
    }

    @Test
    void shouldSetProvisionalRequiredFields() throws Exception {
        var dto = converter.toDTO(document(FULL_PROJECT)).project();

        assertEquals(ProjectStatus.CONCLUDED, dto.getStatus());
        assertEquals(ProjectCollaborationType.NATIONAL, dto.getCollaborationType());
        assertEquals(ProjectResearchType.OTHER, dto.getResearchType());
        verify(migrationLog).valueDropped(anyString(), eq("PROJECT"), eq("TEST001"),
            eq("MAP-016/035"), anyString());
    }

    @Test
    void shouldTreatFutureEndAsOngoing() throws Exception {
        var dto = converter.toDTO(document("""
            {"projectId": "TEST002", "titles": [{"language": "en", "value": "Test"}],
             "endDate": "%s"}
            """.formatted(LocalDate.now().plusYears(1)))).project();

        assertEquals(ProjectStatus.ONGOING, dto.getStatus());
    }

    @Test
    void shouldSkipBlankAcronym() throws Exception {
        var dto = converter.toDTO(document("""
            {"projectId": "TEST003", "acronym": " ",
             "titles": [{"language": "en", "value": "Test"}]}
            """)).project();

        assertTrue(dto.getNameAbbreviation().isEmpty());
    }

    @Test
    void shouldRejectInvalidDate() throws Exception {
        var result = converter.toDTO(document("""
            {"projectId": "TEST004", "titles": [{"language": "en", "value": "Test"}],
             "startDate": "2020-02-30"}
            """));

        assertEquals("invalid date '2020-02-30'", result.rejection());
    }

    @Test
    void shouldSkipRecordWithoutProjectId() throws Exception {
        assertNull(converter.toDTO(document("{\"titles\": []}")));
    }

    @Test
    void shouldSplitSubjectLevelsOnSlash() throws Exception {
        var dto = converter.toDTO(document("""
            {"projectId": "TEST005", "titles": [{"language": "en", "value": "Test"}],
             "subjects": [{"language": "en",
                           "value": "Engineering and technology/Mechanical engineering"}]}
            """)).project();

        assertEquals(Set.of(21), dto.getResearchAreasId());
    }
}
