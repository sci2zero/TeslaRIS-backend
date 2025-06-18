package rs.teslaris.assessment.controller;

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
import rs.teslaris.assessment.dto.indicator.ExternalIndicatorConfigurationDTO;
import rs.teslaris.assessment.service.interfaces.indicator.ExternalIndicatorConfigurationService;
import rs.teslaris.core.annotation.OrgUnitEditCheck;

@RestController
@RequestMapping("/api/external-indicator-configuration")
@RequiredArgsConstructor
public class ExternalIndicatorConfigurationController {

    private final ExternalIndicatorConfigurationService externalIndicatorConfigurationService;


    @GetMapping("/institution/{organisationUnitId}")
    public ExternalIndicatorConfigurationDTO readConfigurationForInstitution(
        @PathVariable Integer organisationUnitId) {
        return externalIndicatorConfigurationService.readConfigurationForInstitution(
            organisationUnitId);
    }

    @GetMapping("/document/{documentId}")
    public ExternalIndicatorConfigurationDTO readConfigurationForDocument(
        @PathVariable Integer documentId) {
        return externalIndicatorConfigurationService.readConfigurationForDocument(documentId);
    }

    @PatchMapping("/{organisationUnitId}")
    @PreAuthorize("hasAuthority('EDIT_EXT_INDICATOR_CONFIGURATION')")
    @OrgUnitEditCheck
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void updateConfigurationForInstitution(@PathVariable
                                                  Integer organisationUnitId,
                                                  @RequestBody
                                                  ExternalIndicatorConfigurationDTO configuration) {
        externalIndicatorConfigurationService.updateConfiguration(configuration,
            organisationUnitId);
    }
}
