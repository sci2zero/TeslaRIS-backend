package rs.teslaris.core.controller;

import jakarta.validation.Valid;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
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
import rs.teslaris.core.annotation.PublicationEditCheck;
import rs.teslaris.core.dto.institution.OrganisationUnitTrustConfigurationDTO;
import rs.teslaris.core.indexmodel.DocumentPublicationIndex;
import rs.teslaris.core.indexmodel.DocumentPublicationType;
import rs.teslaris.core.service.interfaces.institution.OrganisationUnitTrustConfigurationService;
import rs.teslaris.core.util.functional.Pair;

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

    @GetMapping("/document/{documentId}")
    @PreAuthorize("hasAuthority('VALIDATE_METADATA') and hasAuthority('VALIDATE_UPLOADED_FILES')")
    @PublicationEditCheck
    public Pair<Boolean, Boolean> fetchValidationStatusForDocument(
        @PathVariable Integer documentId) {
        return organisationUnitTrustConfigurationService.fetchValidationStatusForDocument(
            documentId);
    }

    @GetMapping("/non-validated-documents/{organisationUnitId}")
    @PreAuthorize("hasAuthority('VALIDATE_METADATA') and hasAuthority('VALIDATE_UPLOADED_FILES')")
    @OrgUnitEditCheck
    public Page<DocumentPublicationIndex> fetchNonValidatedDocumentPublications(
        @PathVariable Integer organisationUnitId,
        @RequestParam Boolean metadata,
        @RequestParam Boolean files,
        @RequestParam(value = "allowedTypes", required = false)
        List<DocumentPublicationType> allowedTypes,
        Pageable pageable) {
        return organisationUnitTrustConfigurationService.fetchNonValidatedPublications(
            organisationUnitId, metadata, files, allowedTypes, pageable);
    }
}
