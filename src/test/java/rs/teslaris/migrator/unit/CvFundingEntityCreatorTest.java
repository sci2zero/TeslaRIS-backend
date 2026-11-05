package rs.teslaris.migrator.unit;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.contains;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;
import rs.teslaris.core.dto.commontypes.MonetaryAmountDTO;
import rs.teslaris.core.dto.commontypes.MultilingualContentDTO;
import rs.teslaris.core.dto.funding.FundingDTO;
import rs.teslaris.core.dto.project.PersonProjectContributionDTO;
import rs.teslaris.core.dto.project.ProjectDTO;
import rs.teslaris.core.migration.FundingMigrationWriter;
import rs.teslaris.core.migration.ProjectMigrationSupplementer;
import rs.teslaris.core.migration.ProjectMigrationWriter;
import rs.teslaris.core.model.commontypes.Currency;
import rs.teslaris.core.model.project.OrganisationUnitProjectContributionType;
import rs.teslaris.core.model.project.PersonProjectContributionType;
import rs.teslaris.core.service.interfaces.commontypes.CurrencyService;
import rs.teslaris.core.service.interfaces.commontypes.LanguageService;
import rs.teslaris.core.service.interfaces.commontypes.LanguageTagService;
import rs.teslaris.core.service.interfaces.commontypes.ResearchAreaService;
import rs.teslaris.migrator.converter.hydrator.CvFundingEntityCreator;
import rs.teslaris.migrator.converter.hydrator.CvFundingMigrationDTO;
import rs.teslaris.migrator.converter.hydrator.HydratorConversionUtil;
import rs.teslaris.migrator.converter.hydrator.HydratorFundingReference;
import rs.teslaris.migrator.converter.hydrator.HydratorInstitutionResolver;
import rs.teslaris.migrator.converter.hydrator.HydratorPersonResolver;
import rs.teslaris.migrator.converter.hydrator.HydratorProjectResolver;
import rs.teslaris.migrator.converter.hydrator.ProjectMigrationDTO;
import rs.teslaris.migrator.model.MigrationRun;
import rs.teslaris.migrator.model.hydrator.HydratorCVModel;
import rs.teslaris.migrator.service.impl.MigrationIdResolver;
import rs.teslaris.migrator.util.InvalidSourceValueException;
import rs.teslaris.migrator.util.MigrationEntityType;
import rs.teslaris.migrator.util.MigrationException;
import rs.teslaris.migrator.util.MigrationLog;
import rs.teslaris.migrator.util.ResolvedToExistingException;

@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
public class CvFundingEntityCreatorTest {

    private static final HydratorCVModel.Institution UNIVERSITY =
        new HydratorCVModel.Institution("Test University", null, null, null);

    private static final HydratorCVModel.Institution AGENCY =
        new HydratorCVModel.Institution("Test Agency", null, null, null);

    private static final int OWNER_ID = 5;

    private static final MigrationRun RUN = MigrationRun.builder().id("run-1").build();

    @Mock
    private ProjectMigrationWriter projectMigrationWriter;

    @Mock
    private FundingMigrationWriter fundingMigrationWriter;

    @Mock
    private ProjectMigrationSupplementer projectSupplementer;

    @Mock
    private CurrencyService currencyService;

    @Mock
    private HydratorProjectResolver projectResolver;

    @Mock
    private HydratorInstitutionResolver institutionResolver;

    @Mock
    private HydratorPersonResolver personResolver;

    @Mock
    private MigrationIdResolver idResolver;

    @Mock
    private MigrationLog migrationLog;

    private CvFundingEntityCreator creator;


    @BeforeEach
    void setUp() {
        var conversionUtil = new HydratorConversionUtil(mock(LanguageTagService.class),
            mock(LanguageService.class), mock(ResearchAreaService.class));
        creator = new CvFundingEntityCreator(projectMigrationWriter, fundingMigrationWriter,
            projectSupplementer, currencyService, projectResolver, institutionResolver,
            personResolver, conversionUtil, idResolver, migrationLog);

        when(idResolver.require("hydrator", MigrationEntityType.PERSON, "cv-1"))
            .thenReturn(OWNER_ID);
        when(institutionResolver.resolve(any()))
            .thenReturn(new HydratorInstitutionResolver.Match(null, "identifier not found"));

        when(projectMigrationWriter.migrate(any(), any(), any())).thenReturn(
            new ProjectMigrationWriter.MigratedProject(42, Map.of(), true, Map.of()));
        when(fundingMigrationWriter.migrate(any(), any()))
            .thenReturn(new FundingMigrationWriter.MigratedFunding(77, true));
    }

