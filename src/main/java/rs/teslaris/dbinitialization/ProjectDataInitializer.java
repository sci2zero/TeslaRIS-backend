package rs.teslaris.dbinitialization;

import java.time.LocalDate;
import java.util.List;
import java.util.Set;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;
import rs.teslaris.core.model.commontypes.LanguageTag;
import rs.teslaris.core.model.commontypes.MultiLingualContent;
import rs.teslaris.core.model.institution.OrganisationUnit;
import rs.teslaris.core.model.project.Currency;
import rs.teslaris.core.model.project.FundingProgram;
import rs.teslaris.core.repository.project.CurrencyRepository;
import rs.teslaris.core.repository.project.FundingProgramRepository;

@Component
@RequiredArgsConstructor
@Transactional
public class ProjectDataInitializer {

    private final CurrencyRepository currencyRepository;

    private final FundingProgramRepository fundingProgramRepository;


    public void initializeProjectTestingData(LanguageTag englishTag, OrganisationUnit funder1) {
        var currencyEuro = new Currency();
        currencyEuro.setName(Set.of(new MultiLingualContent(englishTag, "Euro", 1)));
        currencyEuro.setCode("EUR");
        currencyEuro.setSymbol("€");

        var currencyDollar = new Currency();
        currencyDollar.setName(Set.of(new MultiLingualContent(englishTag, "Dollar", 1)));
        currencyDollar.setCode("USD");
        currencyDollar.setSymbol("$");

        var currencySerbianDinar = new Currency();
        currencySerbianDinar.setName(
            Set.of(new MultiLingualContent(englishTag, "Serbian Dinar", 1)));
        currencySerbianDinar.setCode("RSD");
        currencySerbianDinar.setSymbol("RSD");

        currencyRepository.saveAll(List.of(currencyEuro, currencyDollar, currencySerbianDinar));

        var fundingProgram1 = new FundingProgram();
        fundingProgram1.setName(
            Set.of(new MultiLingualContent(englishTag, "Horizon Europe", 1)));
        fundingProgram1.setProgramOpens(LocalDate.of(2020, 1, 1));
        fundingProgram1.setProgramCloses(LocalDate.of(2035, 5, 14));
        fundingProgram1.setFunder(funder1);

        var fundingProgram2 = new FundingProgram();
        fundingProgram2.setName(
            Set.of(new MultiLingualContent(englishTag, "Ministry of Science", 1)));
        fundingProgram2.setProgramOpens(LocalDate.of(2022, 12, 1));
        fundingProgram2.setProgramCloses(LocalDate.of(2025, 2, 1));
        fundingProgram2.setFunder(funder1);

        fundingProgramRepository.saveAll(List.of(fundingProgram1, fundingProgram2));
    }
}
