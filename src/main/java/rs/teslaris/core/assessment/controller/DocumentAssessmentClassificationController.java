package rs.teslaris.core.assessment.controller;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import rs.teslaris.core.annotation.Idempotent;
import rs.teslaris.core.assessment.dto.EntityAssessmentClassificationResponseDTO;
import rs.teslaris.core.assessment.service.interfaces.DocumentAssessmentClassificationService;
import rs.teslaris.core.util.jwt.JwtUtil;

@RestController
@RequestMapping("/api/assessment/document-assessment-classification")
@RequiredArgsConstructor
public class DocumentAssessmentClassificationController {

    private final DocumentAssessmentClassificationService documentAssessmentClassificationService;

    private final JwtUtil tokenUtil;


    @GetMapping("/{documentId}")
    public List<EntityAssessmentClassificationResponseDTO> getAssessmentClassificationsForDocument(
        @PathVariable Integer documentId) {
        return documentAssessmentClassificationService.getAssessmentClassificationsForDocument(
            documentId);
    }

    @PostMapping("/schedule-journal-publication-assessment")
    @Idempotent
    @PreAuthorize("hasAuthority('SCHEDULE_TASK')")
    public void performJournalPublicationAssessmentForThePastYear(@RequestParam("timestamp")
                                                                  LocalDateTime timestamp,
                                                                  @RequestParam("dateFrom")
                                                                  LocalDate dateFrom,
                                                                  @RequestHeader("Authorization")
                                                                  String bearerToken) {
        documentAssessmentClassificationService.scheduleJournalPublicationClassification(timestamp,
            tokenUtil.extractUserIdFromToken(bearerToken), dateFrom, null, new ArrayList<>(),
            new ArrayList<>(), new ArrayList<>());
    }

    @PostMapping("/journal-publication/{journalPublicationId}")
    @Idempotent
    @PreAuthorize("hasAuthority('ASSESS_DOCUMENT')")
    public void assessJournalPublication(@PathVariable Integer journalPublicationId) {
        documentAssessmentClassificationService.classifyJournalPublication(journalPublicationId);
    }
}