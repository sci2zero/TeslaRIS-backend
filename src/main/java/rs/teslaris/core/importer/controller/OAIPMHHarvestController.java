package rs.teslaris.core.importer.controller;

import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import rs.teslaris.core.dto.person.BasicPersonDTO;
import rs.teslaris.core.importer.service.interfaces.OAIPMHHarvester;
import rs.teslaris.core.importer.service.interfaces.OAIPMHLoader;
import rs.teslaris.core.importer.utility.OAIPMHDataSet;
import rs.teslaris.core.importer.utility.OAIPMHSource;

@RestController
@RequestMapping("/api/import")
@RequiredArgsConstructor
public class OAIPMHHarvestController {

    private final OAIPMHHarvester oaipmhHarvester;

    private final OAIPMHLoader oaipmhLoader;


    @GetMapping("/harvest")
    public void harvest(@RequestParam("dataSet") OAIPMHDataSet dataSet,
                        @RequestParam("source") OAIPMHSource source) {
        oaipmhHarvester.harvest(dataSet, source);
    }

    @GetMapping("/load")
    public void loadAuto(@RequestParam("dataSet") OAIPMHDataSet dataSet,
                         @RequestParam("performIndex") Boolean performIndex) {
        oaipmhLoader.loadRecordsAuto(dataSet, performIndex);
    }

    @GetMapping("/load-wizard/persons")
    public BasicPersonDTO loadPersonsWizard() {
        return oaipmhLoader.loadRecordsWizard(OAIPMHDataSet.PERSONS);
    }

    @GetMapping("/load-wizard/events")
    public BasicPersonDTO loadEventsWizard() {
        return oaipmhLoader.loadRecordsWizard(OAIPMHDataSet.EVENTS);
    }
}
