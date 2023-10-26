package rs.teslaris.core.controller;

import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import rs.teslaris.core.harvester.OAIPMHHarvester;
import rs.teslaris.core.harvester.common.OAIPMHDataSet;

@RestController
@RequestMapping("/api/harvest")
@RequiredArgsConstructor
public class OAIPMHHarvestController {

    private final OAIPMHHarvester oaipmhHarvester;


    @GetMapping
    @PreAuthorize("hasAuthority('HARVEST_OAI_PMH_DATA')")
    public void testHarvestForOU(@RequestParam("dataSet") OAIPMHDataSet dataSet) {
        oaipmhHarvester.harvest(dataSet);
    }
}
