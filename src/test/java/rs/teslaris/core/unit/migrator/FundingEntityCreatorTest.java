package rs.teslaris.core.unit.migrator;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
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
import rs.teslaris.core.service.interfaces.commontypes.CurrencyService;
import rs.teslaris.core.service.interfaces.commontypes.LanguageService;
import rs.teslaris.core.service.interfaces.commontypes.LanguageTagService;
import rs.teslaris.core.service.interfaces.commontypes.ResearchAreaService;
import rs.teslaris.migrator.converter.hydrator.FundingEntityCreator;
import rs.teslaris.migrator.converter.hydrator.FundingMigrationDTO;
import rs.teslaris.migrator.converter.hydrator.HydratorConversionUtil;
import rs.teslaris.migrator.converter.hydrator.HydratorInstitutionResolver;
import rs.teslaris.migrator.converter.hydrator.HydratorSource;
import rs.teslaris.migrator.model.hydrator.HydratorCVModel;
import rs.teslaris.migrator.service.impl.MigrationIdResolver;
import rs.teslaris.migrator.util.MigrationEntityType;
import rs.teslaris.migrator.util.MigrationException;
import rs.teslaris.migrator.util.MigrationLog;
import rs.teslaris.project.dto.funding.FundingDTO;
import rs.teslaris.project.model.common.Currency;
import rs.teslaris.project.model.funding.Funding;
import rs.teslaris.project.service.interfaces.funding.FundingService;

@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
public class FundingEntityCreatorTest {

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


    @BeforeEach
    void setUp() {
        creator = new FundingEntityCreator(fundingService, currencyService, idResolver,
            institutionResolver,
            new HydratorConversionUtil(mock(LanguageTagService.class),
                mock(LanguageService.class),
                mock(ResearchAreaService.class)),
            migrationLog);

        var created = new Funding();
        created.setId(42);
        when(fundingService.createFunding(any())).thenReturn(created);
        when(idResolver.resolve(HydratorSource.NAME, MigrationEntityType.PROJECT, "TEST001"))
            .thenReturn(Optional.of(5));
    }

    private static FundingDTO fundingWithAmount() {
        var dto = new FundingDTO();
        var amount = new MonetaryAmountDTO();
        amount.setAmount(100);
        amount.setCurrencyCode("EUR");
        dto.setAmount(amount);
        return dto;
    }

    @Test
    void shouldLinkProjectFunderAndCurrency() {
        var dto = fundingWithAmount();
        var euro = new Currency();
        euro.setId(3);
        when(currencyService.findCurrencyByCode("EUR")).thenReturn(euro);
        when(institutionResolver.resolve(FUNDER))
            .thenReturn(new HydratorInstitutionResolver.Match(7, null));

        var id = creator.create(new FundingMigrationDTO("TEST001", "F1", FUNDER, dto, null),
            true);

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

        creator.create(new FundingMigrationDTO("TEST001", "F1", FUNDER, dto, null), true);

        assertNull(dto.getFunderId());
        assertEquals("Test Agency", dto.getDisplayFunder().getFirst().getContent());
        verify(migrationLog).valueDropped(anyString(), eq("PROJECT_FUNDING"),
            eq("TEST001#funding#F1"), eq("MAP-009"), anyString());
    }

    @Test
    void shouldDropAmountWithoutCurrency() {
        var dto = fundingWithAmount();
        when(institutionResolver.resolve(FUNDER))
            .thenReturn(new HydratorInstitutionResolver.Match(7, null));

        creator.create(new FundingMigrationDTO("TEST001", "F1", FUNDER, dto, null), true);

        assertNull(dto.getAmount());
        verify(migrationLog).valueDropped(anyString(), eq("PROJECT_FUNDING"),
            eq("TEST001#funding#F1"), eq("MAP-008"),
            eq("currency 'EUR' not in registry, amount dropped"));
    }

    @Test
    void shouldFailWhenProjectIsNotMigrated() {
        when(idResolver.resolve(HydratorSource.NAME, MigrationEntityType.PROJECT, "TEST999"))
            .thenReturn(Optional.empty());

        assertThrows(MigrationException.class, () -> creator.create(
            new FundingMigrationDTO("TEST999", "F1", FUNDER, new FundingDTO(), null), true));
        verify(fundingService, never()).createFunding(any());
    }
}
