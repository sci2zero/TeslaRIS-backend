package rs.teslaris.project.controller.funding;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import rs.teslaris.project.dto.funding.FundingApplicationDTO;
import rs.teslaris.project.dto.funding.FundingCallDTO;
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

    @PostMapping
    @PreAuthorize("hasAuthority('EDIT_FUNDING_APPLICATIONS')")
    @ResponseStatus(HttpStatus.CREATED)
    public FundingApplicationDTO createFundingApplication(
            @RequestBody @Valid FundingApplicationDTO fundingApplicationDTO) {
        var savedFundingApplication = fundingApplicationService.createFundingApplication(fundingApplicationDTO);
        fundingApplicationDTO.setId(savedFundingApplication.getId());

        return fundingApplicationDTO;
    }

    @PutMapping("/{fundingApplicationId}")
    @PreAuthorize("hasAuthority('EDIT_FUNDING_APPLICATIONS')")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void updateFundingApplication(@PathVariable Integer fundingApplicationId,
                                  @RequestBody @Valid FundingApplicationDTO fundingApplicationDTO) {
        fundingApplicationService.updateFundingApplication(fundingApplicationId, fundingApplicationDTO);
    }

    @DeleteMapping("/{fundingApplicationId}")
    @PreAuthorize("hasAuthority('EDIT_FUNDING_APPLICATIONS')")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void deleteFundingApplication(@PathVariable Integer fundingApplicationId) {
        fundingApplicationService.deleteFundingApplication(fundingApplicationId);
    }
}
