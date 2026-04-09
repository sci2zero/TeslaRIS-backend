package rs.teslaris.project.controller.funding;

import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import rs.teslaris.project.dto.funding.FundingDTO;
import rs.teslaris.project.service.interfaces.funding.FundingService;

@RestController
@RequestMapping("/api/funding")
@RequiredArgsConstructor
public class FundingController {

    private final FundingService fundingService;

    @GetMapping("/{fundingId}")
    @PreAuthorize("hasAuthority('READ_FUNDING')")
    public FundingDTO readFunding(@PathVariable Integer fundingId) {
        return fundingService.readFunding(fundingId);
    }

}
