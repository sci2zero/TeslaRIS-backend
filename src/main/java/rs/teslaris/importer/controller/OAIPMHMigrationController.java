package rs.teslaris.importer.controller;

import java.util.Objects;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import rs.teslaris.core.util.jwt.JwtUtil;
import rs.teslaris.importer.dto.RemainingRecordsCountResponseDTO;
import rs.teslaris.importer.service.interfaces.OAIPMHLoader;
import rs.teslaris.importer.service.interfaces.OAIPMHMigrator;
import rs.teslaris.importer.utility.DataSet;
import rs.teslaris.importer.utility.oaipmh.OAIPMHMigrationSource;

@RestController
@RequestMapping("/api/import")
@RequiredArgsConstructor
public class OAIPMHMigrationController {

    private final OAIPMHMigrator oaipmhMigrator;

    private final OAIPMHLoader oaipmhLoader;

    private final JwtUtil tokenUtil;


    @GetMapping("/harvest")
    @PreAuthorize("hasAuthority('PERFORM_OAI_MIGRATION')")
    public void harvest(@RequestParam("dataSet") DataSet dataSet,
                        @RequestParam("source") OAIPMHMigrationSource source,
                        @RequestHeader("Authorization") String bearerToken) {
        oaipmhMigrator.harvest(dataSet, source,
            tokenUtil.extractUserIdFromToken(bearerToken));
    }

    @GetMapping("/load")
    @PreAuthorize("hasAuthority('PERFORM_OAI_MIGRATION')")
    public void loadAuto(@RequestParam("dataSet") DataSet dataSet,
                         @RequestParam("performIndex") Boolean performIndex,
                         @RequestHeader("Authorization") String bearerToken) {
        oaipmhLoader.loadRecordsAuto(dataSet, performIndex,
            tokenUtil.extractUserIdFromToken(bearerToken));
    }

    @PatchMapping("/skip")
    @PreAuthorize("hasAuthority('PERFORM_OAI_MIGRATION')")
    public void skipRecord(@RequestHeader("Authorization") String bearerToken,
                           @RequestParam("dataSet") DataSet dataSet) {
        oaipmhLoader.skipRecord(dataSet,
            tokenUtil.extractUserIdFromToken(bearerToken));
    }

    @PatchMapping("/mark-as-loaded")
    @PreAuthorize("hasAuthority('PERFORM_OAI_MIGRATION')")
    public void markRecordAsLoaded(@RequestHeader("Authorization") String bearerToken,
                                   @RequestParam("dataSet") DataSet dataSet) {
        oaipmhLoader.markRecordAsLoaded(dataSet,
            tokenUtil.extractUserIdFromToken(bearerToken));
    }

    @GetMapping("/load-wizard/count-remaining")
    @PreAuthorize("hasAuthority('PERFORM_OAI_MIGRATION')")
    public RemainingRecordsCountResponseDTO getRemainingRecordsCount(
        @RequestHeader("Authorization") String bearerToken) {
        return oaipmhLoader.countRemainingDocumentsForLoading(
            tokenUtil.extractUserIdFromToken(bearerToken));
    }

    @GetMapping("/load-wizard")
    @PreAuthorize("hasAuthority('PERFORM_OAI_MIGRATION')")
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
