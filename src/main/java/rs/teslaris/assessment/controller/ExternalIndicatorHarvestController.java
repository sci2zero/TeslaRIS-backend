package rs.teslaris.assessment.controller;

import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import rs.teslaris.assessment.service.interfaces.indicator.ExternalIndicatorHarvestService;

@RestController
@RequestMapping("/api/external-indicator")
@RequiredArgsConstructor
public class ExternalIndicatorHarvestController {

    private final ExternalIndicatorHarvestService externalIndicatorHarvestService;


    @PostMapping("/harvest-person/{personId}")
    @PreAuthorize("hasAuthority('EDIT_EXT_INDICATOR_CONFIGURATION')")
    public void harvestExternalIndicatorsForPerson(@PathVariable Integer personId) {
        externalIndicatorHarvestService.performIndicatorHavestForSinglePerson(personId);
    }

    @PostMapping("/deduce-institution/{institutionId}")
    @PreAuthorize("hasAuthority('EDIT_EXT_INDICATOR_CONFIGURATION')")
    public void deduceExternalIndicatorsForInstitution(@PathVariable Integer institutionId) {
        externalIndicatorHarvestService.performIndicatorDeductionForSingleInstitution(
            institutionId);
    }
}
