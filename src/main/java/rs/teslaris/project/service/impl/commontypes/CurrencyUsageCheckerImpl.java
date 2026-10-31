package rs.teslaris.project.service.impl.commontypes;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import rs.teslaris.core.service.interfaces.commontypes.CurrencyUsageChecker;
import rs.teslaris.project.repository.funding.FundingApplicationRepository;
import rs.teslaris.project.repository.funding.FundingCallRepository;
import rs.teslaris.project.repository.funding.FundingPartRepository;
import rs.teslaris.project.repository.funding.FundingProgramRepository;
import rs.teslaris.project.repository.funding.FundingRepository;
import rs.teslaris.project.repository.project.ProjectRepository;

@Service
@RequiredArgsConstructor
public class CurrencyUsageCheckerImpl implements CurrencyUsageChecker {

    private final ProjectRepository projectRepository;

    private final FundingRepository fundingRepository;

    private final FundingPartRepository fundingPartRepository;

    private final FundingCallRepository fundingCallRepository;

    private final FundingProgramRepository fundingProgramRepository;

    private final FundingApplicationRepository fundingApplicationRepository;


    @Override
    public boolean isCurrencyInUse(Integer currencyId) {
        return projectRepository.isCurrencyInUse(currencyId) ||
            fundingRepository.isCurrencyInUse(currencyId) ||
            fundingPartRepository.isCurrencyInUse(currencyId) ||
            fundingCallRepository.isCurrencyInUse(currencyId) ||
            fundingProgramRepository.isCurrencyInUse(currencyId) ||
            fundingApplicationRepository.isCurrencyInUse(currencyId);
    }
}
