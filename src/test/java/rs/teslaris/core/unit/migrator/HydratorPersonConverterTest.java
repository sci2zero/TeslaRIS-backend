package rs.teslaris.core.unit.migrator;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
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
import rs.teslaris.core.dto.person.PersonNameDTO;
import rs.teslaris.core.model.commontypes.Language;
import rs.teslaris.core.model.person.LanguageLevel;
import rs.teslaris.core.model.person.PersonNameType;
import rs.teslaris.core.model.person.Sex;
import rs.teslaris.core.service.interfaces.commontypes.LanguageService;
import rs.teslaris.core.service.interfaces.commontypes.LanguageTagService;
import rs.teslaris.core.service.interfaces.commontypes.ResearchAreaService;
import rs.teslaris.core.util.exceptionhandling.exception.NotFoundException;
import rs.teslaris.core.util.functional.Pair;
import rs.teslaris.migrator.converter.hydrator.HydratorConversionUtil;
import rs.teslaris.migrator.converter.hydrator.HydratorPersonConverter;
import rs.teslaris.migrator.model.hydrator.HydratorCVModel;
import rs.teslaris.migrator.util.MigrationLog;

@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
public class HydratorPersonConverterTest {

    private static final ObjectMapper MAPPER = new ObjectMapper();

    @Mock
    private LanguageTagService languageTagService;

    @Mock
    private LanguageService languageService;

    @Mock
    private ResearchAreaService researchAreaService;

    @Mock
    private MigrationLog migrationLog;

    private HydratorPersonConverter converter;


    @BeforeEach
    void setUp() {
        var english = new Language();
        english.setId(1);
        var french = new Language();
        french.setId(4);
        when(languageService.findLanguageByCode("EN")).thenReturn(english);
        when(languageService.findLanguageByCode("FR")).thenReturn(french);
        when(languageService.findLanguageByCode("RU")).thenThrow(
            new NotFoundException("Language with given code does not exist."));

        when(researchAreaService.getResearchAreaNames("EN")).thenReturn(List.of(
            new Pair<>(10, "social sciences"),
            new Pair<>(11, "psychology"),
            new Pair<>(20, "natural sciences"),
            new Pair<>(21, "computer and information sciences"),
            new Pair<>(30, "agricultural sciences")));

        converter = new HydratorPersonConverter(
            new HydratorConversionUtil(languageTagService, languageService, researchAreaService),
            migrationLog);
    }

    private HydratorCVModel.Curriculum curriculum(String identifyingInfo) throws Exception {
        return MAPPER.readValue("""
            {"id": "cv-1", "fullName": "John Michael Doe Smith",
             "curriculum": {"language": "pt_PT", "identifyingInfo": %s}}
            """.formatted(identifyingInfo), HydratorCVModel.Curriculum.class);
    }

    @Test
    void shouldBuildDefaultNameFromNamesAndSurnamesAndKeepOtherFormsByType() throws Exception {
        // given
        var record = curriculum("""
            {"personInfo": {"fullName": "John Michael Doe Smith",
                            "displayName": "John Smith",
                            "names": "John Michael", "surnames": "Doe Smith"},
             "citationNames": {"total": 2, "citationName": [
                {"privacyLevel": "publico", "preferredCitationName": "false",
                 "value": "Smith, J."},
                {"privacyLevel": "publico", "preferredCitationName": "true",
                 "value": "Smith, John"}]}}
            """);

        // when
        var dto = converter.toDTO(record);

        // then
        assertEquals("CIENCIA_VITAE", dto.getImportSource());
        assertEquals("John Michael", dto.getPersonName().getFirstname());
        assertEquals("Doe Smith", dto.getPersonName().getLastname());

        // full name repeats the default name and "Smith, John" the display name, so both go
        var otherNames = dto.getOtherNames();
        assertEquals(2, otherNames.size());
        assertName(otherNames.get(0), "John", "Smith", PersonNameType.DISPLAY_NAME);
        assertName(otherNames.get(1), "J.", "Smith", PersonNameType.CITATION_NAME);
    }

    @Test
    void shouldFallBackToFullNameWhenNamesAreMissing() throws Exception {
        // given
        var record = curriculum("""
            {"personInfo": {"fullName": "Jane Anne Test Doe"}}
            """);

        // when
        var dto = converter.toDTO(record);

        // then
        assertEquals("Jane Anne Test", dto.getPersonName().getFirstname());
        assertEquals("Doe", dto.getPersonName().getLastname());
        assertTrue(dto.getOtherNames().isEmpty());
    }

