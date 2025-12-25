package rs.teslaris.assessment.controller;

import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;
import rs.teslaris.assessment.service.interfaces.indicator.ExternalIndicatorHarvestService;
import rs.teslaris.core.annotation.OrgUnitEditCheck;
import rs.teslaris.core.annotation.PersonEditCheck;

@RestController
@RequestMapping("/api/external-indicator")
@RequiredArgsConstructor
public class ExternalIndicatorHarvestController {

    private final ExternalIndicatorHarvestService externalIndicatorHarvestService;


    @PostMapping("/harvest-person/{personId}")
    @PersonEditCheck
    @PreAuthorize("hasAuthority('EDIT_EXT_INDICATOR_CONFIGURATION')")
    public void harvestExternalIndicatorsForPerson(@PathVariable Integer personId) {
        externalIndicatorHarvestService.performIndicatorHavestForSinglePerson(personId);
    }

    @PostMapping("/deduce-institution/{organisationUnitId}")
    @OrgUnitEditCheck
    @PreAuthorize("hasAuthority('EDIT_EXT_INDICATOR_CONFIGURATION')")
    public void deduceExternalIndicatorsForInstitution(@PathVariable Integer organisationUnitId) {
        externalIndicatorHarvestService.performIndicatorDeductionForSingleInstitution(
            organisationUnitId);
    }

    @PostMapping("/harvest-all")
    @PreAuthorize("hasAuthority('EDIT_EXT_INDICATOR_CONFIGURATION')")
    @ResponseStatus(HttpStatus.ACCEPTED)
    public void harvestAllExternalIndicators() {
        externalIndicatorHarvestService.harvestAllManually();
    }
}
