package rs.teslaris.core.controller;

import javax.validation.Valid;
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
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;
import rs.teslaris.core.converter.institution.OrganisationUnitConverter;
import rs.teslaris.core.dto.institution.OrganisationUnitDTO;
import rs.teslaris.core.dto.institution.OrganisationUnitDTORequest;
import rs.teslaris.core.model.commontypes.ApproveStatus;
import rs.teslaris.core.service.OrganisationUnitService;

@RestController
@RequestMapping("/api/organisation-units")
@RequiredArgsConstructor
public class OrganisationUnitController {

    private final OrganisationUnitService organisationUnitService;

    @GetMapping
    public Page<OrganisationUnitDTO> getAllOrganisationUnits(Pageable pageable) {
        return organisationUnitService.findOrganisationUnits(pageable);
    }

    @GetMapping("/{organisationUnitId}")
    public OrganisationUnitDTO getOrganisationUnit(@PathVariable Integer organisationUnitId) {
        return OrganisationUnitConverter.toDTO(
            organisationUnitService.findOrganisationUnitById(organisationUnitId));
    }


    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    @PreAuthorize("hasAuthority('EDIT_ORGANISATION_UNITS')")
    public OrganisationUnitDTO createOrganisationUnit(
        @RequestBody @Valid OrganisationUnitDTORequest organisationUnitDTORequest) {
        var organisationUnit =
            organisationUnitService.createOrganisationalUnit(organisationUnitDTORequest);
        return OrganisationUnitConverter.toDTO(organisationUnit);
    }


    @PutMapping("/{organisationUnitId}")
    @PreAuthorize("hasAuthority('EDIT_ORGANISATION_UNITS')")
    @ResponseStatus(HttpStatus.OK)
    public OrganisationUnitDTO updateOrganisationUnit(
        @RequestBody @Valid OrganisationUnitDTORequest organisationUnitDTORequest,
        @PathVariable Integer organisationUnitId) {
        var organisationUnit =
            organisationUnitService.editOrganisationalUnit(organisationUnitDTORequest,
                organisationUnitId);
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
        organisationUnitService.deleteOrganisationalUnit(organisationUnitId);
    }

}
