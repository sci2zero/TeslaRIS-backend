package rs.teslaris.core.assessment.controller;

import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import rs.teslaris.core.assessment.dto.EntityAssessmentClassificationResponseDTO;
import rs.teslaris.core.assessment.service.interfaces.DocumentAssessmentClassificationService;

@RestController
@RequestMapping("/api/assessment/document-assessment-classification")
@RequiredArgsConstructor
public class DocumentAssessmentClassificationController {

    private final DocumentAssessmentClassificationService documentAssessmentClassificationService;

    @GetMapping("/{documentId}")
    public List<EntityAssessmentClassificationResponseDTO> getAssessmentClassificationsForDocument(
        @PathVariable Integer documentId) {
        return documentAssessmentClassificationService.getAssessmentClassificationsForDocument(
            documentId);
    }
}