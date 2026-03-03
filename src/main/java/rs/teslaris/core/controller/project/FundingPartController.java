package rs.teslaris.core.controller.project;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;
import rs.teslaris.core.annotation.Idempotent;
import rs.teslaris.core.dto.project.FundingPartDTO;
import rs.teslaris.core.service.interfaces.project.FundingPartService;

@RestController
@RequestMapping("/api/funding-part")
@RequiredArgsConstructor
public class FundingPartController {

    private final FundingPartService fundingPartService;


    @PostMapping
    @PreAuthorize("hasAuthority('EDIT_FUNDING_PARTS')")
    @Idempotent
    @ResponseStatus(HttpStatus.CREATED)
    public FundingPartDTO createFundingPart(@RequestBody @Valid FundingPartDTO dto) {
        var savedValue = fundingPartService.createFundingPart(dto);

        dto.setId(savedValue.getId());
        return dto;
    }

    @PutMapping("/{fundingPartId}")
    @PreAuthorize("hasAuthority('EDIT_FUNDING_PARTS')")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void updateFundingPart(@PathVariable Integer fundingPartId,
                                   @RequestBody @Valid FundingPartDTO dto) {
        fundingPartService.updateFundingPart(fundingPartId, dto);
    }

    @DeleteMapping("/{fundingPartId}")
    @PreAuthorize("hasAuthority('EDIT_FUNDING_PARTS')")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void deleteFundingPart(@PathVariable Integer fundingPartId) {
        fundingPartService.deleteFundingPart(fundingPartId);
    }
}
