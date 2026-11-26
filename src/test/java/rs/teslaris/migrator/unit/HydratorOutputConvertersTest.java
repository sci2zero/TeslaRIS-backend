package rs.teslaris.migrator.unit;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
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
import rs.teslaris.core.dto.person.PersonNameDTO;
import rs.teslaris.core.model.document.DocumentContributionType;
import rs.teslaris.core.model.document.EventContributionType;
import rs.teslaris.core.service.interfaces.commontypes.CountryService;
import rs.teslaris.core.service.interfaces.commontypes.LanguageService;
import rs.teslaris.core.service.interfaces.commontypes.LanguageTagService;
import rs.teslaris.core.service.interfaces.commontypes.ResearchAreaService;
import rs.teslaris.core.util.functional.Pair;
import rs.teslaris.migrator.converter.hydrator.HydratorConversionUtil;
import rs.teslaris.migrator.converter.hydrator.HydratorInstitutionResolver;
import rs.teslaris.migrator.converter.hydrator.HydratorJournalResolver;
import rs.teslaris.migrator.converter.hydrator.HydratorOutputConverters;
import rs.teslaris.migrator.converter.hydrator.HydratorPersonResolver;
import rs.teslaris.migrator.model.hydrator.HydratorCVModel;
import rs.teslaris.migrator.util.MigrationLog;

@ExtendWith(MockitoExtension.class)
public class HydratorOutputConvertersTest {

    private static final ObjectMapper MAPPER = new ObjectMapper();

    private static final String CLASSIFICATIONS = """
        {"total": 3, "researchClassification": [
          {"code": "501", "value": "Social Sciences - Psychology"},
          {"code": "502", "value": "Social Sciences - Unknown Field"},
          {"code": "999", "value": "Imaginary Sciences"}]}
        """;

    @Mock
    private LanguageTagService languageTagService;

    @Mock
    private LanguageService languageService;

    @Mock
    private ResearchAreaService researchAreaService;

    @Mock
    private HydratorJournalResolver journalResolver;

    @Mock
    private HydratorPersonResolver personResolver;

    @Mock
    private HydratorInstitutionResolver institutionResolver;

    @Mock
    private CountryService countryService;

    @Mock
    private MigrationLog migrationLog;

    private HydratorOutputConverters converters;


    @BeforeEach
    void setUp() {
        converters = new HydratorOutputConverters(
            new HydratorConversionUtil(languageTagService, languageService, researchAreaService),
            journalResolver, personResolver, institutionResolver, countryService, migrationLog);
    }

    private void withResearchAreaVocabulary() {
        when(researchAreaService.getResearchAreaNames("EN")).thenReturn(List.of(
            new Pair<>(10, "social sciences"),
            new Pair<>(11, "psychology")));
    }

    private HydratorCVModel.Curriculum curriculum(String output) throws Exception {
        return MAPPER.readValue("""
            {"id": "cv-1", "fullName": "John Doe",
             "curriculum": {"language": "en",
              "outputs": {"total": 1, "output": %s}}}
            """.formatted(output), HydratorCVModel.Curriculum.class);
    }

    private HydratorCVModel.Output firstOutput(HydratorCVModel.Curriculum record) {
        return record.curriculum().outputs().output().getFirst();
    }

    @Test
    void shouldMapResearchClassificationsToJournalPublicationResearchAreas() throws Exception {
        withResearchAreaVocabulary();

        var record = curriculum("""
            [{"id": "5", "journalArticle": {"articleTitle": "Test Article",
              "researchClassifications": %s}}]
            """.formatted(CLASSIFICATIONS));

        var dto = converters.toJournalPublication(record, firstOutput(record));

        assertEquals(Set.of(11, 10), dto.getResearchAreasId());
        verify(migrationLog).valueDropped(anyString(), eq("JOURNAL_PUBLICATION"),
            eq("cv-1#output#5"), eq("CW-001-MAP-000069"),
            eq("research classification 'Social Sciences - Unknown Field' mapped to " +
                "broader area 'Social Sciences'"));
        verify(migrationLog).valueDropped(anyString(), eq("JOURNAL_PUBLICATION"),
            eq("cv-1#output#5"), eq("CW-001-MAP-000069"),
            eq("research classification '999' (Imaginary Sciences) has no research area"));
    }

