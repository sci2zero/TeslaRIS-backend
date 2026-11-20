package rs.teslaris.migrator.unit;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.verify;

import com.fasterxml.jackson.databind.ObjectMapper;
import java.util.Set;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import rs.teslaris.core.dto.person.PersonNameDTO;
import rs.teslaris.core.model.document.DocumentContributionType;
import rs.teslaris.core.model.document.EventContributionType;
import rs.teslaris.core.model.document.PerformanceRelatedOutputType;
import rs.teslaris.core.service.interfaces.commontypes.CountryService;
import rs.teslaris.core.service.interfaces.commontypes.LanguageService;
import rs.teslaris.core.service.interfaces.commontypes.LanguageTagService;
import rs.teslaris.core.service.interfaces.commontypes.ResearchAreaService;
import rs.teslaris.migrator.converter.hydrator.HydratorConversionUtil;
import rs.teslaris.migrator.converter.hydrator.HydratorInstitutionResolver;
import rs.teslaris.migrator.converter.hydrator.HydratorJournalResolver;
import rs.teslaris.migrator.converter.hydrator.HydratorOutputConverters;
import rs.teslaris.migrator.converter.hydrator.HydratorPersonResolver;
import rs.teslaris.migrator.model.hydrator.HydratorCVModel;
import rs.teslaris.migrator.util.MigrationLog;

@ExtendWith(MockitoExtension.class)
public class HydratorServiceConvertersTest {

    private static final ObjectMapper MAPPER = new ObjectMapper();

    private static final Integer OWNER_ID = 5;

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

