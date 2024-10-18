package rs.teslaris.core.controller;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
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
import rs.teslaris.core.converter.institution.OrganisationUnitConverter;
import rs.teslaris.core.dto.institution.OrganisationUnitDTO;
import rs.teslaris.core.dto.institution.OrganisationUnitRequestDTO;
import rs.teslaris.core.indexmodel.IndexType;
import rs.teslaris.core.indexmodel.OrganisationUnitIndex;
import rs.teslaris.core.model.commontypes.ApproveStatus;
import rs.teslaris.core.service.interfaces.document.DeduplicationService;
import rs.teslaris.core.service.interfaces.person.OrganisationUnitService;
import rs.teslaris.core.util.search.SearchRequestType;
import rs.teslaris.core.util.search.StringUtil;

@RestController
@RequestMapping("/api/organisation-unit")
@RequiredArgsConstructor
public class OrganisationUnitController {

    private final OrganisationUnitService organisationUnitService;

    private final DeduplicationService deduplicationService;


    @GetMapping("/{organisationUnitId}/can-edit")
    @PreAuthorize("hasAuthority('EDIT_ORGANISATION_UNITS')")
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
        return OrganisationUnitConverter.toDTO(
            organisationUnitService.findOrganisationUnitById(organisationUnitId));
    }

    @GetMapping("/old-id/{organisationUnitOldId}")
    public OrganisationUnitDTO getOrganisationUnitForOldId(
        @PathVariable Integer organisationUnitOldId) {
        return organisationUnitService.readOrganisationUnitForOldId(organisationUnitOldId);
    }

    @GetMapping("/scopus-afid/{scopusAfid}")
    public OrganisationUnitIndex getOrganisationUnitByScopusAfid(@PathVariable String scopusAfid) {
        return organisationUnitService.findOrganisationUnitByScopusAfid(scopusAfid);
    }

    @GetMapping("/simple-search")
    public Page<OrganisationUnitIndex> searchOrganisationUnitSimple(
        @RequestParam("tokens")
        @NotNull(message = "You have to provide a valid search input.") List<String> tokens,
        Pageable pageable) {
        StringUtil.sanitizeTokens(tokens);
        return organisationUnitService.searchOrganisationUnits(tokens, pageable,
            SearchRequestType.SIMPLE);
    }

    @GetMapping("/advanced-search")
    public Page<OrganisationUnitIndex> searchOrganisationUnitAdvanced(
        @RequestParam("tokens")
        @NotNull(message = "You have to provide a valid search input.") List<String> tokens,
        Pageable pageable) {
        return organisationUnitService.searchOrganisationUnits(tokens, pageable,
            SearchRequestType.ADVANCED);
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
    @PreAuthorize("hasAuthority('EDIT_ORGANISATION_UNITS')")
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
    public OrganisationUnitDTO updateOrganisationUnit(
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
        deduplicationService.deleteSuggestion(organisationUnitId, IndexType.ORGANISATION_UNIT);
    }
}
