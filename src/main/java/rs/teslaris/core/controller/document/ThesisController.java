package rs.teslaris.core.controller.document;

import jakarta.validation.Valid;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Objects;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;
import rs.teslaris.core.annotation.Idempotent;
import rs.teslaris.core.annotation.PublicationEditCheck;
import rs.teslaris.core.annotation.Traceable;
import rs.teslaris.core.dto.document.DocumentFileDTO;
import rs.teslaris.core.dto.document.DocumentFileResponseDTO;
import rs.teslaris.core.dto.document.ThesisDTO;
import rs.teslaris.core.dto.document.ThesisLibraryFormatsResponseDTO;
import rs.teslaris.core.dto.document.ThesisResponseDTO;
import rs.teslaris.core.model.commontypes.RecurrenceType;
import rs.teslaris.core.model.document.LibraryFormat;
import rs.teslaris.core.model.document.ThesisAttachmentType;
import rs.teslaris.core.model.document.ThesisType;
import rs.teslaris.core.service.interfaces.document.ThesisService;
import rs.teslaris.core.service.interfaces.institution.OrganisationUnitService;
import rs.teslaris.core.service.interfaces.user.UserService;
import rs.teslaris.core.util.exceptionhandling.exception.ThesisException;
import rs.teslaris.core.util.jwt.JwtUtil;
import rs.teslaris.core.util.signposting.FairSignpostingL1Utility;

@RestController
@RequestMapping("/api/thesis")
@RequiredArgsConstructor
@Traceable
public class ThesisController {

    private final ThesisService thesisService;

    private final JwtUtil tokenUtil;

    private final UserService userService;

    private final OrganisationUnitService organisationUnitService;


    @GetMapping("/{documentId}")
    public ResponseEntity<ThesisResponseDTO> readThesis(
        @PathVariable Integer documentId) {
        var dto = thesisService.readThesisById(documentId);

        return ResponseEntity.ok()
            .headers(FairSignpostingL1Utility.constructHeaders(dto, "/api/thesis"))
            .body(dto);
    }

