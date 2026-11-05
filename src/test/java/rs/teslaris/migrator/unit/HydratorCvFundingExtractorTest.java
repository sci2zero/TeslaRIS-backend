package rs.teslaris.migrator.unit;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.contains;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;

import com.fasterxml.jackson.databind.ObjectMapper;
import java.time.LocalDate;
import java.util.Set;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import rs.teslaris.core.model.funding.FundingType;
import rs.teslaris.core.model.project.OrganisationUnitProjectContributionType;
import rs.teslaris.core.model.project.PersonProjectContributionType;
import rs.teslaris.core.model.project.PersonProjectInvestigationRole;
import rs.teslaris.core.model.project.ProjectStatus;
import rs.teslaris.core.service.interfaces.commontypes.LanguageService;
import rs.teslaris.core.service.interfaces.commontypes.LanguageTagService;
import rs.teslaris.core.service.interfaces.commontypes.ResearchAreaService;
import rs.teslaris.migrator.converter.hydrator.CvFundingMigrationDTO;
import rs.teslaris.migrator.converter.hydrator.HydratorConversionUtil;
import rs.teslaris.migrator.converter.hydrator.HydratorCvFundingExtractor;
import rs.teslaris.migrator.converter.hydrator.HydratorFundingReference;
import rs.teslaris.migrator.model.hydrator.HydratorCVModel;
import rs.teslaris.migrator.util.MigrationLog;

@ExtendWith(MockitoExtension.class)
public class HydratorCvFundingExtractorTest {

    private static final ObjectMapper MAPPER = new ObjectMapper();

    @Mock
    private LanguageTagService languageTagService;

    @Mock
    private LanguageService languageService;

    @Mock
    private ResearchAreaService researchAreaService;

    @Mock
    private MigrationLog migrationLog;

    private HydratorCvFundingExtractor extractor;


    @BeforeEach
    void setUp() {
        extractor = new HydratorCvFundingExtractor(
            new HydratorConversionUtil(languageTagService, languageService, researchAreaService),
            migrationLog);
    }

    private HydratorCVModel.Curriculum curriculum(String fundings) throws Exception {
        return MAPPER.readValue("""
            {"id": "cv-1", "fullName": "John Doe",
             "curriculum": {"language": "en", "fundings": %s}}
            """.formatted(fundings), HydratorCVModel.Curriculum.class);
    }

    private CvFundingMigrationDTO single(String funding) throws Exception {
        var result = extractor.extract(curriculum("{\"funding\": [" + funding + "]}"));
        assertEquals(1, result.size());
        return result.getFirst();
    }

    @Test
    void shouldMapFundingWithProjectAndOwner() throws Exception {
        var item = single("""
            {"id": "5", "fundingCategory": {"code": "B", "value": "Grant"},
             "projectTitle": "Test Project", "projectDescription": "Test description",
             "startDate": {"year": "2018", "month": "01", "day": "01"},
             "endDate": {"year": "2021", "month": "12", "day": "31"},
             "startDateParticipation": {"year": "2019", "month": "03"},
             "endDateParticipation": {"year": "2020"},
             "status": {"code": "C", "status": "Concluded"},
             "investigationRole": {"code": "BI", "role": "Research Fellow"},
             "programName": "Test Programme", "yearAwarded": "2017",
             "totalAmount": {"currency": "eur", "amount": "1500.5"},
             "fundingIdentifiers": {"fundingIdentifier": [
               {"reference": "123"}, {"reference": "PTDC/ABC-XYZ/1234/2017"}]},
             "keywords": {"keyword": ["alpha", " beta ", "alpha"]}}
            """);

        assertEquals("cv-1", item.personSourceKey());
        assertEquals("cv-1#funding#5", item.sourceKey());
        assertNull(item.rejection());

        assertEquals(HydratorFundingReference.Kind.FCT, item.reference().kind());
        var project = item.project();
        assertEquals("Test Project", project.getName().getFirst().getContent());
        assertEquals("Test description", project.getDescription().getFirst().getContent());
        assertEquals("alpha\nbeta", project.getKeywords().getFirst().getContent());
        assertEquals(LocalDate.of(2018, 1, 1), project.getDateFrom());
        assertEquals(LocalDate.of(2021, 12, 31), project.getDateTo());
        assertEquals(ProjectStatus.CONCLUDED, project.getStatus());
        assertEquals("PTDC/ABC-XYZ/1234/2017", project.getNationalId());
        assertEquals(Set.of("PTDC/ABC-XYZ/1234/2017"), project.getInternalIdentifiers());
        assertEquals(Set.of("cv-1#funding#5"), project.getOldIds());

        var owner = item.owner();
        assertEquals(PersonProjectContributionType.TEAM_MEMBER, owner.getContributionType());
        assertEquals(PersonProjectInvestigationRole.RESEARCH_FELLOW, owner.getInvestigationRole());
        assertEquals(LocalDate.of(2019, 3, 1), owner.getDateFrom());
        assertEquals(LocalDate.of(2020, 1, 1), owner.getDateTo());

        var funding = item.funding();
        assertTrue(funding.getName().isEmpty());
        assertEquals(Set.of(FundingType.GRANT), funding.getFundingTypes());
        assertEquals("Test Programme", funding.getDisplayFunder().getFirst().getContent());
        assertEquals(LocalDate.of(2017, 1, 1), funding.getDateAwarded());
        assertEquals(1500.5, funding.getAmount().getAmount());
        assertEquals("EUR", funding.getAmount().getCurrencyCode());
        assertEquals(Set.of("123", "PTDC/ABC-XYZ/1234/2017"), funding.getInternalIdentifiers());

        verify(migrationLog, never()).valueDropped(anyString(), anyString(), anyString(),
            any(), anyString());
    }

