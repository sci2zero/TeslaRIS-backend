package rs.teslaris.core.controller;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;
import java.io.IOException;
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
import rs.teslaris.core.annotation.OrgUnitEditCheck;
import rs.teslaris.core.annotation.Traceable;
import rs.teslaris.core.converter.institution.OrganisationUnitConverter;
import rs.teslaris.core.dto.commontypes.MultilingualContentDTO;
import rs.teslaris.core.dto.commontypes.ProfilePhotoOrLogoDTO;
import rs.teslaris.core.dto.institution.OrganisationUnitDTO;
import rs.teslaris.core.dto.institution.OrganisationUnitRequestDTO;
import rs.teslaris.core.indexmodel.EntityType;
import rs.teslaris.core.indexmodel.OrganisationUnitIndex;
import rs.teslaris.core.model.commontypes.ApproveStatus;
import rs.teslaris.core.service.interfaces.document.DeduplicationService;
import rs.teslaris.core.service.interfaces.person.OrganisationUnitService;
import rs.teslaris.core.util.Triple;
import rs.teslaris.core.util.search.SearchRequestType;
import rs.teslaris.core.util.search.StringUtil;

@RestController
@RequestMapping("/api/organisation-unit")
@RequiredArgsConstructor
@Traceable
public class OrganisationUnitController {

    private final OrganisationUnitService organisationUnitService;

    private final DeduplicationService deduplicationService;


    @GetMapping("/{organisationUnitId}/can-edit")
    @PreAuthorize("hasAnyAuthority('EDIT_ORGANISATION_UNITS', 'EDIT_EMPLOYMENT_INSTITUTION')")
    @OrgUnitEditCheck
    public boolean canEditOrganisationUnit() {
        return true;
    }

    @GetMapping
    public Page<OrganisationUnitDTO> getAllOrganisationUnits(Pageable pageable) {
        return organisationUnitService.findOrganisationUnits(pageable);
    }

    @GetMapping("/count")
    public Long countAll() {
        return organisationUnitService.getOrganisationUnitsCount();
    }

    @GetMapping("/{organisationUnitId}")
    public OrganisationUnitDTO getOrganisationUnit(@PathVariable Integer organisationUnitId) {
        return organisationUnitService.readOrganisationUnitById(organisationUnitId);
    }

    @GetMapping("/sub-units/{organisationUnitId}")
    public Page<OrganisationUnitIndex> getOUSubUnits(@PathVariable Integer organisationUnitId,
                                                     Pageable pageable) {
        return organisationUnitService.getOUSubUnits(organisationUnitId, pageable);
    }

    @GetMapping("/old-id/{organisationUnitOldId}")
    public OrganisationUnitDTO getOrganisationUnitForOldId(
        @PathVariable Integer organisationUnitOldId) {
        return organisationUnitService.readOrganisationUnitForOldId(organisationUnitOldId);
    }

    @GetMapping("/import-identifier/{importId}")
    public OrganisationUnitIndex getOrganisationUnitByScopusAfid(@PathVariable String importId) {
        return organisationUnitService.findOrganisationUnitByImportId(importId);
    }

    @GetMapping("/simple-search")
    public Page<OrganisationUnitIndex> searchOrganisationUnitSimple(
        @RequestParam("tokens")
        @NotNull(message = "You have to provide a valid search input.") List<String> tokens,
        @RequestParam(value = "personId", required = false) Integer personId,
        @RequestParam(value = "topLevelInstitutionId", required = false)
        Integer topLevelInstitutionId,
        @RequestParam(required = false) Boolean onlyReturnOnesWhichCanHarvest,
        Pageable pageable) {
        StringUtil.sanitizeTokens(tokens);
        return organisationUnitService.searchOrganisationUnits(tokens, pageable,
            SearchRequestType.SIMPLE, personId, topLevelInstitutionId,
            onlyReturnOnesWhichCanHarvest);
    }

