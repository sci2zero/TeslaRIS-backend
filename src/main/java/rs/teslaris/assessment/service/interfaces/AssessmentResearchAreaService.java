package rs.teslaris.assessment.service.interfaces;

import java.util.List;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import rs.teslaris.assessment.dto.AssessmentResearchAreaDTO;
import rs.teslaris.assessment.model.AssessmentResearchArea;
import rs.teslaris.core.indexmodel.PersonIndex;
import rs.teslaris.core.service.interfaces.JPAService;

@Service
public interface AssessmentResearchAreaService extends JPAService<AssessmentResearchArea> {

    List<AssessmentResearchAreaDTO> readAllAssessmentResearchAreas();

    AssessmentResearchAreaDTO readPersonAssessmentResearchArea(Integer personId);

    void setPersonAssessmentResearchAreaForCommission(Integer personId, String researchAreaCode,
                                                      Integer commissionId);

    Page<PersonIndex> readPersonAssessmentResearchAreaForCommission(Integer commissionId,
                                                                    String code,
                                                                    Pageable pageable);

    void setPersonAssessmentResearchArea(Integer personId, String researchAreaCode);

    void deletePersonAssessmentResearchArea(Integer personId);

    void removePersonAssessmentResearchAreaForCommission(Integer personId, Integer commissionId);
}