    @Test
    void shouldMapPublicBirthDateAndGender() throws Exception {
        // given
        var record = curriculum("""
            {"personInfo": {"names": "John", "surnames": "Doe",
                            "dateOfBirth": {"privacyLevel": "publico", "year": "1987",
                                            "month": "07", "day": "11"},
                            "gender": {"privacyLevel": "publico", "code": "F",
                                       "value": "Female"}}}
            """);

        // when
        var dto = converter.toDTO(record);

        // then
        assertEquals(LocalDate.of(1987, 7, 11), dto.getLocalBirthDate());
        assertEquals(Sex.FEMALE, dto.getSex());
    }

    @Test
    void shouldIgnorePrivateBirthDate() throws Exception {
        // given
        var record = curriculum("""
            {"personInfo": {"names": "John", "surnames": "Doe",
                            "dateOfBirth": {"privacyLevel": "privado"},
                            "gender": {"privacyLevel": "privado"}}}
            """);

        // when
        var dto = converter.toDTO(record);

        // then
        assertNull(dto.getLocalBirthDate());
        assertNull(dto.getSex());
    }

    @Test
    void shouldRouteContactsByType() throws Exception {
        // given
        var record = curriculum("""
            {"personInfo": {"names": "John", "surnames": "Doe"},
             "emails": {"total": 2, "email": [
                {"emailAddress": "john.doe@example.com", "preferredEmail": "true",
                 "emailType": {"code": "I", "value": "Professional"}},
                {"emailAddress": "john@example.org",
                 "emailType": {"code": "R", "value": "Personal"}}]},
             "phoneNumbers": {"total": 2, "phoneNumber": [
                {"countryCode": "351", "localNumber": "900000001",
                 "phoneType": {"code": "2"}, "usageType": {"code": "R"}},
                {"localNumber": "200000002", "extension": "12",
                 "phoneType": {"code": "1"}, "usageType": {"code": "I"}}]},
             "mailingAddresses": {"total": 1, "mailingAddress": [
                {"id": "1", "streetAddress": "Test Street 1", "city": "Test City",
                 "postalCode": "0000-001", "provinceState": "Test Region",
                 "country": {"code": "PT", "name": "Portugal"},
                 "addressType": {"code": "R"}}]},
             "webAddresses": {"total": 1, "webAddress": [
                {"url": "www.example.com", "siteType": {"code": "5", "value": "Professional"}}]}}
            """);

        // when
        var dto = converter.toDTO(record);

        // then
        assertEquals("john.doe@example.com", dto.getContactEmail());
        assertEquals("john@example.org", dto.getPrivateContactEmail());
        assertEquals("+351 900000001", dto.getPrivateMobilePhoneNumber());
        assertEquals("200000002 ext. 12", dto.getPhoneNumber());
        assertEquals("Test Street 1", dto.getPrivateAddressLine().getFirst().getContent());
        assertEquals("PT", dto.getPrivateAddressCity().getFirst().getLanguageTag());
        assertEquals("0000-001", dto.getPrivatePostalNumber());
        assertEquals("PT", dto.getPrivateCountryCode());
        assertNull(dto.getCountryCode());
        assertTrue(dto.getAddressLine().isEmpty());
        assertEquals(Set.of("https://www.example.com"), dto.getUris());
        verify(migrationLog).valueDropped(anyString(), anyString(), eq("cv-1"),
            eq("MAP-000033"), anyString());
    }

    @Test
    void shouldRouteIdentifiersByTypeAndCanonicaliseThem() throws Exception {
        // given
        var record = curriculum("""
            {"personInfo": {"names": "John", "surnames": "Doe"},
             "authorIdentifiers": {"total": 5, "authorIdentifier": [
                {"identifierType": {"code": "CIENCIAID"}, "identifier": "AAAA-BBBB-CCCC"},
                {"identifierType": {"code": "ORCID"},
                 "identifier": "https://orcid.org/0000-0002-1825-0097"},
                {"identifierType": {"code": "SCOPUS"}, "identifier": "12345678900"},
                {"identifierType": {"code": "GOOGLE"},
                 "identifier": "https://scholar.google.pt/citations?user=AbCdEfGhIjKl&hl=pt-PT"},
                {"identifierType": {"code": "WOS"}, "identifier": " a-1234-2010"}]}}
            """);

        // when
        var dto = converter.toDTO(record);

        // then
        assertEquals("AAAA-BBBB-CCCC", dto.getNationalScienceId());
        assertEquals("0000-0002-1825-0097", dto.getOrcid());
        assertEquals("12345678900", dto.getScopusAuthorId());
        assertEquals("AbCdEfGhIjKl", dto.getScholarId());
        assertEquals("A-1234-2010", dto.getWebOfScienceResearcherId());
        verify(migrationLog, never()).valueDropped(anyString(), anyString(), anyString(),
            eq("MAP-000037"), anyString());
    }

