package rs.teslaris.assessment.service.interfaces.classification;

import java.time.LocalDate;
import java.util.List;
import org.springframework.stereotype.Service;
import rs.teslaris.assessment.dto.EnrichedResearcherAssessmentResponseDTO;
import rs.teslaris.assessment.dto.ResearcherAssessmentResponseDTO;
import rs.teslaris.assessment.dto.classification.EntityAssessmentClassificationResponseDTO;
import rs.teslaris.assessment.model.AssessmentMeasure;
import rs.teslaris.core.indexmodel.PersonIndex;

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

    void reindexPublicationPointsForResearcher(PersonIndex index,
                                               List<AssessmentMeasure> assessmentMeasures);

    void reindexPublicationPointsForAllResearchers(List<Integer> personIds,
                                                   List<Integer> institutionIds);
}
