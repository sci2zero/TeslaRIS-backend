package rs.teslaris.project.controller.funding;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
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

    @PostMapping
    @PreAuthorize("hasAuthority('EDIT_FUNDING')")
    @ResponseStatus(HttpStatus.CREATED)
    public FundingDTO createFunding(
            @RequestBody @Valid FundingDTO fundingDTO) {
        var savedFunding = fundingService.createFunding(fundingDTO);
        fundingDTO.setId(savedFunding.getId());

        return fundingDTO;
    }

    @PutMapping("/{fundingId}")
    @PreAuthorize("hasAuthority('EDIT_FUNDING')")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void updateFunding(@PathVariable Integer fundingId,
                                  @RequestBody @Valid FundingDTO fundingDTO) {
        fundingService.updateFunding(fundingId, fundingDTO);
    }

}