    @GetMapping("/old-id/{oldId}")
    public ThesisResponseDTO readThesisByOldId(@PathVariable Integer oldId) {
        return thesisService.readThesisByOldId(oldId);
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    @PublicationEditCheck("CREATE")
    @Idempotent
    public ThesisDTO createThesis(@RequestBody @Valid ThesisDTO thesis,
                                  @RequestHeader("Authorization") String bearerToken) {
        performReferenceAdditionChecks(thesis, bearerToken);

        var savedThesis = thesisService.createThesis(thesis, true);
        thesis.setId(savedThesis.getId());
        return thesis;
    }

    @PutMapping("/{documentId}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    @PublicationEditCheck
    public void editThesis(@PathVariable Integer documentId,
                           @RequestBody @Valid ThesisDTO thesis,
                           @RequestHeader("Authorization") String bearerToken) {
        performReferenceAdditionChecks(thesis, bearerToken);

        thesisService.editThesis(documentId, thesis);
    }

    @DeleteMapping("/{documentId}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    @PublicationEditCheck
    public void deleteThesis(@PathVariable Integer documentId) {
        thesisService.deleteThesis(documentId);
    }

    @PatchMapping("/{attachmentType}/{documentId}")
    @PreAuthorize("hasAuthority('MANAGE_THESIS_ATTACHMENTS')")
    @PublicationEditCheck("THESIS")
    @Idempotent
    public DocumentFileResponseDTO addThesisAttachment(@PathVariable
                                                       ThesisAttachmentType attachmentType,
                                                       @PathVariable Integer documentId,
                                                       @ModelAttribute
                                                       @Valid DocumentFileDTO file) {
        return thesisService.addThesisAttachment(documentId, file, attachmentType);
    }

    @PatchMapping("/put-on-public-review/{documentId}")
    @PreAuthorize("hasAuthority('PUT_THESIS_ON_PUBLIC_REVIEW')")
    @PublicationEditCheck("THESIS")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void putOnPublicReview(@PathVariable Integer documentId,
                                  @RequestParam(name = "continue", required = false, defaultValue = "false")
                                  Boolean continueLastReview,
                                  @RequestParam(name = "shortened", required = false, defaultValue = "false")
                                  Boolean shortened) {
        thesisService.putOnPublicReview(documentId, continueLastReview, shortened);
    }

    @PatchMapping("/remove-from-public-review/{documentId}")
    @PreAuthorize("hasAuthority('REMOVE_THESIS_FROM_PUBLIC_REVIEW')")
    @PublicationEditCheck("THESIS")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void removeFromPublicReview(@PathVariable Integer documentId) {
        thesisService.removeFromPublicReview(documentId);
    }

    @PatchMapping("/archive/{documentId}")
    @PreAuthorize("hasAuthority('ARCHIVE_THESIS')")
    @PublicationEditCheck("THESIS")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void archiveThesis(@PathVariable Integer documentId) {
        thesisService.archiveThesis(documentId);
    }

    @PatchMapping("/unarchive/{documentId}")
    @PreAuthorize("hasAuthority('UNARCHIVE_THESIS')")
    @PublicationEditCheck("THESIS")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void unarchiveThesis(@PathVariable Integer documentId) {
        thesisService.unarchiveThesis(documentId);
    }

    @DeleteMapping("/{attachmentType}/{documentId}/{documentFileId}")
    @PreAuthorize("hasAuthority('DELETE_THESIS_ATTACHMENTS')")
    @PublicationEditCheck("THESIS")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void deleteThesisAttachment(@PathVariable
                                       ThesisAttachmentType attachmentType,
                                       @PathVariable Integer documentId,
                                       @PathVariable Integer documentFileId) {
        thesisService.deleteThesisAttachment(documentId, documentFileId, attachmentType);
    }

    @PatchMapping("/make-official/{documentId}/{documentFileId}")
    @PreAuthorize("hasAuthority('PROMOTE_PRELIMINARY_ATTACHMENTS')")
    @PublicationEditCheck("THESIS")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void movePreliminaryAttachmentToOfficial(@PathVariable Integer documentId,
                                                    @PathVariable Integer documentFileId) {
        thesisService.transferPreliminaryFileToOfficial(documentId, documentFileId);
    }

    @GetMapping("/library-formats/{documentId}")
    public ThesisLibraryFormatsResponseDTO getAllLibraryReferenceFormats(
        @PathVariable Integer documentId) {
        return thesisService.getLibraryReferenceFormat(documentId);
    }

    @GetMapping("/library-format/{documentId}/{libraryFormat}")
    public ResponseEntity<String> getSingleLibraryReferenceFormat(@PathVariable Integer documentId,
                                                                  @PathVariable
                                                                  LibraryFormat libraryFormat) {
        var content = thesisService.getSingleLibraryReferenceFormat(documentId, libraryFormat);

        var headers = new HttpHeaders();
        FairSignpostingL1Utility.addHeadersForMetadataFormats(headers, documentId);

        return ResponseEntity.ok()
            .headers(headers)
            .body(content);
    }

    @PostMapping("/schedule-public-review-end-check")
    @PreAuthorize("hasAnyAuthority('SCHEDULE_TASK')")
    @Idempotent
    @ResponseStatus(HttpStatus.ACCEPTED)
    public void schedulePublicReviewEndCheck(@RequestParam("timestamp")
                                             LocalDateTime timestamp,
                                             @RequestParam("types")
                                             List<ThesisType> types,
                                             @RequestParam(defaultValue = "ONCE")
                                             RecurrenceType recurrence,
                                             @RequestParam("publicReviewLengthDays")
                                             Integer publicReviewLengthDays,
                                             @RequestHeader("Authorization")
                                             String bearerToken) {
        thesisService.schedulePublicReviewEndCheck(timestamp, types, publicReviewLengthDays,
            tokenUtil.extractUserIdFromToken(bearerToken), recurrence);
    }

    private void performReferenceAdditionChecks(ThesisDTO thesis, String bearerToken) {
        var role = tokenUtil.extractUserRoleFromToken(bearerToken);

        if (role.equals("RESEARCHER") &&
            (Objects.isNull(thesis.getDocumentDate()) || thesis.getDocumentDate().isEmpty())) {
            throw new ThesisException(
                "You have to provide document date when adding thesis as reference.");
        } else if (role.equals("INSTITUTIONAL_LIBRARIAN")) {
            var userInstitutionId = userService.getUserOrganisationUnitId(
                tokenUtil.extractUserIdFromToken(bearerToken));
            var possibleInstitutions =
                organisationUnitService.getOrganisationUnitIdsFromSubHierarchy(userInstitutionId);

            if (!possibleInstitutions.contains(thesis.getOrganisationUnitId())) {
                throw new ThesisException(
                    "Librarian can only add theses to his/her own institution.");
            }
        }
    }
}
