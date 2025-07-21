package rs.teslaris.core.controller;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;
import rs.teslaris.core.annotation.OrgUnitEditCheck;
import rs.teslaris.core.annotation.PublicationEditCheck;
import rs.teslaris.core.dto.institution.OrganisationUnitTrustConfigurationDTO;
import rs.teslaris.core.service.interfaces.institution.OrganisationUnitTrustConfigurationService;

@RestController
@RequestMapping("/api/organisation-unit/trust-configuration")
@RequiredArgsConstructor
public class OrganisationUnitTrustConfigurationController {

    private final OrganisationUnitTrustConfigurationService
        organisationUnitTrustConfigurationService;


    @GetMapping("/{organisationUnitId}")
    @PreAuthorize("hasAuthority('SAVE_OU_TRUST_CONFIGURATION')")
    @OrgUnitEditCheck
    public OrganisationUnitTrustConfigurationDTO fetchConfigurationForOrganisationUnit(
        @PathVariable Integer organisationUnitId) {
        return organisationUnitTrustConfigurationService.readTrustConfigurationForOrganisationUnit(
            organisationUnitId);
    }

    @PatchMapping("/{organisationUnitId}")
    @PreAuthorize("hasAuthority('SAVE_OU_TRUST_CONFIGURATION')")
    @OrgUnitEditCheck
    @ResponseStatus(HttpStatus.ACCEPTED)
    public OrganisationUnitTrustConfigurationDTO saveConfigurationForOrganisationUnit(
        @PathVariable Integer organisationUnitId,
        @RequestBody @Valid OrganisationUnitTrustConfigurationDTO configuration) {
        return organisationUnitTrustConfigurationService.saveConfiguration(configuration,
            organisationUnitId);
    }

    @PatchMapping("/validate-document-metadata/{documentId}")
    @PreAuthorize("hasAuthority('VALIDATE_METADATA')")
    @PublicationEditCheck
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void approvePublicationMetadata(@PathVariable Integer documentId) {
        organisationUnitTrustConfigurationService.approvePublicationMetadata(documentId);
    }

    @PatchMapping("/validate-document-files/{documentId}")
    @PreAuthorize("hasAuthority('VALIDATE_UPLOADED_FILES')")
    @PublicationEditCheck
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void approvePublicationDocuments(@PathVariable Integer documentId) {
        organisationUnitTrustConfigurationService.approvePublicationUploadedDocuments(documentId);
    }
}
