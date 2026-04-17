package rs.teslaris.project.controller.funding;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import rs.teslaris.core.annotation.Idempotent;
import rs.teslaris.project.dto.funding.FundingDTO;
import rs.teslaris.project.indexmodel.funding.FundingIndex;
import rs.teslaris.project.service.interfaces.funding.FundingService;

import java.time.LocalDate;
import java.util.List;

@RestController
@RequestMapping("/api/funding")
@RequiredArgsConstructor
public class FundingController {

    private final FundingService fundingService;

    @GetMapping("/search")
    @PreAuthorize("hasAuthority('READ_FUNDING')")
    public Page<FundingIndex> searchFunding(@RequestParam List<String> tokens,
                                            @RequestParam(required = false)
                                            LocalDate dateFrom,
                                            @RequestParam(required = false)
                                            LocalDate dateTo,
                                            @RequestParam(required = false)
                                            Integer projectId,
                                            @RequestParam(required = false)
                                            Integer fundingCallId,
                                            @RequestParam(required = false)
                                            Integer funderId,
                                            Pageable pageable) {
        return fundingService.searchFunding(tokens, dateFrom, dateTo, projectId,
                fundingCallId, funderId, pageable);
    }

    @GetMapping("/{fundingId}")
    @PreAuthorize("hasAuthority('READ_FUNDING')")
    public FundingDTO readFunding(@PathVariable Integer fundingId) {
        return fundingService.readFunding(fundingId);
    }

    @PostMapping
    @PreAuthorize("hasAuthority('EDIT_FUNDING')")
    @ResponseStatus(HttpStatus.CREATED)
    @Idempotent
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

    @DeleteMapping("/{fundingId}")
    @PreAuthorize("hasAuthority('EDIT_FUNDING')")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void deleteFunding(@PathVariable Integer fundingId) {
        fundingService.deleteFunding(fundingId);
    }

}