    private static CvFundingMigrationDTO dto(String reference,
                                             List<ProjectMigrationDTO.TeamEntry> investigators,
                                             FundingDTO funding, String rejection) {
        var owner = new PersonProjectContributionDTO();
        owner.setContributionType(PersonProjectContributionType.TEAM_MEMBER);
        return new CvFundingMigrationDTO("cv-1", "3",
            Objects.isNull(reference) ? null : HydratorFundingReference.of(reference),
            new ProjectDTO(),
            List.of(new ProjectMigrationDTO.ConsortiumEntry(
                OrganisationUnitProjectContributionType.CONSORTIUM_MEMBER, UNIVERSITY, 1, null)),
            investigators, owner, funding, AGENCY, rejection);
    }

    private static FundingDTO funding() {
        var funding = new FundingDTO();
        funding.setDisplayFunder(new ArrayList<>());
        return funding;
    }

    private static ProjectMigrationDTO.TeamEntry investigator(String name, String cienciaId,
                                                              int order) {
        return new ProjectMigrationDTO.TeamEntry(PersonProjectContributionType.TEAM_MEMBER, name,
            cienciaId, null, order);
    }

    @Test
    void shouldCreateProjectAndFundingWhenNotFound() {
        var item = dto("PTDC/ABC/1234/2020", List.of(), funding(), null);
        when(projectResolver.resolve(item.reference())).thenReturn(
            new HydratorProjectResolver.Match(null, "no project", false));

        var id = creator.create(item, RUN);

        assertEquals(77, id);
        var project = item.project();
        assertEquals(1, project.getPersons().size());
        assertEquals(OWNER_ID, project.getPersons().getFirst().getPersonId());
        assertEquals(1, project.getPersons().getFirst().getOrderNumber());
        assertEquals("Test University",
            project.getOrganisations().getFirst().getDisplayOrganisationUnit().getFirst()
                .getContent());
        assertEquals(42, item.funding().getProjectId());
        verify(projectSupplementer, never()).supplement(any(), any(), any());
    }

    @Test
    void shouldNotLookUpWeakReference() {
        var item = dto(null, List.of(), funding(), null);

        assertEquals(77, creator.create(item, RUN));
        verify(projectResolver, never()).resolve(any());
    }

    @Test
    void shouldSupplementFoundProjectAndReportResolved() {
        var item = dto("PTDC/ABC/1234/2020", List.of(), funding(), null);
        when(projectResolver.resolve(item.reference())).thenReturn(
            new HydratorProjectResolver.Match(9, null, false));
        when(projectSupplementer.supplement(eq(9), any(), any()))
            .thenReturn(List.of("investigation role RESEARCHER not set"));

        var exception = assertThrows(ResolvedToExistingException.class,
            () -> creator.create(item, RUN));

        assertNull(exception.getEntityId());
        verify(projectMigrationWriter, never()).migrate(any(), any(), any());
        verify(fundingMigrationWriter, never()).migrate(any(), any());
        verify(migrationLog).valueDropped(anyString(), eq("CV_FUNDING"), eq("cv-1#funding#3"),
            eq("CW-001-MAP-000017"), eq("investigation role RESEARCHER not set"));
    }

    @Test
    void shouldSkipItemWhenReferenceMatchesSeveralProjects() {
        var item = dto("PTDC/ABC/1234/2020", List.of(), funding(), null);
        when(projectResolver.resolve(item.reference())).thenReturn(
            new HydratorProjectResolver.Match(null, "ambiguous reference", true));

        assertThrows(MigrationException.class, () -> creator.create(item, RUN));

        verify(projectMigrationWriter, never()).migrate(any(), any(), any());
        verify(projectSupplementer, never()).supplement(any(), any(), any());
        verify(migrationLog).valueDropped(anyString(), eq("CV_FUNDING"), eq("cv-1#funding#3"),
            eq("CW-001-MAP-000005"), eq("ambiguous reference"));
    }

