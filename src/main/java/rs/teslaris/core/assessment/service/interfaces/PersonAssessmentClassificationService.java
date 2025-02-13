package rs.teslaris.core.assessment.service.interfaces;

import java.time.LocalDate;
import java.util.List;
import org.springframework.stereotype.Service;
import rs.teslaris.core.assessment.dto.EntityAssessmentClassificationResponseDTO;
import rs.teslaris.core.assessment.dto.ResearcherAssessmentResponseDTO;

@Service
public interface PersonAssessmentClassificationService {

    List<EntityAssessmentClassificationResponseDTO> getAssessmentClassificationsForPerson(
        Integer personId);

    void assessResearchers(LocalDate fromDate, Integer commissionId,
                           List<Integer> researcherIds, List<Integer> orgUnitIds,
                           LocalDate startDate, LocalDate endDate);

    List<ResearcherAssessmentResponseDTO> assessSingleResearcher(Integer researcherId,
                                                                 LocalDate startDate,
                                                                 LocalDate endDate);
}
