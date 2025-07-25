package rs.teslaris.core.controller;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;
import rs.teslaris.core.annotation.OrgUnitEditCheck;
import rs.teslaris.core.dto.institution.InstitutionDefaultSubmissionContentDTO;
import rs.teslaris.core.service.interfaces.person.InstitutionDefaultSubmissionContentService;
import rs.teslaris.core.util.jwt.JwtUtil;

@RestController
@RequestMapping("/api/institution-default-submission-content")
@RequiredArgsConstructor
public class InstitutionDefaultSubmissionContentController {

    private final InstitutionDefaultSubmissionContentService
        institutionDefaultSubmissionContentService;

    private final JwtUtil tokenUtil;


    @GetMapping("/institution/{organisationUnitId}")
    @PreAuthorize("hasAuthority('SET_DEFAULT_CONTENT')")
    @OrgUnitEditCheck
    public InstitutionDefaultSubmissionContentDTO getContentForInstitution(
        @PathVariable Integer organisationUnitId) {
        return institutionDefaultSubmissionContentService.readInstitutionDefaultContent(
            organisationUnitId);
    }

    @GetMapping("/for-user")
    public InstitutionDefaultSubmissionContentDTO getContentForUser(
        @RequestHeader("Authorization") String bearerToken) {
        return institutionDefaultSubmissionContentService.readInstitutionDefaultContentForUser(
            tokenUtil.extractUserIdFromToken(bearerToken));
    }

    @PatchMapping("/{organisationUnitId}")
    @PreAuthorize("hasAuthority('SET_DEFAULT_CONTENT')")
    @OrgUnitEditCheck
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void saveDefaultContent(@PathVariable Integer organisationUnitId, @RequestBody @Valid
    InstitutionDefaultSubmissionContentDTO content) {
        institutionDefaultSubmissionContentService.saveConfiguration(organisationUnitId, content);
    }
}