        var ownerName = new PersonNameDTO();
        ownerName.setFirstname("John");
        ownerName.setLastname("Doe");
        lenient().when(personResolver.resolveOwner(any())).thenReturn(OWNER_ID);
        lenient().when(personResolver.ownerName(any())).thenReturn(ownerName);
        lenient().when(personResolver.resolveByCienciaId(any())).thenReturn(null);
    }

    private HydratorCVModel.Curriculum curriculum(String service) throws Exception {
        return MAPPER.readValue("""
            {"id": "cv-1", "fullName": "John Doe",
             "curriculum": {"language": "en", "services": {"total": 1, "service": [%s]}}}
            """.formatted(service), HydratorCVModel.Curriculum.class);
    }

    private HydratorCVModel.Service service(HydratorCVModel.Curriculum record) {
        return record.curriculum().services().service().getFirst();
    }

    @Test
    void shouldMapOralPresentationWithAuthorsWhenSourceHasAuthors() throws Exception {
        // given
        var record = curriculum("""
            {"id": "9", "nonResearchPresentation": {"title": "Test Talk",
              "presentationDate": {"year": "2019", "month": "5", "day": "14"},
              "conferenceEvent": "Test Event", "organizationPresentedTo": "Test School",
              "authors": {"total": 2, "author": [{"self": true, "name": "Doe, John"},
                {"name": "Smith, Jane"}]},
              "presenters": {"total": 1, "author": [{"self": true, "name": "Doe, John"}]},
              "invited": true, "keynote": false, "competitive": false}}
            """);

        // when
        var dto = converters.toNonResearchPresentation(record, service(record));

        // then
        assertEquals(PerformanceRelatedOutputType.NON_RESEARCH_PRESENTATION, dto.getType());
        assertEquals("Test Talk", dto.getTitle().getFirst().getContent());
        assertEquals(2019, dto.getDocumentDate().year());
        assertEquals(5, dto.getDocumentDate().month());
        assertEquals(14, dto.getDocumentDate().day());
        assertEquals(2, dto.getContributions().size());
        assertTrue(dto.getContributions().stream().allMatch(contribution ->
            DocumentContributionType.AUTHOR.equals(contribution.getContributionType())));
        assertEquals(OWNER_ID, dto.getContributions().getFirst().getPersonId());
        assertNull(dto.getContributions().get(1).getPersonId());
    }

    @Test
    void shouldAddOwnerAsAuthorWhenOralPresentationHasNoAuthors() throws Exception {
        // given
        var record = curriculum("""
            {"id": "9", "nonResearchPresentation": {"title": "Test Talk",
              "presentationDate": {"year": "2019"}}}
            """);

        // when
        var dto = converters.toNonResearchPresentation(record, service(record));

        // then
        assertEquals(1, dto.getContributions().size());
        assertEquals(DocumentContributionType.AUTHOR,
            dto.getContributions().getFirst().getContributionType());
        assertEquals(OWNER_ID, dto.getContributions().getFirst().getPersonId());
        verify(migrationLog).valueDefaulted(anyString(), eq("PERFORMANCE_RELATED_OUTPUT"),
            eq("cv-1#service#9"), eq("CW-001-MAP-000955"), anyString());
    }

    @Test
    void shouldLogKeynoteAndCompetitiveWhenSet() throws Exception {
        // given
        var record = curriculum("""
            {"id": "9", "nonResearchPresentation": {"title": "Test Talk",
              "presentationDate": {"year": "2019"}, "keynote": true, "competitive": true}}
            """);

        // when
        converters.toNonResearchPresentation(record, service(record));

        // then
        verify(migrationLog).valueDropped(anyString(), eq("PERFORMANCE_RELATED_OUTPUT"),
            eq("cv-1#service#9"), eq("CW-001-MAP-000953"), anyString());
        verify(migrationLog).valueDropped(anyString(), eq("PERFORMANCE_RELATED_OUTPUT"),
            eq("cv-1#service#9"), eq("CW-001-MAP-000954"), anyString());
    }

    @Test
    void shouldSkipOralPresentationWhenTitleIsBlank() throws Exception {
        // given
        var record = curriculum("""
            {"id": "9", "nonResearchPresentation": {"title": " ",
              "presentationDate": {"year": "2019"}}}
            """);

        // when
        var dto = converters.toNonResearchPresentation(record, service(record));

        // then
        assertNull(dto);
    }

    @Test
    void shouldMapPresentersToInvitedSpeakersWhenInvited() throws Exception {
        // given
        var record = curriculum("""
            {"id": "9", "nonResearchPresentation": {"title": "Test Talk",
              "presenters": {"total": 2, "author": [{"self": true, "name": "Doe, John"},
                {"name": "Smith, Jane"}]},
              "invited": true}}
            """);

        // when
        var speakers = converters.speakerContributions(record,
            service(record).nonResearchPresentation());

        // then
        assertEquals(2, speakers.size());
        assertTrue(speakers.stream().allMatch(speaker ->
            EventContributionType.SPEAKER.equals(speaker.getEventContributionType()) &&
                Boolean.TRUE.equals(speaker.getIsInvitedContributor())));
        assertEquals(OWNER_ID, speakers.getFirst().getPersonId());
        assertEquals(1, speakers.getFirst().getOrderNumber());
        assertEquals("Smith", speakers.get(1).getPersonName().getLastname());
        assertEquals(2, speakers.get(1).getOrderNumber());
    }

    @Test
    void shouldMapBroadcastInterviewWhenThemeIsPresent() throws Exception {
        // given
        var record = curriculum("""
            {"id": "3", "broadcastInterview": {"theme": "Test Theme",
              "firstBroadcastDate": {"year": "2020", "month": "3"},
              "program": "Test Program", "interviewerName": "Test Interviewer",
              "producer": "Test Producer", "url": "https://example.com/show",
              "keywords": {"total": 2, "keyword": ["alpha", "beta"]}}}
            """);

        // when
        var dto = converters.toBroadcastInterview(record, service(record));

        // then
        assertEquals(PerformanceRelatedOutputType.BROADCAST_INTERVIEW, dto.getType());
        assertEquals("Test Theme", dto.getTitle().getFirst().getContent());
        assertEquals(2020, dto.getDocumentDate().year());
        assertEquals(3, dto.getDocumentDate().month());
        assertEquals("Test Interviewer", dto.getOtherActors().getFirst().getContent());
        assertEquals("Test Producer", dto.getProducer().getFirst().getContent());
        assertEquals(Set.of("https://example.com/show"), dto.getUris());
        assertEquals("alpha\nbeta", dto.getKeywords().getFirst().getContent());
        assertTrue(dto.getSourceTitle().isEmpty());
        assertEquals(DocumentContributionType.AUTHOR,
            dto.getContributions().getFirst().getContributionType());
        assertEquals(OWNER_ID, dto.getContributions().getFirst().getPersonId());
    }

    @Test
    void shouldMapTextInterviewForumToDescriptionWhenForumIsPresent() throws Exception {
        // given
        var record = curriculum("""
            {"id": "4", "textInterview": {"topic": "Test Topic",
              "publicationDate": {"year": "2021"}, "forum": "Test Newspaper",
              "interviewerName": "Test Interviewer",
              "researchClassifications": {"total": 1,
                "researchClassification": [{"code": "1", "value": "Test Area"}]}}}
            """);

        // when
        var dto = converters.toTextInterview(record, service(record));

        // then
        assertEquals(PerformanceRelatedOutputType.TEXT_INTERVIEW, dto.getType());
        assertEquals("Test Topic", dto.getTitle().getFirst().getContent());
        assertEquals("Test Newspaper", dto.getDescription().getFirst().getContent());
        assertEquals("Test Interviewer", dto.getOtherActors().getFirst().getContent());
        assertTrue(dto.getSourceTitle().isEmpty());
        verify(migrationLog).valueDropped(anyString(), eq("PERFORMANCE_RELATED_OUTPUT"),
            eq("cv-1#service#4"), eq("CW-001-MAP-000860"), anyString());
    }
}
