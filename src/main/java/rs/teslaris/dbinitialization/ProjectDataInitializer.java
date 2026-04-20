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
import rs.teslaris.project.model.common.Currency;
import rs.teslaris.project.model.common.MonetaryAmount;
import rs.teslaris.project.model.funding.Funding;
import rs.teslaris.project.model.funding.FundingApplication;
import rs.teslaris.project.model.funding.FundingCall;
import rs.teslaris.project.model.funding.FundingPart;
import rs.teslaris.project.model.funding.FundingProgram;
import rs.teslaris.project.repository.common.CurrencyRepository;
import rs.teslaris.project.repository.funding.FundingApplicationRepository;
import rs.teslaris.project.repository.funding.FundingCallRepository;
import rs.teslaris.project.repository.funding.FundingPartRepository;
import rs.teslaris.project.repository.funding.FundingProgramRepository;
import rs.teslaris.project.repository.funding.FundingRepository;

@Component
@RequiredArgsConstructor
@Transactional
public class ProjectDataInitializer {

    private final CurrencyRepository currencyRepository;

    private final FundingProgramRepository fundingProgramRepository;

    private final FundingCallRepository fundingCallRepository;

    private final FundingRepository fundingRepository;

    private final FundingPartRepository fundingPartRepository;

    private final FundingApplicationRepository fundingApplicationRepository;


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
        fundingProgram1.setDateFrom(LocalDate.of(2020, 1, 1));
        fundingProgram1.setDateTo(LocalDate.of(2035, 5, 14));
        fundingProgram1.setFunder(funder1);

        var fundingProgram2 = new FundingProgram();
        fundingProgram2.setName(
            Set.of(new MultiLingualContent(englishTag, "Ministry of Science", 1)));
        fundingProgram2.setDateFrom(LocalDate.of(2022, 12, 1));
        fundingProgram2.setDateTo(LocalDate.of(2025, 2, 1));
        fundingProgram2.setFunder(funder1);

        fundingProgramRepository.saveAll(List.of(fundingProgram1, fundingProgram2));

        var fundingCall1 = new FundingCall();
        fundingCall1.setName(
            Set.of(new MultiLingualContent(englishTag, "Funding Call 1", 1)));
        fundingCall1.setDateFrom(LocalDate.of(2020, 2, 1));
        fundingCall1.setDateTo(LocalDate.of(2021, 2, 1));
        fundingCall1.setFundingProgram(fundingProgram1);
        fundingCall1.setFunder(fundingProgram1.getFunder());

        var fundingCall2 = new FundingCall();
        fundingCall2.setName(
            Set.of(new MultiLingualContent(englishTag, "Funding Call 2", 1)));
        fundingCall2.setDateFrom(LocalDate.of(2022, 7, 6));
        fundingCall2.setDateTo(LocalDate.of(2023, 3, 17));
        fundingCall2.setFundingProgram(fundingProgram1);
        fundingCall2.setFunder(fundingProgram1.getFunder());

        fundingCallRepository.saveAll(List.of(fundingCall1, fundingCall2));

        var funding1 = new Funding();
        funding1.setName(Set.of(new MultiLingualContent(englishTag, "Funding 1", 1)));
        funding1.setAmount(new MonetaryAmount(1000000, currencyEuro));

        var funding2 = new Funding();
        funding2.setName(Set.of(new MultiLingualContent(englishTag, "Funding 2", 1)));
        funding2.setAmount(new MonetaryAmount(1000000, currencyEuro));

        var funding3 = new Funding();
        funding3.setName(Set.of(new MultiLingualContent(englishTag, "Big Funding", 1)));
        funding3.setAmount(new MonetaryAmount(1000000000, currencyEuro));

        fundingRepository.saveAll(List.of(funding1, funding2, funding3));

        var fundingApplication1 = new FundingApplication();
        fundingApplication1.setFundingCall(fundingCall1);
        fundingApplicationRepository.save(fundingApplication1);

        var fundingPart1 = new FundingPart();
        fundingPart1.setDescription(Set.of(new MultiLingualContent(englishTag, "Small Part", 1)));
        fundingPart1.setAmount(new MonetaryAmount(3000, currencyEuro));
        fundingPart1.setFunding(funding3);
        fundingPart1.setFundingApplication(fundingApplication1);

        var fundingPart2 = new FundingPart();
        fundingPart2.setDescription(Set.of(new MultiLingualContent(englishTag, "Big Part", 1)));
        fundingPart2.setAmount(new MonetaryAmount(10000, currencyEuro));
        fundingPart2.setFunding(funding3);
        fundingPart2.setFundingApplication(fundingApplication1);

        fundingPartRepository.saveAll(List.of(fundingPart1, fundingPart2));
    }
}
