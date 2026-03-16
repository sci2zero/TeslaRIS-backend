package rs.teslaris.project.controller.funding;

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
import rs.teslaris.project.dto.funding.FundingProgramDTO;
import rs.teslaris.project.indexmodel.funding.FundingProgramIndex;
import rs.teslaris.project.service.interfaces.funding.FundingProgramService;

@RestController
@RequestMapping("/api/funding-program")
@RequiredArgsConstructor
public class FundingProgramController {

    private final FundingProgramService fundingProgramService;


    @GetMapping("/search")
    @PreAuthorize("hasAuthority('READ_FUNDING_PROGRAMS')")
    public Page<FundingProgramIndex> searchFundingPrograms(@RequestParam List<String> tokens,
                                                           @RequestParam(required = false)
                                                           LocalDate dateFrom,
                                                           @RequestParam(required = false)
                                                           LocalDate dateTo,
                                                           @RequestParam(required = false)
                                                           Integer funderId,
                                                           Pageable pageable) {
        return fundingProgramService.searchFundingPrograms(tokens, dateFrom, dateTo, funderId,
            pageable);
    }

    @GetMapping("/{fundingProgramId}")
    @PreAuthorize("hasAuthority('READ_FUNDING_PROGRAMS')")
    public FundingProgramDTO readFundingProgram(@PathVariable Integer fundingProgramId) {
        return fundingProgramService.readFundingProgram(fundingProgramId);
    }

    @PostMapping
    @PreAuthorize("hasAuthority('EDIT_FUNDING_PROGRAMS')")
    @ResponseStatus(HttpStatus.CREATED)
    public FundingProgramDTO createFundingProgram(
        @RequestBody @Valid FundingProgramDTO fundingProgramDTO) {
        var savedFundingProgram = fundingProgramService.createFundingProgram(fundingProgramDTO);
        fundingProgramDTO.setId(savedFundingProgram.getId());

        return fundingProgramDTO;
    }

    @PutMapping("/{fundingProgramId}")
    @PreAuthorize("hasAuthority('EDIT_FUNDING_PROGRAMS')")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void updateFundingProgram(@PathVariable Integer fundingProgramId,
                                     @RequestBody @Valid FundingProgramDTO fundingProgramDTO) {
        fundingProgramService.updateFundingProgram(fundingProgramId, fundingProgramDTO);
    }

    @DeleteMapping("/{fundingProgramId}")
    @PreAuthorize("hasAuthority('EDIT_FUNDING_PROGRAMS')")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void deleteFundingProgram(@PathVariable Integer fundingProgramId) {
        fundingProgramService.deleteFundingProgram(fundingProgramId);
    }

    @PatchMapping("/{fundingProgramId}")
    @PreAuthorize("hasAuthority('EDIT_FUNDING_PROGRAMS')")
    @Idempotent
    public DocumentFileResponseDTO addFundingProgramDocument(@PathVariable Integer fundingProgramId,
                                                             @ModelAttribute @Valid
                                                             DocumentFileDTO documentFile) {
        return fundingProgramService.addFundingProgramDocument(fundingProgramId, documentFile);
    }

    @PatchMapping("/update-program")
    @PreAuthorize("hasAuthority('EDIT_FUNDING_PROGRAMS')")
    @Idempotent
    public DocumentFileResponseDTO updateFundingProgramDocument(
        @ModelAttribute @Valid DocumentFileDTO documentFile) {
        return fundingProgramService.updateFundingProgramDocument(documentFile);
    }

    @DeleteMapping("/{fundingProgramId}/{documentFileId}")
    @PreAuthorize("hasAuthority('EDIT_FUNDING_PROGRAMS')")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void deleteFundingProgramDocument(@PathVariable Integer fundingProgramId,
                                             @PathVariable Integer documentFileId) {
        fundingProgramService.deleteFundingProgramDocument(documentFileId, fundingProgramId);
    }
}
