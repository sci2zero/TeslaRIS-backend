package rs.teslaris.core.importer.controller;

import io.jsonwebtoken.MalformedJwtException;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestHeader;
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

    @Value("${harvester.api-key}")
    private String apiKey;


    @GetMapping("/harvest")
    public void harvest(@RequestParam("dataSet") OAIPMHDataSet dataSet,
                        @RequestParam("source") OAIPMHSource source,
                        @RequestHeader("X-API-KEY") String userApiKey) {
        if (!apiKey.equals(userApiKey)) {
            throw new MalformedJwtException("Bad API key");
        }

        oaipmhHarvester.harvest(dataSet, source);
    }

    @GetMapping("/load")
    public void loadAuto(@RequestParam("dataSet") OAIPMHDataSet dataSet,
                         @RequestParam("performIndex") Boolean performIndex,
                         @RequestHeader("X-API-KEY") String userApiKey) {
        if (!apiKey.equals(userApiKey)) {
            throw new MalformedJwtException("Bad API key");
        }

        oaipmhLoader.loadRecordsAuto(dataSet, performIndex);
    }

    @GetMapping("/load-wizard/persons")
    public BasicPersonDTO loadPersonsWizard(@RequestHeader("X-API-KEY") String userApiKey) {
        if (!apiKey.equals(userApiKey)) {
            throw new MalformedJwtException("Bad API key");
        }

        return oaipmhLoader.loadRecordsWizard(OAIPMHDataSet.PERSONS);
    }

    @GetMapping("/load-wizard/events")
    public BasicPersonDTO loadEventsWizard(@RequestHeader("X-API-KEY") String userApiKey) {
        if (!apiKey.equals(userApiKey)) {
            throw new MalformedJwtException("Bad API key");
        }

        return oaipmhLoader.loadRecordsWizard(OAIPMHDataSet.EVENTS);
    }
}