    @Test
    void shouldPreferValidIdentifierOverInvalidOneOfSameType() throws Exception {
        // given
        var record = curriculum("""
            {"personInfo": {"names": "John", "surnames": "Doe"},
             "authorIdentifiers": {"total": 2, "authorIdentifier": [
                {"identifierType": {"code": "ORCID"}, "identifier": "not-an-orcid"},
                {"identifierType": {"code": "ORCID"}, "identifier": "0000-0002-1825-0097"}]}}
            """);

        // when
        var dto = converter.toDTO(record);

        // then
        assertEquals("0000-0002-1825-0097", dto.getOrcid());
        verify(migrationLog).valueDropped(anyString(), anyString(), eq("cv-1"),
            eq("MAP-000037"), anyString());
    }

    @Test
    void shouldPassInvalidIdentifierOnSoTheImportFailsThePerson() throws Exception {
        // given
        var record = curriculum("""
            {"personInfo": {"names": "John", "surnames": "Doe"},
             "authorIdentifiers": {"total": 1, "authorIdentifier": [
                {"identifierType": {"code": "WOS"},
                 "identifier": "https://www.researchgate.net/profile/X"}]}}
            """);

        // when
        var dto = converter.toDTO(record);

        // then
        assertEquals("HTTPS://WWW.RESEARCHGATE.NET/PROFILE/X", dto.getWebOfScienceResearcherId());
    }

    @Test
    void shouldRankCompetingValuesForOneSlot() throws Exception {
        // given
        var record = curriculum("""
            {"personInfo": {"names": "John", "surnames": "Doe"},
             "emails": {"total": 3, "email": [
                {"emailAddress": "broken-address", "emailType": {"code": "I"}},
                {"emailAddress": "old@example.com", "lastModifiedDate": "2019-01-01T10:00:00",
                 "emailType": {"code": "I"}},
                {"emailAddress": "new@example.com", "lastModifiedDate": "2024-05-01T10:00:00",
                 "emailType": {"code": "I"}}]},
             "phoneNumbers": {"total": 2, "phoneNumber": [
                {"localNumber": "900000001", "phoneType": {"code": "2"},
                 "usageType": {"code": "I"}},
                {"countryCode": "351", "localNumber": "900000003",
                 "phoneType": {"code": "2"}, "usageType": {"code": "I"}}]},
             "mailingAddresses": {"total": 2, "mailingAddress": [
                {"id": "1", "city": "Test City", "addressType": {"code": "I"}},
                {"id": "2", "streetAddress": "Test Street 2", "city": "Other City",
                 "postalCode": "0000-002", "addressType": {"code": "I"}}]}}
            """);

        // when
        var dto = converter.toDTO(record);

        // then
        // valid over invalid, then most recently updated
        assertEquals("new@example.com", dto.getContactEmail());
        // more complete (with country code) wins
        assertEquals("+351 900000003", dto.getMobilePhoneNumber());
        // more complete address wins
        assertEquals("Other City", dto.getAddressCity().getFirst().getContent());
        verify(migrationLog, times(2)).valueDropped(anyString(), anyString(), eq("cv-1"),
            eq("MAP-000030"), anyString());
        verify(migrationLog).valueDropped(anyString(), anyString(), eq("cv-1"),
            eq("MAP-000031"), anyString());
        verify(migrationLog).valueDropped(anyString(), anyString(), eq("cv-1"),
            eq("MAP-000032"), anyString());
    }

    @Test
    void shouldLetPreferredValueWinOverMoreRecentOne() throws Exception {
        // given
        var record = curriculum("""
            {"personInfo": {"names": "John", "surnames": "Doe"},
             "emails": {"total": 2, "email": [
                {"emailAddress": "new@example.com", "lastModifiedDate": "2024-05-01T10:00:00",
                 "emailType": {"code": "I"}},
                {"emailAddress": "main@example.com", "preferredEmail": "true",
                 "lastModifiedDate": "2019-01-01T10:00:00", "emailType": {"code": "I"}}]}}
            """);

        // when
        var dto = converter.toDTO(record);

        // then
        assertEquals("main@example.com", dto.getContactEmail());
    }

