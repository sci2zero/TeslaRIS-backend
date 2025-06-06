package rs.teslaris.assessment.service.interfaces;

import java.time.LocalDate;
import java.util.List;
import org.springframework.stereotype.Service;
import rs.teslaris.assessment.dto.EnrichedResearcherAssessmentResponseDTO;
import rs.teslaris.assessment.dto.EntityAssessmentClassificationResponseDTO;
import rs.teslaris.assessment.dto.ResearcherAssessmentResponseDTO;

@Service
public interface PersonAssessmentClassificationService {

    List<EntityAssessmentClassificationResponseDTO> getAssessmentClassificationsForPerson(
        Integer personId);

    List<EnrichedResearcherAssessmentResponseDTO> assessResearchers(Integer commissionId,
                                                                    List<Integer> researcherIds,
                                                                    Integer startYear,
                                                                    Integer endYear,
                                                                    Integer topLevelInstitutionId);

    List<ResearcherAssessmentResponseDTO> assessSingleResearcher(Integer researcherId,
                                                                 LocalDate startDate,
                                                                 LocalDate endDate);
}
