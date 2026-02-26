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
import rs.teslaris.core.dto.project.FundingProgramDTO;
import rs.teslaris.core.indexmodel.FundingProgramIndex;
import rs.teslaris.core.service.interfaces.project.FundingProgramService;

@RestController
@RequestMapping("/api/funding-program")
@RequiredArgsConstructor
public class FundingProgramController {

    private final FundingProgramService fundingProgramService;


    @GetMapping("/search")
    public Page<FundingProgramIndex> searchFundingPrograms(@RequestParam List<String> tokens,
                                                           @RequestParam LocalDate dateFrom,
                                                           @RequestParam LocalDate dateTo,
                                                           @RequestParam Integer funderId,
                                                           Pageable pageable) {
        return fundingProgramService.searchFundingPrograms(tokens, dateFrom, dateTo, funderId,
            pageable);
    }

    @GetMapping("/{fundingProgramId}")
    public FundingProgramDTO readFundingProgram(@PathVariable Integer fundingProgramId) {
        return fundingProgramService.readFundingProgram(fundingProgramId);
    }

    @PostMapping
    public FundingProgramDTO createFundingProgram(
        @RequestBody @Valid FundingProgramDTO fundingProgramDTO) {
        var savedFundingProgram = fundingProgramService.createFundingProgram(fundingProgramDTO);
        fundingProgramDTO.setId(savedFundingProgram.getId());

        return fundingProgramDTO;
    }

    @PutMapping("/{fundingProgramId}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void updateFundingProgram(@PathVariable Integer fundingProgramId,
                                     @RequestBody @Valid FundingProgramDTO fundingProgramDTO) {
        fundingProgramService.updateFundingProgram(fundingProgramId, fundingProgramDTO);
    }

    @DeleteMapping("/{fundingProgramId}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void deleteFundingProgram(@PathVariable Integer fundingProgramId) {
        fundingProgramService.deleteFundingProgram(fundingProgramId);
    }

    @PatchMapping("/{fundingProgramId}")
    @PreAuthorize("hasAuthority('EDIT_FUNDING_PROGRAM')")
    @Idempotent
    public DocumentFileResponseDTO addFundingProgramDocument(@PathVariable Integer fundingProgramId,
                                                             @ModelAttribute @Valid
                                                             DocumentFileDTO documentFile) {
        return fundingProgramService.addFundingProgramDocument(fundingProgramId, documentFile);
    }

    @PatchMapping("/update-program")
    @PreAuthorize("hasAuthority('EDIT_FUNDING_PROGRAM')")
    @Idempotent
    public DocumentFileResponseDTO updateFundingProgramDocument(
        @ModelAttribute @Valid DocumentFileDTO documentFile) {
        return fundingProgramService.updateFundingProgramDocument(documentFile);
    }

    @DeleteMapping("/{fundingProgramId}/{documentFileId}")
    @PreAuthorize("hasAuthority('EDIT_FUNDING_PROGRAM')")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void deleteFundingProgramDocument(@PathVariable Integer fundingProgramId,
                                             @PathVariable Integer documentFileId) {
        fundingProgramService.deleteFundingProgramDocument(documentFileId, fundingProgramId);
    }
}
