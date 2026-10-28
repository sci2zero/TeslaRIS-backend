package rs.teslaris.migrator.unit;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;
import rs.teslaris.core.dto.commontypes.MonetaryAmountDTO;
import rs.teslaris.core.model.commontypes.Currency;
import rs.teslaris.core.service.interfaces.commontypes.CurrencyService;
import rs.teslaris.core.service.interfaces.commontypes.LanguageService;
import rs.teslaris.core.service.interfaces.commontypes.LanguageTagService;
import rs.teslaris.core.service.interfaces.commontypes.ResearchAreaService;
import rs.teslaris.migrator.converter.hydrator.FundingEntityCreator;
import rs.teslaris.migrator.converter.hydrator.FundingMigrationDTO;
import rs.teslaris.migrator.converter.hydrator.HydratorConversionUtil;
import rs.teslaris.migrator.converter.hydrator.HydratorFundingPartFactory;
import rs.teslaris.migrator.converter.hydrator.HydratorInstitutionResolver;
import rs.teslaris.migrator.converter.hydrator.HydratorSource;
import rs.teslaris.migrator.converter.hydrator.ProjectMigrationDTO;
import rs.teslaris.migrator.model.MigrationRun;
import rs.teslaris.migrator.model.hydrator.HydratorCVModel;
import rs.teslaris.migrator.model.hydrator.HydratorProjectModel;
import rs.teslaris.migrator.service.impl.MigrationIdResolver;
import rs.teslaris.migrator.util.MigrationEntityType;
import rs.teslaris.migrator.util.MigrationException;
import rs.teslaris.migrator.util.MigrationLog;
import rs.teslaris.project.dto.funding.FundingDTO;
import rs.teslaris.project.model.funding.Funding;
import rs.teslaris.project.model.project.OrganisationUnitProjectContributionType;
import rs.teslaris.project.service.interfaces.funding.FundingService;

@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
public class FundingEntityCreatorTest {

    private static final MigrationRun RUN = MigrationRun.builder().id("run-1").build();

    private static final HydratorCVModel.Institution FUNDER =
        new HydratorCVModel.Institution("Test Agency", null, null,
            new HydratorCVModel.OtherIdentifiers(1,
                List.of(new HydratorCVModel.InstitutionIdentifier("12345", "RingGold"))));

    @Mock
    private FundingService fundingService;

    @Mock
    private CurrencyService currencyService;

    @Mock
    private MigrationIdResolver idResolver;

    @Mock
    private HydratorInstitutionResolver institutionResolver;

    @Mock
    private MigrationLog migrationLog;

    private FundingEntityCreator creator;

    private static FundingDTO fundingWithAmount() {
        var dto = new FundingDTO();
        var amount = new MonetaryAmountDTO();
        amount.setAmount(100);
        amount.setCurrencyCode("EUR");
        dto.setAmount(amount);
        return dto;
    }

    private static ProjectMigrationDTO.ConsortiumEntry partnerWithAmount() {
        return new ProjectMigrationDTO.ConsortiumEntry(
            OrganisationUnitProjectContributionType.PARTNER,
            new HydratorCVModel.Institution("Test Partner", null, null, null), 2,
            new HydratorProjectModel.Amount("EUR", "1000"));
    }

    @BeforeEach
    void setUp() {
        var conversionUtil = new HydratorConversionUtil(mock(LanguageTagService.class),
            mock(LanguageService.class), mock(ResearchAreaService.class));
        creator = new FundingEntityCreator(fundingService, currencyService, idResolver,
            institutionResolver, conversionUtil,
            new HydratorFundingPartFactory(currencyService, conversionUtil, migrationLog),
            migrationLog);

        var created = new Funding();
        created.setId(42);
        when(fundingService.createFunding(any())).thenReturn(created);
        when(idResolver.require(HydratorSource.NAME, MigrationEntityType.PROJECT, "TEST001"))
            .thenReturn(5);
    }

