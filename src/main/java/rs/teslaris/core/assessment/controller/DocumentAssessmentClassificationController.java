package rs.teslaris.core.assessment.controller;

import jakarta.validation.Valid;
import jakarta.validation.ValidationException;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Objects;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;
import rs.teslaris.core.annotation.ApiKeyValidation;
import rs.teslaris.core.annotation.Idempotent;
import rs.teslaris.core.annotation.PublicationEditCheck;
import rs.teslaris.core.assessment.dto.DocumentAssessmentClassificationDTO;
import rs.teslaris.core.assessment.dto.EntityAssessmentClassificationResponseDTO;
import rs.teslaris.core.assessment.dto.ImaginaryPublicationAssessmentRequestDTO;
import rs.teslaris.core.assessment.dto.ImaginaryPublicationAssessmentResponseDTO;
import rs.teslaris.core.assessment.dto.PublicationAssessmentRequestDTO;
import rs.teslaris.core.assessment.service.interfaces.DocumentAssessmentClassificationService;
import rs.teslaris.core.indexmodel.DocumentPublicationType;
import rs.teslaris.core.model.commontypes.ApiKeyType;
import rs.teslaris.core.model.user.UserRole;
import rs.teslaris.core.service.interfaces.commontypes.ReCaptchaService;
import rs.teslaris.core.service.interfaces.document.ConferenceService;
import rs.teslaris.core.service.interfaces.document.JournalService;
import rs.teslaris.core.service.interfaces.user.UserService;
import rs.teslaris.core.util.exceptionhandling.exception.CaptchaException;
import rs.teslaris.core.util.jwt.JwtUtil;

@RestController
@RequestMapping("/api/assessment/document-assessment-classification")
@RequiredArgsConstructor
public class DocumentAssessmentClassificationController {

    private final DocumentAssessmentClassificationService documentAssessmentClassificationService;

    private final JwtUtil tokenUtil;

    private final UserService userService;

    private final ConferenceService conferenceService;

    private final JournalService journalService;

    private final ReCaptchaService reCaptchaService;


    @GetMapping("/{documentId}/can-classify")
    @PreAuthorize("hasAnyAuthority('ASSESS_DOCUMENT', 'EDIT_DOCUMENT_ASSESSMENT')")
    public boolean canClassifyDocument() {
        return true;
    }

    @GetMapping("/{documentId}")
    public List<EntityAssessmentClassificationResponseDTO> getAssessmentClassificationsForDocument(
        @PathVariable Integer documentId) {
        return documentAssessmentClassificationService.getAssessmentClassificationsForDocument(
            documentId);
    }

    @PostMapping("/schedule-publication-assessment/{documentType}")
    @Idempotent
    @PreAuthorize("hasAuthority('SCHEDULE_TASK')")
    public void performPublicationAssessmentForThePastYear(@RequestParam("timestamp")
                                                           LocalDateTime timestamp,
                                                           @RequestParam("dateFrom")
                                                           LocalDate dateFrom,
                                                           @RequestBody
                                                           PublicationAssessmentRequestDTO publicationAssessmentRequestDTO,
                                                           @RequestHeader("Authorization")
                                                           String bearerToken,
                                                           @PathVariable
                                                           DocumentPublicationType documentType) {
        documentAssessmentClassificationService.schedulePublicationClassification(timestamp,
            tokenUtil.extractUserIdFromToken(bearerToken), dateFrom, documentType,
            publicationAssessmentRequestDTO.getCommissionId(),
            publicationAssessmentRequestDTO.getAuthorIds(),
            publicationAssessmentRequestDTO.getOrganisationUnitIds(),
            publicationAssessmentRequestDTO.getPublishedInIds());
    }

    @PostMapping("/imaginary-journal-publication")
    public ImaginaryPublicationAssessmentResponseDTO assessImaginaryJournalPublication(
        @RequestBody @Valid
        ImaginaryPublicationAssessmentRequestDTO imaginaryJournalPublicationAssessmentRequest,
        @RequestParam String token) {
        if (reCaptchaService.isCaptchaValid(token)) {
            throw new CaptchaException("Invalid captcha solution.");
        }

        return documentAssessmentClassificationService.assessImaginaryJournalPublication(
            imaginaryJournalPublicationAssessmentRequest.getContainingEntityId(),
            imaginaryJournalPublicationAssessmentRequest.getCommissionId(),
            imaginaryJournalPublicationAssessmentRequest.getClassificationYear(),
            imaginaryJournalPublicationAssessmentRequest.getResearchAreaCode(),
            imaginaryJournalPublicationAssessmentRequest.getAuthorCount(),
            imaginaryJournalPublicationAssessmentRequest.getExperimental(),
            imaginaryJournalPublicationAssessmentRequest.getTheoretical(),
            imaginaryJournalPublicationAssessmentRequest.getSimulation(),
            imaginaryJournalPublicationAssessmentRequest.getJournalPublicationType());
    }