    @Test
    void shouldMapPrincipalInvestigatorAsContributionType() throws Exception {
        var item = single("""
            {"id": "1", "projectTitle": "Test Project",
             "investigationRole": {"code": "IR", "role": "Principal investigator"}}
            """);

        assertEquals(PersonProjectContributionType.PRINCIPLE_INVESTIGATOR,
            item.owner().getContributionType());
        assertNull(item.owner().getInvestigationRole());
    }

    @ParameterizedTest
    @CsvSource({"I,RESEARCHER", "D,PHD_STUDENT_FELLOW", "OT,OTHER", "O,SUPERVISOR",
        "BGC,SCIENCE_AND_TECHNOLOGY_MANAGEMENT_FELLOW"})
    void shouldMapInvestigationRole(String code, PersonProjectInvestigationRole role)
        throws Exception {
        var item = single("""
            {"id": "1", "projectTitle": "Test Project", "investigationRole": {"code": "%s"}}
            """.formatted(code));

        assertEquals(PersonProjectContributionType.TEAM_MEMBER,
            item.owner().getContributionType());
        assertEquals(role, item.owner().getInvestigationRole());
    }

    @Test
    void shouldDropUnknownInvestigationRole() throws Exception {
        var item = single("""
            {"id": "1", "projectTitle": "Test Project", "investigationRole": {"code": "XX"}}
            """);

        assertNull(item.owner().getInvestigationRole());
        verify(migrationLog).valueDropped(anyString(), eq("CV_FUNDING"), eq("cv-1#funding#1"),
            eq("CW-001-MAP-000017"), contains("XX"));
    }

    @Test
    void shouldMapContractCategory() throws Exception {
        var item = single("""
            {"id": "1", "projectTitle": "Test Project", "fundingCategory": {"code": "C"}}
            """);

        assertEquals(Set.of(FundingType.CONTRACT), item.funding().getFundingTypes());
    }

    @Test
    void shouldMapCompetitiveAndRenewable() throws Exception {
        var item = single("""
            {"id": "1", "projectTitle": "Test Project", "competitive": "true",
             "fundingRenewable": "False"}
            """);

        assertEquals(Boolean.TRUE, item.funding().getCompetitive());
        assertEquals(Boolean.FALSE, item.funding().getRenewable());
    }

    @Test
    void shouldDropInvalidCompetitiveValue() throws Exception {
        var item = single("""
            {"id": "1", "projectTitle": "Test Project", "competitive": "yes"}
            """);

        assertNull(item.funding().getCompetitive());
        verify(migrationLog).valueDropped(anyString(), eq("CV_FUNDING"), eq("cv-1#funding#1"),
            eq("CW-001-NO-RULE"), contains("'yes'"));
    }

    @Test
    void shouldDeriveStatusFromDatesWhenMissing() throws Exception {
        var item = single("""
            {"id": "1", "projectTitle": "Test Project",
             "startDate": {"year": "2001"}, "endDate": {"year": "2003"}}
            """);

        assertEquals(ProjectStatus.CONCLUDED, item.project().getStatus());
    }

