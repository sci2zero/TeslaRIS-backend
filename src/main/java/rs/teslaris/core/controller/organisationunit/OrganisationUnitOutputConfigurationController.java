package rs.teslaris.core.controller.organisationunit;

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
import rs.teslaris.core.dto.institution.OrganisationUnitOutputConfigurationDTO;
import rs.teslaris.core.service.interfaces.institution.OrganisationUnitOutputConfigurationService;

@RestController
@RequestMapping("/api/organisation-unit/output-configuration")
@RequiredArgsConstructor
public class OrganisationUnitOutputConfigurationController {

    private final OrganisationUnitOutputConfigurationService
        organisationUnitOutputConfigurationService;

    @GetMapping("/{organisationUnitId}")
    public OrganisationUnitOutputConfigurationDTO readConfigurationForInstitution(
        @PathVariable Integer organisationUnitId) {
        return organisationUnitOutputConfigurationService.readOutputConfigurationForOrganisationUnit(
            organisationUnitId);
    }

    @PatchMapping("/{organisationUnitId}")
    @PreAuthorize("hasAuthority('SAVE_OU_OUTPUT_CONFIGURATION')")
    @OrgUnitEditCheck
    @ResponseStatus(HttpStatus.ACCEPTED)
    public OrganisationUnitOutputConfigurationDTO saveConfigurationForInstitution(
        @PathVariable Integer organisationUnitId,
        @RequestBody @Valid OrganisationUnitOutputConfigurationDTO configuration) {
        return organisationUnitOutputConfigurationService.saveConfiguration(configuration,
            organisationUnitId);
    }
}
