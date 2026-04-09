package rs.teslaris.project.controller.funding;

import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import rs.teslaris.project.dto.funding.FundingApplicationDTO;
import rs.teslaris.project.service.interfaces.funding.FundingApplicationService;

@RestController
@RequestMapping("/api/funding-application")
@RequiredArgsConstructor
public class FundingApplicationController {

    private final FundingApplicationService fundingApplicationService;

    @GetMapping("/{fundingApplicationId}")
    @PreAuthorize("hasAuthority('READ_FUNDING_APPLICATIONS')")
    public FundingApplicationDTO readFundingApplication(@PathVariable Integer fundingApplicationId) {
        return fundingApplicationService.readFundingApplication(fundingApplicationId);
    }
}
