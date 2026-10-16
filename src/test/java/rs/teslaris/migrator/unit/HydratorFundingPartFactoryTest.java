package rs.teslaris.migrator.unit;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;
import rs.teslaris.core.model.commontypes.Currency;
import rs.teslaris.core.model.project.OrganisationUnitProjectContributionType;
import rs.teslaris.core.service.interfaces.commontypes.CurrencyService;
import rs.teslaris.core.service.interfaces.commontypes.LanguageService;
import rs.teslaris.core.service.interfaces.commontypes.LanguageTagService;
import rs.teslaris.core.service.interfaces.commontypes.ResearchAreaService;
import rs.teslaris.migrator.converter.hydrator.HydratorConversionUtil;
import rs.teslaris.migrator.converter.hydrator.HydratorFundingPartFactory;
import rs.teslaris.migrator.converter.hydrator.ProjectMigrationDTO;
import rs.teslaris.migrator.model.hydrator.HydratorCVModel;
import rs.teslaris.migrator.model.hydrator.HydratorProjectModel;
import rs.teslaris.migrator.util.MigrationEntityType;
import rs.teslaris.migrator.util.MigrationLog;

@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
public class HydratorFundingPartFactoryTest {

    @Mock
    private CurrencyService currencyService;

    @Mock
    private MigrationLog migrationLog;

    private HydratorFundingPartFactory factory;


    @BeforeEach
    void setUp() {
        factory = new HydratorFundingPartFactory(currencyService,
            new HydratorConversionUtil(mock(LanguageTagService.class), mock(LanguageService.class),
                mock(ResearchAreaService.class)),
            migrationLog);

        var euro = new Currency();
        euro.setId(3);
        when(currencyService.findCurrencyByCode("EUR")).thenReturn(euro);
    }

    private static ProjectMigrationDTO.ConsortiumEntry partner(String currency, String value) {
        return new ProjectMigrationDTO.ConsortiumEntry(
            OrganisationUnitProjectContributionType.PARTNER,
            new HydratorCVModel.Institution("Test Partner", null, null, null), 2,
            new HydratorProjectModel.Amount(currency, value));
    }

    @Test
    void shouldBuildPartFromAmount() {
        var part = factory.fundingPart(MigrationEntityType.PROJECT, "TEST001",
            partner("eur", "1500.50")).orElseThrow();

        assertEquals(1500.50, part.getAmount().getAmount());
        assertEquals(3, part.getAmount().getCurrencyId());
        assertEquals("Test Partner", part.getDescription().getFirst().getContent());
    }

    @Test
    void shouldDropNonPositiveAmount() {
        var part = factory.fundingPart(MigrationEntityType.PROJECT, "TEST001",
            partner("EUR", "0"));

        assertTrue(part.isEmpty());
        verify(migrationLog).valueDropped(anyString(), eq("PROJECT"), eq("TEST001"),
            eq("CW-002-MAP-000022"), eq("PARTNER #2 amount dropped: invalid amount '0'"));
    }

    @Test
    void shouldDropUnparsableAmount() {
        var part = factory.fundingPart(MigrationEntityType.PROJECT, "TEST001",
            partner("EUR", "n/a"));

        assertTrue(part.isEmpty());
        verify(migrationLog).valueDropped(anyString(), eq("PROJECT"), eq("TEST001"),
            eq("CW-002-MAP-000022"), eq("PARTNER #2 amount dropped: invalid amount 'n/a'"));
    }

    @Test
    void shouldDropAmountInUnknownCurrency() {
        var part = factory.fundingPart(MigrationEntityType.PROJECT_FUNDING, "TEST001#funding#F1",
            partner("XYZ", "10"));

        assertTrue(part.isEmpty());
        verify(migrationLog).valueDropped(anyString(), eq("PROJECT_FUNDING"),
            eq("TEST001#funding#F1"), eq("CW-002-MAP-000022"),
            eq("PARTNER #2 amount dropped: currency 'XYZ' not in registry"));
    }
}
