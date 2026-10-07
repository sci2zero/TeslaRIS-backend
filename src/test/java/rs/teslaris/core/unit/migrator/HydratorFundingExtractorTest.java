package rs.teslaris.core.unit.migrator;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;

import com.fasterxml.jackson.databind.ObjectMapper;
import java.time.LocalDate;
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
import rs.teslaris.migrator.converter.hydrator.HydratorConversionUtil;
import rs.teslaris.migrator.converter.hydrator.HydratorFundingExtractor;
import rs.teslaris.migrator.model.hydrator.HydratorProjectModel;
import rs.teslaris.migrator.util.MigrationLog;

@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
public class HydratorFundingExtractorTest {

    private static final ObjectMapper MAPPER = new ObjectMapper();

    private static final String ID_TYPES = "https://w3id.org/cerif/vocab/IdentifierTypes#";

    @Mock
    private LanguageTagService languageTagService;

    @Mock
    private LanguageService languageService;

    @Mock
    private ResearchAreaService researchAreaService;

    @Mock
    private MigrationLog migrationLog;

    private HydratorFundingExtractor extractor;


    @BeforeEach
    void setUp() {
        extractor = new HydratorFundingExtractor(
            new HydratorConversionUtil(languageTagService, languageService, researchAreaService),
            migrationLog);
    }

    private HydratorProjectModel.ProjectDocument document(String funded) throws Exception {
        return MAPPER.readValue("""
            {"id": "doc-1", "record": {"metadata": {"project": {
              "projectId": "TEST001",
              "oaMandate": {"mandated": "True", "uri": "https://example.com/oa-policy"},
              "funded": %s}}}}
            """.formatted(funded), HydratorProjectModel.ProjectDocument.class);
    }

    private static String funding(String id, String startDate) {
        return """
            {"fundedBy": {"orgUnit": {"id": "OU1", "name": {"language": "en", "value": "Test Agency"},
               "rorId": "https://ror.org/00example",
               "identifiers": [{"type": "%sRingGold", "value": "12345"}]}},
             "fundedAs": {"funding": {"id": "%s", "grantId": "GA-1",
               "type": {"value": "https://example.com/vocab#FundingProgramme"},
               "name": {"language": "en", "value": "Test Programme 2020"},
               "amount": {"currency": "eur", "value": "1500.50"},
               "duration": {"startDate": "%s", "endDate": "2021-12-31"},
               "identifiers": [{"type": "%sDOI", "value": "10.1234/test"},
                               {"type": "%sProjectReference", "value": "TEST/0001/2020"},
                               {"type": "%sQREN", "value": "QREN-1"}]}}}
            """.formatted(ID_TYPES, id, startDate, ID_TYPES, ID_TYPES, ID_TYPES);
    }

    @Test
    void shouldMapFunding() throws Exception {
        var record = document("[" + funding("F1", "2020-01-01") + "]");

        var result = extractor.extract(record);

        assertEquals(1, result.size());
        var item = result.getFirst();
        assertEquals("TEST001#funding#F1", extractor.keyOf(record, item));
        assertNull(item.rejection());
        var dto = item.funding();
        assertEquals("Test Programme 2020", dto.getName().getFirst().getContent());
        assertEquals("10.1234/test", dto.getDoi());
        assertEquals(Set.of("TEST/0001/2020", "QREN-1"), dto.getInternalIdentifiers());
        assertEquals("GA-1", dto.getGrantAgreementId());
        assertEquals(1500.50, dto.getAmount().getAmount());
        assertEquals("EUR", dto.getAmount().getCurrencyCode());
        assertEquals(LocalDate.of(2020, 1, 1), dto.getDateFrom());
        assertEquals(LocalDate.of(2021, 12, 31), dto.getDateTo());
        assertEquals(Boolean.TRUE, dto.getOaMandated());
        assertEquals("https://example.com/oa-policy", dto.getOaMandateUrl());
        assertTrue(dto.getFundingTypes().isEmpty());
    }

    @Test
    void shouldPassFunderIdentifiersToResolver() throws Exception {
        var funder = extractor.extract(document("[" + funding("F1", "2020-01-01") + "]"))
            .getFirst().funder();

        assertEquals("Test Agency", funder.name());
        var identifiers = funder.otherIdentifiers().identifiers();
        assertEquals("12345", identifiers.get(0).identifier());
        assertEquals("RingGold", identifiers.get(0).type());
        assertEquals("ROR", identifiers.get(1).type());
    }

    @Test
    void shouldLogMissingTypeVocabulary() throws Exception {
        extractor.extract(document("[" + funding("F1", "2020-01-01") + "]"));

        verify(migrationLog).valueDropped(anyString(), eq("PROJECT_FUNDING"),
            eq("TEST001#funding#F1"), eq("MAP-002"),
            eq("funding type 'FundingProgramme' has no vocabulary"));
    }

    @Test
    void shouldMapEveryFundingOfProject() throws Exception {
        var result = extractor.extract(document(
            "[" + funding("F1", "2020-01-01") + "," + funding("F2", "2020-06-01") + "]"));

        assertEquals(2, result.size());
        assertEquals("F2", result.get(1).sourceId());
    }

    @Test
    void shouldRejectInvalidDate() throws Exception {
        var result = extractor.extract(document("[" + funding("F1", "2020-02-30") + "]"));

        assertEquals("invalid date '2020-02-30'", result.getFirst().rejection());
    }
}
