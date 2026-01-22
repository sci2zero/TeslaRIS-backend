package rs.teslaris.assessment.service.interfaces.classification;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import org.springframework.stereotype.Service;
import rs.teslaris.assessment.dto.ImaginaryPublicationAssessmentResponseDTO;
import rs.teslaris.assessment.dto.classification.DocumentAssessmentClassificationDTO;
import rs.teslaris.assessment.dto.classification.EntityAssessmentClassificationResponseDTO;
import rs.teslaris.core.indexmodel.DocumentPublicationType;
import rs.teslaris.core.model.document.PublicationType;

@Service
public interface DocumentAssessmentClassificationService {

    List<EntityAssessmentClassificationResponseDTO> getAssessmentClassificationsForDocument(
        Integer documentId);

    EntityAssessmentClassificationResponseDTO createDocumentAssessmentClassification(
        DocumentAssessmentClassificationDTO documentAssessmentClassificationDTO);

    void editDocumentAssessmentClassification(Integer classificationId,
                                              DocumentAssessmentClassificationDTO documentAssessmentClassificationDTO);

    void classifyJournalPublication(Integer journalPublicationId);

    void classifyProceedingsPublication(Integer proceedingsPublicationId);

    void schedulePublicationClassification(LocalDateTime timeToRun,
                                           Integer userId, LocalDate fromDate,
                                           DocumentPublicationType documentPublicationType,
                                           Integer commissionId,
                                           List<Integer> authorIds,
                                           List<Integer> orgUnitIds,
                                           List<Integer> journalIds);

    void classifyJournalPublications(LocalDate fromDate, Integer commissionId,
                                     List<Integer> authorIds, List<Integer> orgUnitIds,
                                     List<Integer> journalIds);

    void classifyProceedingsPublications(LocalDate fromDate, Integer commissionId,
                                         List<Integer> authorIds, List<Integer> orgUnitIds,
                                         List<Integer> eventIds);

    void classifyTheses(LocalDate fromDate, Integer commissionId,
                        List<Integer> authorIds, List<Integer> orgUnitIds,
                        List<Integer> eventIds);

    void classifyMonographPublications(LocalDate fromDate, Integer commissionId,
                                       List<Integer> authorIds, List<Integer> orgUnitIds,
                                       List<Integer> monographIds);

    ImaginaryPublicationAssessmentResponseDTO assessImaginaryJournalPublication(
        Integer journalId, Integer commissionId, Integer classificationYear, String researchArea,
        Integer authorCount, boolean isExperimental, boolean isTheoretical, boolean isSimulation,
        PublicationType publicationType);

    ImaginaryPublicationAssessmentResponseDTO assessImaginaryProceedingsPublication(
        Integer conferenceId, Integer commissionId, String researchArea, Integer authorCount,
        boolean isExperimental, boolean isTheoretical, boolean isSimulation,
        PublicationType publicationType);
}
