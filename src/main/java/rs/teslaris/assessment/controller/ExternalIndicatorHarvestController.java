package rs.teslaris.assessment.controller;

import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import rs.teslaris.assessment.service.interfaces.indicator.ExternalIndicatorHarvestService;

@RestController
@RequestMapping("/api/external-indicator-harvest")
@RequiredArgsConstructor
public class ExternalIndicatorHarvestController {

    private final ExternalIndicatorHarvestService externalIndicatorHarvestService;

    @GetMapping
    public void harvestFromOpenAlex() {
        externalIndicatorHarvestService.performPersonIndicatorHarvest();
        externalIndicatorHarvestService.performOUIndicatorDeduction();
    }
}
