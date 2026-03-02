package rs.teslaris.core.controller.project;

import jakarta.validation.Valid;
import java.time.LocalDate;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;
import rs.teslaris.core.annotation.Idempotent;
import rs.teslaris.core.dto.document.DocumentFileDTO;
import rs.teslaris.core.dto.document.DocumentFileResponseDTO;
import rs.teslaris.core.dto.project.FundingCallDTO;
import rs.teslaris.core.indexmodel.project.FundingCallIndex;
import rs.teslaris.core.service.interfaces.project.FundingCallService;

@RestController
@RequestMapping("/api/funding-call")
@RequiredArgsConstructor
public class FundingCallController {

    private final FundingCallService fundingCallService;


    @GetMapping("/search")
    @PreAuthorize("hasAuthority('READ_FUNDING_CALLS')")
    public Page<FundingCallIndex> searchFundingCalls(@RequestParam List<String> tokens,
                                                     @RequestParam(required = false)
                                                     LocalDate dateFrom,
                                                     @RequestParam(required = false)
                                                     LocalDate dateTo,
                                                     @RequestParam(required = false)
                                                     Integer programId,
                                                     Pageable pageable) {
        return fundingCallService.searchFundingCalls(tokens, dateFrom, dateTo, programId,
            pageable);
    }

    @GetMapping("/{fundingCallId}")
    @PreAuthorize("hasAuthority('READ_FUNDING_CALLS')")
    public FundingCallDTO readFundingCall(@PathVariable Integer fundingCallId) {
        return fundingCallService.readFundingCall(fundingCallId);
    }

    @PostMapping
    @PreAuthorize("hasAuthority('EDIT_FUNDING_CALLS')")
    @ResponseStatus(HttpStatus.CREATED)
    public FundingCallDTO createFundingCall(
        @RequestBody @Valid FundingCallDTO fundingCallDTO) {
        var savedFundingCall = fundingCallService.createFundingCall(fundingCallDTO);
        fundingCallDTO.setId(savedFundingCall.getId());

        return fundingCallDTO;
    }

    @PutMapping("/{fundingCallId}")
    @PreAuthorize("hasAuthority('EDIT_FUNDING_CALLS')")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void updateFundingCall(@PathVariable Integer fundingCallId,
                                  @RequestBody @Valid FundingCallDTO fundingCallDTO) {
        fundingCallService.updateFundingCall(fundingCallId, fundingCallDTO);
    }

    @DeleteMapping("/{fundingCallId}")
    @PreAuthorize("hasAuthority('EDIT_FUNDING_CALLS')")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void deleteFundingCall(@PathVariable Integer fundingCallId) {
        fundingCallService.deleteFundingCall(fundingCallId);
    }

    @PatchMapping("/{fundingCallId}")
    @PreAuthorize("hasAuthority('EDIT_FUNDING_CALLS')")
    @Idempotent
    public DocumentFileResponseDTO addFundingCallDocument(@PathVariable Integer fundingCallId,
                                                          @ModelAttribute @Valid
                                                          DocumentFileDTO documentFile) {
        return fundingCallService.addFundingCallDocument(fundingCallId, documentFile);
    }

    @PatchMapping("/update-call")
    @PreAuthorize("hasAuthority('EDIT_FUNDING_CALLS')")
    @Idempotent
    public DocumentFileResponseDTO updateFundingCallDocument(
        @ModelAttribute @Valid DocumentFileDTO documentFile) {
        return fundingCallService.updateFundingCallDocument(documentFile);
    }

    @DeleteMapping("/{fundingCallId}/{documentFileId}")
    @PreAuthorize("hasAuthority('EDIT_FUNDING_CALLS')")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void deleteFundingCallDocument(@PathVariable Integer fundingCallId,
                                          @PathVariable Integer documentFileId) {
        fundingCallService.deleteFundingCallDocument(documentFileId, fundingCallId);
    }
}