    @Test
    void shouldKeepWeakReferencesOnFundingOnly() throws Exception {
        var item = single("""
            {"id": "1", "projectTitle": "Test Project",
             "fundingIdentifiers": {"fundingIdentifier": [{"reference": " 123456 "},
               {"reference": ""}]}}
            """);

        assertNull(item.reference());
        assertNull(item.project().getNationalId());
        assertTrue(item.project().getInternalIdentifiers().isEmpty());
        assertEquals(Set.of("123456"), item.funding().getInternalIdentifiers());
    }

    @Test
    void shouldKeepQrenOnProjectWithoutNationalId() throws Exception {
        var item = single("""
            {"id": "1", "projectTitle": "Test Project",
             "fundingIdentifiers": {"fundingIdentifier": [
               {"reference": "POCI-01-0145-FEDER-000001"}]}}
            """);

        assertEquals(HydratorFundingReference.Kind.QREN, item.reference().kind());
        assertNull(item.project().getNationalId());
        assertEquals(Set.of("POCI-01-0145-FEDER-000001"),
            item.project().getInternalIdentifiers());
    }

    @Test
    void shouldMapInvestigatorsConsortiumAndFunder() throws Exception {
        var item = single("""
            {"id": "1", "projectTitle": "Test Project",
             "investigators": {"investigator": [
               {"name": "Jane Roe"}, {"name": "Test Person", "cienciaId": " 1A2B-3C4D-5E6F "},
               {"name": " "}]},
             "institutions": {"institution": [{"name": "Test University"},
               {"name": "Test Institute"}]},
             "fundingInstitutions": {"institution": [{"name": "Test Agency"},
               {"name": "Other Agency"}]}}
            """);

        assertEquals(2, item.investigators().size());
        assertEquals("Jane Roe", item.investigators().get(0).name());
        assertNull(item.investigators().get(0).cienciaId());
        assertEquals("1A2B-3C4D-5E6F", item.investigators().get(1).cienciaId());
        assertEquals(2, item.investigators().get(1).orderNumber());

        assertEquals(2, item.consortium().size());
        assertEquals(OrganisationUnitProjectContributionType.CONSORTIUM_MEMBER,
            item.consortium().getFirst().type());
        assertEquals("Test Institute", item.consortium().get(1).institution().name());

        assertEquals("Test Agency", item.funder().name());
        verify(migrationLog).valueDropped(anyString(), eq("CV_FUNDING"), eq("cv-1#funding#1"),
            eq("CW-001-MAP-000016"), anyString());
        verify(migrationLog).valueDropped(anyString(), eq("CV_FUNDING"), eq("cv-1#funding#1"),
            eq("CW-001-MAP-000002"), contains("1 further"));
    }

    @Test
    void shouldDropCurrencyWithoutAmount() throws Exception {
        var item = single("""
            {"id": "1", "projectTitle": "Test Project", "totalAmount": {"currency": "EUR"}}
            """);

        assertNull(item.funding().getAmount());
        assertNull(item.rejection());
        verify(migrationLog).valueDropped(anyString(), eq("CV_FUNDING"), eq("cv-1#funding#1"),
            eq("CW-001-NO-RULE"), contains("'EUR'"));
    }

    @Test
    void shouldRejectInvalidSourceValues() throws Exception {
        assertNotNull(single("""
            {"id": "1", "projectTitle": "Test Project", "totalAmount": {"amount": "1,5"}}
            """).rejection());
        assertNotNull(single("""
            {"id": "1", "projectTitle": "Test Project", "yearAwarded": "n/a"}
            """).rejection());
        assertNotNull(single("""
            {"id": "1", "projectTitle": "Test Project",
             "startDateParticipation": {"year": "2020", "month": "13"}}
            """).rejection());
    }

    @Test
    void shouldSkipFundingWithoutIdOrTitle() throws Exception {
        var result = extractor.extract(curriculum("""
            {"funding": [{"projectTitle": "Test Project"}, {"id": "2"}]}
            """));

        assertTrue(result.isEmpty());
    }

    @Test
    void shouldReturnNothingWithoutFundings() throws Exception {
        assertTrue(extractor.extract(curriculum("null")).isEmpty());
    }
}
