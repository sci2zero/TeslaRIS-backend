package rs.teslaris.core.importer.controller;

import java.util.Objects;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import rs.teslaris.core.importer.dto.RemainingRecordsCountResponseDTO;
import rs.teslaris.core.importer.service.interfaces.OAIPMHHarvester;
import rs.teslaris.core.importer.service.interfaces.OAIPMHLoader;
import rs.teslaris.core.importer.utility.DataSet;
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
    public void harvest(@RequestParam("dataSet") DataSet dataSet,
                        @RequestParam("source") OAIPMHSource source,
                        @RequestHeader("Authorization") String bearerToken) {
        oaipmhHarvester.harvest(dataSet, source,
            tokenUtil.extractUserIdFromToken(bearerToken));
    }

    @GetMapping("/load")
    public void loadAuto(@RequestParam("dataSet") DataSet dataSet,
                         @RequestParam("performIndex") Boolean performIndex,
                         @RequestHeader("Authorization") String bearerToken) {
        oaipmhLoader.loadRecordsAuto(dataSet, performIndex,
            tokenUtil.extractUserIdFromToken(bearerToken));
    }

    @PatchMapping("/skip")
    public void skipRecord(@RequestHeader("Authorization") String bearerToken,
                           @RequestParam("dataSet") DataSet dataSet) {
        oaipmhLoader.skipRecord(dataSet,
            tokenUtil.extractUserIdFromToken(bearerToken));
    }

    @PatchMapping("/mark-as-loaded")
    public void markRecordAsLoaded(@RequestHeader("Authorization") String bearerToken,
                                   @RequestParam("dataSet") DataSet dataSet) {
        oaipmhLoader.markRecordAsLoaded(dataSet,
            tokenUtil.extractUserIdFromToken(bearerToken));
    }

    @GetMapping("/load-wizard/count-remaining")
    public RemainingRecordsCountResponseDTO getRemainingRecordsCount(
        @RequestHeader("Authorization") String bearerToken) {
        return oaipmhLoader.countRemainingDocumentsForLoading(
            tokenUtil.extractUserIdFromToken(bearerToken));
    }

    @GetMapping("/load-wizard")
    @SuppressWarnings("unchecked")
    private <R> R loadUsingWizard(@RequestParam("dataSet") DataSet dataSet,
                                  @RequestHeader("Authorization") String bearerToken) {
        var returnDto = oaipmhLoader.loadRecordsWizard(dataSet,
            tokenUtil.extractUserIdFromToken(bearerToken));

        if (Objects.isNull(returnDto)) {
            return oaipmhLoader.loadSkippedRecordsWizard(dataSet,
                tokenUtil.extractUserIdFromToken(bearerToken));
        }

        return (R) returnDto;
    }
}