    @PostMapping("/imaginary-proceedings-publication")
    public ImaginaryPublicationAssessmentResponseDTO assessImaginaryProceedingsPublication(
        @RequestBody @Valid
        ImaginaryPublicationAssessmentRequestDTO imaginaryProceedingsPublicationAssessmentRequest,
        @RequestParam String token) {
        if (reCaptchaService.isCaptchaValid(token)) {
            throw new CaptchaException("Invalid captcha solution.");
        }

        return documentAssessmentClassificationService.assessImaginaryProceedingsPublication(
            imaginaryProceedingsPublicationAssessmentRequest.getContainingEntityId(),
            imaginaryProceedingsPublicationAssessmentRequest.getCommissionId(),
            imaginaryProceedingsPublicationAssessmentRequest.getResearchAreaCode(),
            imaginaryProceedingsPublicationAssessmentRequest.getAuthorCount(),
            imaginaryProceedingsPublicationAssessmentRequest.getExperimental(),
            imaginaryProceedingsPublicationAssessmentRequest.getTheoretical(),
            imaginaryProceedingsPublicationAssessmentRequest.getSimulation(),
            imaginaryProceedingsPublicationAssessmentRequest.getProceedingsPublicationType());
    }

    @PostMapping("/{documentId}")
    @Idempotent
    @PreAuthorize("hasAuthority('EDIT_DOCUMENT_ASSESSMENT')")
    @ResponseStatus(HttpStatus.CREATED)
    public EntityAssessmentClassificationResponseDTO createDocumentClassification(
        @RequestHeader("Authorization") String bearerToken,
        @RequestBody
        DocumentAssessmentClassificationDTO documentAssessmentClassificationDTO) {
        if (tokenUtil.extractUserRoleFromToken(bearerToken).equals(UserRole.COMMISSION.name())) {
            var user = userService.findOne(tokenUtil.extractUserIdFromToken(bearerToken));
            documentAssessmentClassificationDTO.setCommissionId(user.getCommission().getId());
        }

        return documentAssessmentClassificationService.createDocumentAssessmentClassification(
            documentAssessmentClassificationDTO);
    }

    @PutMapping("/{documentId}/{documentClassificationId}")
    @PreAuthorize("hasAuthority('EDIT_DOCUMENT_ASSESSMENT')")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void updateDocumentClassification(@PathVariable Integer documentClassificationId,
                                             @RequestBody
                                             DocumentAssessmentClassificationDTO documentAssessmentClassificationDTO) {
        documentAssessmentClassificationService.editDocumentAssessmentClassification(
            documentClassificationId, documentAssessmentClassificationDTO);
    }

    @PostMapping("/journal-publication/{documentId}")
    @Idempotent
    @PublicationEditCheck
    @PreAuthorize("hasAuthority('ASSESS_DOCUMENT')")
    public void assessJournalPublication(@PathVariable Integer documentId) {
        documentAssessmentClassificationService.classifyJournalPublication(documentId);
    }

    @PostMapping("/proceedings-publication/{documentId}")
    @Idempotent
    @PublicationEditCheck
    @PreAuthorize("hasAuthority('ASSESS_DOCUMENT')")
    public void assessProceedingsPublication(@PathVariable Integer documentId) {
        documentAssessmentClassificationService.classifyProceedingsPublication(documentId);
    }

    @PostMapping("/journal-m-service")
    @ApiKeyValidation(ApiKeyType.M_SERVICE)
    public ImaginaryPublicationAssessmentResponseDTO journalMService(
        @RequestBody @Valid
        ImaginaryPublicationAssessmentRequestDTO imaginaryJournalPublicationAssessmentRequest) {
        if (Objects.isNull(imaginaryJournalPublicationAssessmentRequest.getIssn())) {
            throw new ValidationException("You have to provide journal ISSN");
        }

        var journalId = journalService.readJournalByIssn(
            imaginaryJournalPublicationAssessmentRequest.getIssn(),
            imaginaryJournalPublicationAssessmentRequest.getIssn()).getDatabaseId();

        return documentAssessmentClassificationService.assessImaginaryJournalPublication(
            journalId,
            imaginaryJournalPublicationAssessmentRequest.getCommissionId(),
            imaginaryJournalPublicationAssessmentRequest.getClassificationYear(),
            imaginaryJournalPublicationAssessmentRequest.getResearchAreaCode(),
            imaginaryJournalPublicationAssessmentRequest.getAuthorCount(),
            imaginaryJournalPublicationAssessmentRequest.getExperimental(),
            imaginaryJournalPublicationAssessmentRequest.getTheoretical(),
            imaginaryJournalPublicationAssessmentRequest.getSimulation(),
            imaginaryJournalPublicationAssessmentRequest.getJournalPublicationType());
    }

    @PostMapping("/conference-m-service")
    @ApiKeyValidation(ApiKeyType.M_SERVICE)
    public ImaginaryPublicationAssessmentResponseDTO conferenceMService(
        @RequestBody @Valid
        ImaginaryPublicationAssessmentRequestDTO imaginaryProceedingsPublicationAssessmentRequest) {
        if (Objects.isNull(imaginaryProceedingsPublicationAssessmentRequest.getConfId())) {
            throw new ValidationException("You have to provide conf ID");
        }

        var conferenceId = conferenceService.findConferenceByConfId(
            imaginaryProceedingsPublicationAssessmentRequest.getConfId()).getId();

        return documentAssessmentClassificationService.assessImaginaryProceedingsPublication(
            conferenceId,
            imaginaryProceedingsPublicationAssessmentRequest.getCommissionId(),
            imaginaryProceedingsPublicationAssessmentRequest.getResearchAreaCode(),
            imaginaryProceedingsPublicationAssessmentRequest.getAuthorCount(),
            imaginaryProceedingsPublicationAssessmentRequest.getExperimental(),
            imaginaryProceedingsPublicationAssessmentRequest.getTheoretical(),
            imaginaryProceedingsPublicationAssessmentRequest.getSimulation(),
            imaginaryProceedingsPublicationAssessmentRequest.getProceedingsPublicationType());
    }
}