    @Test
    void shouldLinkProjectFunderAndCurrency() {
        var dto = fundingWithAmount();
        var euro = new Currency();
        euro.setId(3);
        when(currencyService.findCurrencyByCode("EUR")).thenReturn(euro);
        when(institutionResolver.resolve(FUNDER))
            .thenReturn(new HydratorInstitutionResolver.Match(7, null));

        var id = creator.create(
            new FundingMigrationDTO("TEST001", "F1", FUNDER, dto, List.of(), null), RUN);

        assertEquals(42, id);
        assertEquals(5, dto.getProjectId());
        assertEquals(7, dto.getFunderId());
        assertEquals(3, dto.getAmount().getCurrencyId());
        assertEquals(0, dto.getDisplayFunder().size());
    }

    @Test
    void shouldKeepFunderNameWhenNotLinked() {
        var dto = new FundingDTO();
        when(institutionResolver.resolve(FUNDER))
            .thenReturn(new HydratorInstitutionResolver.Match(null, "identifier not found"));

        creator.create(
            new FundingMigrationDTO("TEST001", "F1", FUNDER, dto, List.of(), null), RUN);

        assertNull(dto.getFunderId());
        assertEquals("Test Agency", dto.getDisplayFunder().getFirst().getContent());
        verify(migrationLog).valueDropped(anyString(), eq("PROJECT_FUNDING"),
            eq("TEST001#funding#F1"), eq("CW-002-MAP-000009"), anyString());
    }

    @Test
    void shouldDropAmountWithoutCurrency() {
        var dto = fundingWithAmount();
        when(institutionResolver.resolve(FUNDER))
            .thenReturn(new HydratorInstitutionResolver.Match(7, null));

        creator.create(
            new FundingMigrationDTO("TEST001", "F1", FUNDER, dto, List.of(), null), RUN);

        assertNull(dto.getAmount());
        verify(migrationLog).valueDropped(anyString(), eq("PROJECT_FUNDING"),
            eq("TEST001#funding#F1"), eq("CW-002-MAP-000008"),
            eq("currency 'EUR' not in registry, amount dropped"));
    }

    @Test
    void shouldFailWhenProjectIsNotMigrated() {
        when(idResolver.require(HydratorSource.NAME, MigrationEntityType.PROJECT, "TEST999"))
            .thenThrow(new MigrationException("not migrated"));

        assertThrows(MigrationException.class, () -> creator.create(
            new FundingMigrationDTO("TEST999", "F1", FUNDER, new FundingDTO(), List.of(), null),
            RUN));
        verify(fundingService, never()).createFunding(any());
    }

    @Test
    void shouldAddConsortiumAmountsAsFundingParts() {
        var dto = new FundingDTO();
        var euro = new Currency();
        euro.setId(3);
        when(currencyService.findCurrencyByCode("EUR")).thenReturn(euro);
        when(idResolver.resolve(HydratorSource.NAME, MigrationEntityType.PROJECT_ORGANISATION,
            "TEST001#consortium#2"))
            .thenReturn(Optional.of(11));

        creator.create(new FundingMigrationDTO("TEST001", "F1", null, dto,
            List.of(partnerWithAmount()), null), RUN);

        assertEquals(1, dto.getFundingParts().size());
        var part = dto.getFundingParts().getFirst();
        assertEquals(11, part.getOrganisationUnitProjectContributionId());
        assertEquals(1000, part.getAmount().getAmount());
        assertEquals(3, part.getAmount().getCurrencyId());
        assertEquals("Test Partner", part.getDescription().getFirst().getContent());
    }

    @Test
    void shouldDropAmountWhenContributionIsNotFound() {
        var dto = new FundingDTO();
        when(idResolver.resolve(HydratorSource.NAME, MigrationEntityType.PROJECT_ORGANISATION,
            "TEST001#consortium#2"))
            .thenReturn(Optional.empty());

        creator.create(new FundingMigrationDTO("TEST001", "F1", null, dto,
            List.of(partnerWithAmount()), null), RUN);

        assertTrue(dto.getFundingParts().isEmpty());
        verify(migrationLog).valueDropped(anyString(), eq("PROJECT_FUNDING"),
            eq("TEST001#funding#F1"), eq("CW-002-MAP-000022"),
            eq("PARTNER #2 amount dropped: contribution not found"));
    }
}
