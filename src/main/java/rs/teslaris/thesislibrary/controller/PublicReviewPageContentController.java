package rs.teslaris.thesislibrary.controller;

import jakarta.validation.Valid;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;
import rs.teslaris.core.annotation.OrgUnitEditCheck;
import rs.teslaris.core.model.document.ThesisType;
import rs.teslaris.thesislibrary.dto.PublicReviewPageContentDTO;
import rs.teslaris.thesislibrary.service.interfaces.PublicReviewPageContentService;

@RestController
@RequestMapping("/api/public-review-page-content")
@RequiredArgsConstructor
public class PublicReviewPageContentController {

    private final PublicReviewPageContentService publicReviewPageContentService;

    @GetMapping("/for-institution/{organisationUnitId}")
    public List<PublicReviewPageContentDTO> fetchInstitutionConfiguration(
        @PathVariable Integer organisationUnitId) {
        return publicReviewPageContentService.readPageContentConfigurationForInstitution(
            organisationUnitId);
    }

    @GetMapping("/for-institution-and-type/{organisationUnitId}")
    public List<PublicReviewPageContentDTO> fetchInstitutionConfigurationForType(
        @PathVariable Integer organisationUnitId, @RequestParam List<ThesisType> thesisTypes) {
        return publicReviewPageContentService.readPageContentConfigurationForInstitutionAndType(
            organisationUnitId, thesisTypes);
    }

    @PatchMapping("/{organisationUnitId}")
    @PreAuthorize("hasAuthority('SAVE_OU_PAGE_CONFIGURATION')")
    @OrgUnitEditCheck
    @ResponseStatus(HttpStatus.ACCEPTED)
    public void saveConfigurationForInstitution(@PathVariable Integer organisationUnitId,
                                                @RequestBody @Valid
                                                List<PublicReviewPageContentDTO> configuration) {
        publicReviewPageContentService.savePageConfiguration(configuration, organisationUnitId);
    }
}
