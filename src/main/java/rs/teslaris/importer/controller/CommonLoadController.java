package rs.teslaris.importer.controller;

import jakarta.annotation.Nullable;
import java.util.Objects;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import rs.teslaris.core.annotation.Idempotent;
import rs.teslaris.core.annotation.Traceable;
import rs.teslaris.core.dto.document.ProceedingsDTO;
import rs.teslaris.core.dto.document.PublicationSeriesDTO;
import rs.teslaris.core.dto.institution.OrganisationUnitDTO;
import rs.teslaris.core.dto.person.PersonResponseDTO;
import rs.teslaris.core.model.user.UserRole;
import rs.teslaris.core.service.interfaces.user.UserService;
import rs.teslaris.core.util.jwt.JwtUtil;
import rs.teslaris.importer.service.interfaces.CommonLoader;

@RestController
@RequestMapping("/api/load")
@RequiredArgsConstructor
@Traceable
public class CommonLoadController {

    private final CommonLoader loader;

    private final UserService userService;

    private final JwtUtil tokenUtil;


    @PatchMapping("/skip")
    @PreAuthorize("hasAuthority('PERFORM_IMPORT_AND_LOADING')")
    public void skipRecord(
        @RequestParam(name = "institutionId", required = false) Integer providedInstitutionId,
        @RequestHeader("Authorization") String bearerToken) {
        loader.skipRecord(tokenUtil.extractUserIdFromToken(bearerToken),
            getOrganisationUnitIdFromToken(bearerToken, providedInstitutionId));
    }

    @PatchMapping("/mark-as-loaded")
    @PreAuthorize("hasAuthority('PERFORM_IMPORT_AND_LOADING')")
    public void markRecordAsLoaded(
        @RequestParam(name = "institutionId", required = false) Integer providedInstitutionId,
        @RequestParam(required = false) Integer oldDocumentId,
        @RequestParam(required = false) Boolean deleteOldDocument,
        @RequestHeader("Authorization") String bearerToken) {
        loader.markRecordAsLoaded(tokenUtil.extractUserIdFromToken(bearerToken),
            getOrganisationUnitIdFromToken(bearerToken, providedInstitutionId), oldDocumentId,
            deleteOldDocument);
    }

    @GetMapping("/load-wizard/count-remaining")
    @PreAuthorize("hasAuthority('PERFORM_IMPORT_AND_LOADING')")
    public Integer getRemainingRecordsCount(
        @RequestParam(name = "institutionId", required = false) Integer providedInstitutionId,
        @RequestHeader("Authorization") String bearerToken) {
        return loader.countRemainingDocumentsForLoading(
            tokenUtil.extractUserIdFromToken(bearerToken),
            getOrganisationUnitIdFromToken(bearerToken, providedInstitutionId));
    }

    @GetMapping("/load-wizard")
    @PreAuthorize("hasAuthority('PERFORM_IMPORT_AND_LOADING')")
    @SuppressWarnings("unchecked")
    public <R> R loadUsingWizard(
        @RequestParam(name = "institutionId", required = false) Integer providedInstitutionId,
        @RequestHeader("Authorization") String bearerToken) {
        var returnDto =
            loader.loadRecordsWizard(tokenUtil.extractUserIdFromToken(bearerToken),
                getOrganisationUnitIdFromToken(bearerToken, providedInstitutionId));

        if (Objects.isNull(returnDto)) {
            return loader.loadSkippedRecordsWizard(
                tokenUtil.extractUserIdFromToken(bearerToken),
                getOrganisationUnitIdFromToken(bearerToken, providedInstitutionId));
        }

        return (R) returnDto;
    }

    @PostMapping("/institution/{scopusAfid}")
    @PreAuthorize("hasAuthority('PERFORM_IMPORT_AND_LOADING')")
    @Idempotent
    public OrganisationUnitDTO createInstitution(
        @RequestParam(name = "institutionId", required = false) Integer providedInstitutionId,
        @RequestHeader("Authorization") String bearerToken,
        @PathVariable String scopusAfid) {
        return loader.createInstitution(scopusAfid,
            tokenUtil.extractUserIdFromToken(bearerToken),
            getOrganisationUnitIdFromToken(bearerToken, providedInstitutionId));
    }

    @PostMapping("/person/{scopusAuthorId}")
    @PreAuthorize("hasAuthority('PERFORM_IMPORT_AND_LOADING')")
    @Idempotent
    public PersonResponseDTO createPerson(
        @RequestParam(name = "institutionId", required = false) Integer providedInstitutionId,
        @RequestHeader("Authorization") String bearerToken,
        @PathVariable String scopusAuthorId) {
        return loader.createPerson(scopusAuthorId, tokenUtil.extractUserIdFromToken(bearerToken),
            getOrganisationUnitIdFromToken(bearerToken, providedInstitutionId));
    }

    @PostMapping("/journal")
    @PreAuthorize("hasAuthority('PERFORM_IMPORT_AND_LOADING')")
    @Idempotent
    public PublicationSeriesDTO createJournal(
        @RequestParam(name = "institutionId", required = false) Integer providedInstitutionId,
        @RequestHeader("Authorization") String bearerToken,
        @RequestParam String eIssn,
        @RequestParam String printIssn) {
        return loader.createJournal(eIssn, printIssn,
            tokenUtil.extractUserIdFromToken(bearerToken),
            getOrganisationUnitIdFromToken(bearerToken, providedInstitutionId));
    }

    @PostMapping("/proceedings")
    @PreAuthorize("hasAuthority('PERFORM_IMPORT_AND_LOADING')")
    @Idempotent
    public ProceedingsDTO createProceedings(
        @RequestParam(name = "institutionId", required = false) Integer providedInstitutionId,
        @RequestHeader("Authorization") String bearerToken) {
        return loader.createProceedings(
            tokenUtil.extractUserIdFromToken(bearerToken),
            getOrganisationUnitIdFromToken(bearerToken, providedInstitutionId));
    }

    @Nullable
    private Integer getOrganisationUnitIdFromToken(String bearerToken,
                                                   Integer providedInstitutionId) {
        var role = tokenUtil.extractUserRoleFromToken(bearerToken);

        if (role.equals(UserRole.INSTITUTIONAL_EDITOR.name())) {
            var userId = tokenUtil.extractUserIdFromToken(bearerToken);
            return userService.getUserOrganisationUnitId(userId);
        } else if (role.equals(UserRole.ADMIN.name())) {
            return providedInstitutionId;
        }

        return null;
    }
}
