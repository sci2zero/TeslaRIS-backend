package rs.teslaris.core.controller;

import io.jsonwebtoken.MalformedJwtException;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import rs.teslaris.core.importer.OAIPMHHarvester;
import rs.teslaris.core.importer.common.OAIPMHDataSet;

@RestController
@RequestMapping("/api/import")
@RequiredArgsConstructor
public class OAIPMHHarvestController {

    private final OAIPMHHarvester oaipmhHarvester;

    @Value("${harvester.api-key}")
    private String apiKey;


    @GetMapping("/harvest")
    public void harvest(@RequestParam("dataSet") OAIPMHDataSet dataSet,
                        @RequestHeader("X-API-KEY") String userApiKey) {
        if (!apiKey.equals(userApiKey)) {
            throw new MalformedJwtException("Bad API key");
        }

        oaipmhHarvester.harvest(dataSet);
    }

    @GetMapping("/load")
    public void load(@RequestParam("dataSet") OAIPMHDataSet dataSet,
                     @RequestParam("performIndex") Boolean performIndex,
                     @RequestHeader("X-API-KEY") String userApiKey) {
        if (!apiKey.equals(userApiKey)) {
            throw new MalformedJwtException("Bad API key");
        }

        oaipmhHarvester.loadRecords(dataSet, performIndex);
    }
}
