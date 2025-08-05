package rs.teslaris.importer.controller;

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
import rs.teslaris.importer.dto.OrganisationUnitImportSourceConfigurationDTO;
import rs.teslaris.importer.service.interfaces.OrganisationUnitImportSourceConfigurationService;

@RestController
@RequestMapping("/api/import-source-configuration")
@RequiredArgsConstructor
public class OrganisationUnitImportSourceConfigurationController {

    private final OrganisationUnitImportSourceConfigurationService
        organisationUnitImportSourceConfigurationService;


    @GetMapping("/{organisationUnitId}")
    @PreAuthorize("hasAuthority('CONFIGURE_HARVEST_SOURCES')")
    @OrgUnitEditCheck
    public OrganisationUnitImportSourceConfigurationDTO readConfigurationForInstitution(
        @PathVariable Integer organisationUnitId) {
        return organisationUnitImportSourceConfigurationService.readConfigurationForInstitution(
            organisationUnitId);
    }

    @PatchMapping("/{organisationUnitId}")
    @PreAuthorize("hasAuthority('CONFIGURE_HARVEST_SOURCES')")
    @OrgUnitEditCheck
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void saveConfigurationForInstitution(@PathVariable Integer organisationUnitId,
                                                @RequestBody @Valid
                                                OrganisationUnitImportSourceConfigurationDTO configuration) {
        organisationUnitImportSourceConfigurationService.saveConfigurationForInstitution(
            organisationUnitId, configuration);
    }
}