    @Test
    void shouldMapLanguageCompetenciesToLanguageKnowledge() throws Exception {
        // given
        var record = curriculum("""
            {"personInfo": {"names": "John", "surnames": "Doe"},
             "languageCompetencies": {"total": 4, "languageCompetency": [
                {"language": {"code": "ENG", "value": "English"}, "motherTongue": "false",
                 "read": {"code": "C2"}, "write": {"code": "C1"}, "speak": {"code": "B2"},
                 "understandSpoken": {"code": "C1"}, "peerReview": {"code": "B1"}},
                {"language": {"code": "FRA/FRE", "value": "French"}, "motherTongue": "true"},
                {"language": {"code": "CAT", "value": "Catalan"}},
                {"language": {"code": "RUS", "value": "Russian"}}]}}
            """);

        // when
        var languages = converter.toDTO(record).getLanguageKnowledges();

        // then
        assertEquals(2, languages.size());

        var english = languages.get(0);
        assertEquals(1, english.getLanguageId());
        assertEquals(false, english.getMotherTongue());
        assertEquals(LanguageLevel.C2, english.getRead());
        assertEquals(LanguageLevel.C1, english.getWrite());
        assertEquals(LanguageLevel.B2, english.getSpeak());
        assertEquals(LanguageLevel.C1, english.getUnderstandSpoken());
        assertEquals(LanguageLevel.B1, english.getPeerReview());

        var french = languages.get(1);
        assertEquals(4, french.getLanguageId());
        assertEquals(true, french.getMotherTongue());
        assertNull(french.getRead());

        // Catalan has no registry mapping, Russian is mapped but missing from the registry
        verify(migrationLog, times(2)).valueDropped(anyString(), anyString(), eq("cv-1"),
            eq("MAP-000034"), anyString());
    }

    @Test
    void shouldMatchResearchClassificationsByName() throws Exception {
        // given
        var record = curriculum("""
            {"personInfo": {"names": "John", "surnames": "Doe"},
             "domainActivities": {"total": 5, "domainActivity": [
                {"researchClassification": {"code": "50100009",
                                            "value": "Social Sciences - Psychology"}},
                {"researchClassification": {"code": "70201000", "value":
                 "Exact Sciences - Computer and Information Sciences - Computer Sciences"}},
                {"researchClassification": {"code": "40000000",
                                            "value": "Agrarian Sciences"}},
                {"researchClassification": {"code": "50100009",
                                            "value": "Social Sciences - Psychology"}},
                {"researchClassification": {"code": "99", "value": "Unknown Field"}}]}}
            """);

        // when
        var researchAreas = converter.toDTO(record).getResearchAreasId();

        // then
        // exact, broader (Computer Sciences is not in EuroSciVoc), synonym; duplicates merged
        assertEquals(Set.of(11, 21, 30), researchAreas);
        // one broader fallback and one unknown field are logged
        verify(migrationLog, times(2)).valueDropped(anyString(), anyString(), eq("cv-1"),
            eq("MAP-000035"), anyString());
    }

    @Test
    void shouldJoinDomainKeywords() throws Exception {
        // given
        var record = curriculum("""
            {"personInfo": {"names": "John", "surnames": "Doe"},
             "domainActivities": {"total": 2, "domainActivity": [
                {"keywords": {"total": 2, "keyword": ["tourism", "heritage"]}},
                {"keywords": {"total": 1, "keyword": ["tourism"]}}]}}
            """);

        // when
        var dto = converter.toDTO(record);

        // then
        assertEquals(List.of("tourism\nheritage"),
            dto.getKeywords().stream().map(keyword -> keyword.getContent()).toList());
    }

    @Test
    void shouldSkipRecordWithoutAnyName() throws Exception {
        // given
        var record = MAPPER.readValue("""
            {"id": "cv-2", "curriculum": {"identifyingInfo": {"personInfo": {}}}}
            """, HydratorCVModel.Curriculum.class);

        // when / then
        assertNull(converter.toDTO(record));
    }

    private void assertName(PersonNameDTO name, String firstname, String lastname,
                            PersonNameType type) {
        assertEquals(firstname, name.getFirstname());
        assertEquals(lastname, name.getLastname());
        assertEquals(type, name.getPersonNameType());
    }
}