    @GetMapping("/advanced-search")
    public Page<OrganisationUnitIndex> searchOrganisationUnitAdvanced(
        @RequestParam("tokens")
        @NotNull(message = "You have to provide a valid search input.") List<String> tokens,
        Pageable pageable) {
        return organisationUnitService.searchOrganisationUnits(tokens, pageable,
            SearchRequestType.ADVANCED, null, null, null);
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    @PreAuthorize("hasAuthority('EDIT_ORGANISATION_UNITS')")
    @Idempotent
    public OrganisationUnitDTO createOrganisationUnit(
        @RequestBody @Valid OrganisationUnitRequestDTO organisationUnitRequestDTO) {
        return organisationUnitService.createOrganisationUnit(organisationUnitRequestDTO, true);
    }


    @PutMapping("/{organisationUnitId}")
    @PreAuthorize("hasAnyAuthority('EDIT_ORGANISATION_UNITS', 'EDIT_EMPLOYMENT_INSTITUTION')")
    @OrgUnitEditCheck
    @ResponseStatus(HttpStatus.OK)
    public OrganisationUnitDTO updateOrganisationUnit(
        @RequestBody @Valid OrganisationUnitRequestDTO organisationUnitRequestDTO,
        @PathVariable Integer organisationUnitId) {
        var organisationUnit =
            organisationUnitService.editOrganisationUnit(organisationUnitId,
                organisationUnitRequestDTO);
        return OrganisationUnitConverter.toDTO(organisationUnit);
    }

    @PatchMapping("/{organisationUnitId}/approve-status")
    @PreAuthorize("hasAuthority('EDIT_ORGANISATION_UNITS')")
    @ResponseStatus(HttpStatus.OK)
    public OrganisationUnitDTO updateOrganisationUnitApproveStatus(
        @RequestBody @Valid ApproveStatus approveStatus,
        @PathVariable Integer organisationUnitId) {
        var organisationUnit =
            organisationUnitService.editOrganisationalUnitApproveStatus(approveStatus,
                organisationUnitId);
        return OrganisationUnitConverter.toDTO(organisationUnit);
    }

    @DeleteMapping("/{organisationUnitId}")
    @PreAuthorize("hasAuthority('EDIT_ORGANISATION_UNITS')")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void deleteOrganisationUnit(@PathVariable Integer organisationUnitId) {
        organisationUnitService.deleteOrganisationUnit(organisationUnitId);
        deduplicationService.deleteSuggestion(organisationUnitId, EntityType.ORGANISATION_UNIT);
    }

    @DeleteMapping("/force/{organisationUnitId}")
    @PreAuthorize("hasAuthority('FORCE_DELETE_ENTITIES')")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void forceDeleteOrganisationUnit(@PathVariable Integer organisationUnitId) {
        organisationUnitService.forceDeleteOrganisationUnit(organisationUnitId);
        deduplicationService.deleteSuggestion(organisationUnitId, EntityType.ORGANISATION_UNIT);
    }

    @GetMapping("/admin-exists/{organisationUnitId}")
    @PreAuthorize("hasAuthority('EDIT_ORGANISATION_UNITS')")
    public boolean doesAdminExistForOrganisationUnit(@PathVariable Integer organisationUnitId) {
        return organisationUnitService.checkIfInstitutionalAdminsExist(organisationUnitId);
    }

    @GetMapping("/identifier-usage/{organisationUnitId}")
    @PreAuthorize("hasAnyAuthority('EDIT_ORGANISATION_UNITS', 'EDIT_EMPLOYMENT_INSTITUTION')")
    @OrgUnitEditCheck
    public boolean checkIdentifierUsage(@PathVariable Integer organisationUnitId,
                                        @RequestParam String identifier) {
        return organisationUnitService.isIdentifierInUse(identifier, organisationUnitId);
    }

    @GetMapping("/fields")
    public List<Triple<String, List<MultilingualContentDTO>, String>> getSearchFields(
        @RequestParam("export") Boolean onlyExportFields) {
        return organisationUnitService.getSearchFields(onlyExportFields);
    }

    @PatchMapping("/logo/{organisationUnitId}")
    @PreAuthorize("hasAnyAuthority('EDIT_ORGANISATION_UNITS', 'EDIT_EMPLOYMENT_INSTITUTION')")
    @ResponseStatus(HttpStatus.OK)
    @OrgUnitEditCheck
    public String updateOrganisationUnitLogo(@ModelAttribute
                                             @Valid ProfilePhotoOrLogoDTO logoDTO,
                                             @PathVariable Integer organisationUnitId)
        throws IOException {
        return organisationUnitService.setOrganisationUnitLogo(organisationUnitId, logoDTO);
    }

    @DeleteMapping("/logo/{organisationUnitId}")
    @PreAuthorize("hasAnyAuthority('EDIT_ORGANISATION_UNITS', 'EDIT_EMPLOYMENT_INSTITUTION')")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    @OrgUnitEditCheck
    public void removeOrganisationUnitLogo(@PathVariable Integer organisationUnitId) {
        organisationUnitService.removeOrganisationUnitLogo(organisationUnitId);
    }
}