    @Test
    void shouldMapResearchClassificationsToProceedingsPublicationResearchAreas() throws Exception {
        withResearchAreaVocabulary();

        var record = curriculum("""
            [{"id": "6", "conferencePaper": {"paperTitle": "Test Paper",
              "researchClassifications": %s}}]
            """.formatted(CLASSIFICATIONS));

        var dto = converters.toProceedingsPublication(record, firstOutput(record));

        assertEquals(Set.of(11, 10), dto.getResearchAreasId());
        verify(migrationLog).valueDropped(anyString(), eq("PROCEEDINGS_PUBLICATION"),
            eq("cv-1#output#6"), eq("CW-001-MAP-000088"),
            eq("research classification '999' (Imaginary Sciences) has no research area"));
    }

    @Test
    void shouldMapResearchClassificationsToThesisResearchAreas() throws Exception {
        withResearchAreaVocabulary();

        var record = curriculum("""
            [{"id": "7", "dissertation": {"title": "Test Thesis",
              "researchClassifications": %s}}]
            """.formatted(CLASSIFICATIONS));

        var dto = converters.toThesis(record, firstOutput(record));

        assertEquals(Set.of(11, 10), dto.getResearchAreasId());
        verify(migrationLog).valueDropped(anyString(), eq("THESIS"),
            eq("cv-1#output#7"), eq("CW-001-MAP-000217"),
            eq("research classification '999' (Imaginary Sciences) has no research area"));
    }

    @Test
    void shouldLeaveResearchAreasEmptyWhenOutputHasNoClassifications() throws Exception {
        var record = curriculum("""
            [{"id": "8", "journalArticle": {"articleTitle": "Test Article"}}]
            """);

        var dto = converters.toJournalPublication(record, firstOutput(record));

        assertTrue(dto.getResearchAreasId().isEmpty());
        verify(migrationLog, never()).valueDropped(anyString(), anyString(), anyString(),
            eq("CW-001-MAP-000069"), anyString());
    }

    @Test
    void shouldLeaveSlidesWithoutResearchAreas() throws Exception {
        var record = curriculum("""
            [{"id": "9", "conferencePaper": {"paperTitle": "Test Paper",
              "presentationDate": {"year": "2024"},
              "researchClassifications": %s}}]
            """.formatted(CLASSIFICATIONS));

        var dto = converters.toPresentation(record, firstOutput(record));

        assertTrue(dto.getResearchAreasId().isEmpty());
    }

    private void withOwner() {
        var ownerName = new PersonNameDTO();
        ownerName.setFirstname("John");
        ownerName.setLastname("Doe");
        when(personResolver.ownerName(any())).thenReturn(ownerName);
        when(personResolver.resolveOwner(any())).thenReturn(7);
    }

    @Test
    void shouldAddOwnerAsPresenterWhenSlidesHaveNoPresenters() throws Exception {
        // given
        withOwner();
        var record = curriculum("""
            [{"id": "9", "conferencePaper": {"paperTitle": "Test Paper",
              "presentationDate": {"year": "2024"}}}]
            """);

        // when
        var dto = converters.toPresentation(record, firstOutput(record));

        // then
        assertEquals(1, dto.getContributions().size());
        assertEquals(DocumentContributionType.PRESENTER,
            dto.getContributions().getFirst().getContributionType());
        assertEquals(7, dto.getContributions().getFirst().getPersonId());
        verify(migrationLog).valueDefaulted(anyString(), eq("CONFERENCE_PRESENTATION"),
            anyString(), eq("CW-001-MAP-000091"), anyString());
    }

    @Test
    void shouldAddOwnerAsPresenterAndSpeakerWhenPosterIsMapped() throws Exception {
        // given
        withOwner();
        var record = curriculum("""
            [{"id": "9", "conferencePoster": {"title": "Test Poster",
              "conferenceName": "Test Conference",
              "conferenceDate": {"year": "2024", "month": "6", "day": "3"},
              "authors": {"total": 2, "author": [{"self": true, "name": "Doe, John"},
                {"name": "Smith, Jane"}]}}}]
            """);
        var output = firstOutput(record);

        // when
        var dto = converters.toConferencePoster(record, output);
        var speakers = converters.speakerContributions(dto,
            output.conferencePoster().conferenceDate(), null);

        // then
        var presenter = dto.getContributions().getLast();
        assertEquals(DocumentContributionType.PRESENTER, presenter.getContributionType());
        assertEquals(7, presenter.getPersonId());
        assertEquals(1, speakers.size());
        assertEquals(EventContributionType.SPEAKER,
            speakers.getFirst().getEventContributionType());
        assertEquals(LocalDate.of(2024, 6, 3), speakers.getFirst().getDateFrom());
        assertEquals(LocalDate.of(2024, 6, 3), speakers.getFirst().getDateTo());
    }
}
