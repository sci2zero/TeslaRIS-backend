package rs.teslaris.core.importer.controller;

import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import rs.teslaris.core.dto.document.ConferenceDTO;
import rs.teslaris.core.dto.document.PatentDTO;
import rs.teslaris.core.dto.institution.OrganisationUnitWizardDTO;
import rs.teslaris.core.dto.person.BasicPersonDTO;
import rs.teslaris.core.importer.service.interfaces.OAIPMHHarvester;
import rs.teslaris.core.importer.service.interfaces.OAIPMHLoader;
import rs.teslaris.core.importer.utility.OAIPMHDataSet;
import rs.teslaris.core.importer.utility.OAIPMHSource;
import rs.teslaris.core.util.jwt.JwtUtil;

@RestController
@RequestMapping("/api/import")
@RequiredArgsConstructor
public class OAIPMHHarvestController {

    private final OAIPMHHarvester oaipmhHarvester;

    private final OAIPMHLoader oaipmhLoader;

    private final JwtUtil tokenUtil;


    @GetMapping("/harvest")
    public void harvest(@RequestParam("dataSet") OAIPMHDataSet dataSet,
                        @RequestParam("source") OAIPMHSource source,
                        @RequestHeader("Authorization") String bearerToken) {
        oaipmhHarvester.harvest(dataSet, source,
            tokenUtil.extractUserIdFromToken(bearerToken.split(" ")[1]));
    }

    @GetMapping("/load")
    public void loadAuto(@RequestParam("dataSet") OAIPMHDataSet dataSet,
                         @RequestParam("performIndex") Boolean performIndex,
                         @RequestHeader("Authorization") String bearerToken) {
        oaipmhLoader.loadRecordsAuto(dataSet, performIndex,
            tokenUtil.extractUserIdFromToken(bearerToken.split(" ")[1]));
    }

    @GetMapping("/load-wizard/persons")
    public BasicPersonDTO loadPersonsWizard(@RequestHeader("Authorization") String bearerToken) {
        return oaipmhLoader.loadRecordsWizard(OAIPMHDataSet.PERSONS,
            tokenUtil.extractUserIdFromToken(bearerToken.split(" ")[1]));
    }

    @GetMapping("/load-wizard/events")
    public ConferenceDTO loadEventsWizard(@RequestHeader("Authorization") String bearerToken) {
        return oaipmhLoader.loadRecordsWizard(OAIPMHDataSet.EVENTS,
            tokenUtil.extractUserIdFromToken(bearerToken.split(" ")[1]));
    }

    @GetMapping("/load-wizard/patents")
    public PatentDTO loadPatentsWizard(@RequestHeader("Authorization") String bearerToken) {
        return oaipmhLoader.loadRecordsWizard(OAIPMHDataSet.PATENTS,
            tokenUtil.extractUserIdFromToken(bearerToken.split(" ")[1]));
    }

    @GetMapping("/load-wizard/organisation-units")
    public OrganisationUnitWizardDTO loadOrganisationUnitsWizard(
        @RequestHeader("Authorization") String bearerToken) {
        return oaipmhLoader.loadRecordsWizard(OAIPMHDataSet.ORGANISATION_UNITS,
            tokenUtil.extractUserIdFromToken(bearerToken.split(" ")[1]));
    }
}