    @Test
    void shouldAddInvestigatorsWithoutOwnerAndDuplicates() {
        when(personResolver.resolve("1A2B-3C4D-5E6F", null))
            .thenReturn(new HydratorPersonResolver.Match(OWNER_ID, null));
        when(personResolver.resolve("2A2B-3C4D-5E6F", null))
            .thenReturn(new HydratorPersonResolver.Match(8, null));
        when(personResolver.resolve("9A9B-9C9D-9E9F", null))
            .thenReturn(new HydratorPersonResolver.Match(null, "person not found"));
        var item = dto(null, List.of(
            investigator("Test Owner", "1A2B-3C4D-5E6F", 1),
            investigator("Test Person", "2A2B-3C4D-5E6F", 2),
            investigator("Jane Roe", null, 3),
            investigator(" jane  roe", null, 4),
            investigator("Test Person", "2A2B-3C4D-5E6F", 5),
            investigator("Unknown Person", "9A9B-9C9D-9E9F", 6)), funding(), null);

        creator.create(item, RUN);

        var persons = item.project().getPersons();
        assertEquals(4, persons.size());
        assertEquals(OWNER_ID, persons.get(0).getPersonId());
        assertEquals(8, persons.get(1).getPersonId());
        assertEquals("Jane Roe", persons.get(2).getPersonName().getLastname());
        assertNull(persons.get(3).getPersonId());
        assertEquals(4, persons.get(3).getOrderNumber());
        verify(migrationLog).valueDropped(anyString(), eq("CV_FUNDING"), eq("cv-1#funding#3"),
            eq("CW-001-MAP-000016"), contains("1 investigator(s) not linked"));
    }

    @Test
    void shouldKeepUnlinkedFunderAsDisplayFunderOnlyWithoutProgramme() {
        var withoutProgramme = dto(null, List.of(), funding(), null);
        creator.create(withoutProgramme, RUN);
        assertEquals("Test Agency",
            withoutProgramme.funding().getDisplayFunder().getFirst().getContent());

        var withProgramme = funding();
        withProgramme.setDisplayFunder(new ArrayList<>(List.of(
            new MultilingualContentDTO(null, null, "Test Programme", 1))));
        creator.create(dto(null, List.of(), withProgramme, null), RUN);
        assertEquals("Test Programme", withProgramme.getDisplayFunder().getFirst().getContent());
        verify(migrationLog).valueDropped(anyString(), eq("CV_FUNDING"), anyString(),
            eq("CW-001-MAP-000002"), contains("not kept"));
    }

    @Test
    void shouldDropAmountWithUnknownCurrency() {
        var funding = funding();
        var amount = new MonetaryAmountDTO();
        amount.setAmount(100);
        amount.setCurrencyCode("XXX");
        funding.setAmount(amount);

        creator.create(dto(null, List.of(), funding, null), RUN);

        assertNull(funding.getAmount());
        verify(migrationLog).valueDropped(anyString(), eq("CV_FUNDING"), anyString(),
            eq("CW-001-NO-RULE"), contains("'XXX'"));
    }

    @Test
    void shouldResolveKnownCurrency() {
        var currency = new Currency();
        currency.setId(2);
        when(currencyService.findCurrencyByCode("EUR")).thenReturn(currency);
        var funding = funding();
        var amount = new MonetaryAmountDTO();
        amount.setAmount(100);
        amount.setCurrencyCode("EUR");
        funding.setAmount(amount);

        creator.create(dto(null, List.of(), funding, null), RUN);

        assertEquals(2, funding.getAmount().getCurrencyId());
    }

    @Test
    void shouldFailItemWithInvalidSourceValue() {
        assertThrows(InvalidSourceValueException.class,
            () -> creator.create(dto(null, List.of(), funding(), "invalid date 'x'"), RUN));
        verify(projectMigrationWriter, never()).migrate(any(), any(), any());
    }
}
