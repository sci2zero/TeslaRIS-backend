package rs.teslaris.core.importer.controller;

import java.util.Objects;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import rs.teslaris.core.annotation.Idempotent;
import rs.teslaris.core.dto.document.ProceedingsDTO;
import rs.teslaris.core.dto.document.PublicationSeriesDTO;
import rs.teslaris.core.dto.institution.OrganisationUnitDTO;
import rs.teslaris.core.dto.person.PersonResponseDTO;
import rs.teslaris.core.importer.service.interfaces.CommonLoader;
import rs.teslaris.core.util.jwt.JwtUtil;

@RestController
@RequestMapping("/api/load")
@RequiredArgsConstructor
public class CommonLoadController {

    private final CommonLoader loader;

    private final JwtUtil tokenUtil;


    @PatchMapping("/skip")
    public void skipRecord(@RequestHeader("Authorization") String bearerToken) {
        loader.skipRecord(tokenUtil.extractUserIdFromToken(bearerToken));
    }

    @PatchMapping("/mark-as-loaded")
    public void markRecordAsLoaded(@RequestHeader("Authorization") String bearerToken) {
        loader.markRecordAsLoaded(tokenUtil.extractUserIdFromToken(bearerToken));
    }

    @GetMapping("/load-wizard/count-remaining")
    public Integer getRemainingRecordsCount(
        @RequestHeader("Authorization") String bearerToken) {
        return loader.countRemainingDocumentsForLoading(
            tokenUtil.extractUserIdFromToken(bearerToken));
    }

    @GetMapping("/load-wizard")
    @SuppressWarnings("unchecked")
    public <R> R loadUsingWizard(@RequestHeader("Authorization") String bearerToken) {
        var returnDto =
            loader.loadRecordsWizard(tokenUtil.extractUserIdFromToken(bearerToken));

        if (Objects.isNull(returnDto)) {
            return loader.loadSkippedRecordsWizard(
                tokenUtil.extractUserIdFromToken(bearerToken));
        }

        return (R) returnDto;
    }

    @PostMapping("/institution/{scopusAfid}")
    @Idempotent
    public OrganisationUnitDTO createInstitution(@RequestHeader("Authorization") String bearerToken,
                                                 @PathVariable String scopusAfid) {
        return loader.createInstitution(scopusAfid,
            tokenUtil.extractUserIdFromToken(bearerToken));
    }

    @PostMapping("/person/{scopusAuthorId}")
    @Idempotent
    public PersonResponseDTO createPerson(@RequestHeader("Authorization") String bearerToken,
                                          @PathVariable String scopusAuthorId) {
        return loader.createPerson(scopusAuthorId, tokenUtil.extractUserIdFromToken(bearerToken));
    }

    @PostMapping("/journal")
    @Idempotent
    public PublicationSeriesDTO createJournal(@RequestHeader("Authorization") String bearerToken,
                                              @RequestParam String eIssn,
                                              @RequestParam String printIssn) {
        return loader.createJournal(eIssn, printIssn,
            tokenUtil.extractUserIdFromToken(bearerToken));
    }

    @PostMapping("/proceedings")
    @Idempotent
    public ProceedingsDTO createProceedings(
        @RequestHeader("Authorization") String bearerToken) {
        return loader.createProceedings(
            tokenUtil.extractUserIdFromToken(bearerToken));
    }

}
