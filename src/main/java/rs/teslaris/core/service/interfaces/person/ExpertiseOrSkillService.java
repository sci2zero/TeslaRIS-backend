package rs.teslaris.core.service.interfaces.person;

import org.springframework.stereotype.Service;
import rs.teslaris.core.dto.document.DocumentFileDTO;
import rs.teslaris.core.dto.document.DocumentFileResponseDTO;
import rs.teslaris.core.dto.person.ExpertiseOrSkillDTO;
import rs.teslaris.core.dto.person.ExpertiseOrSkillResponseDTO;
import rs.teslaris.core.model.person.ExpertiseOrSkill;
import rs.teslaris.core.service.interfaces.JPAService;

@Service
public interface ExpertiseOrSkillService extends JPAService<ExpertiseOrSkill> {

    ExpertiseOrSkillResponseDTO addExpertiseOrSkill(Integer personId, ExpertiseOrSkillDTO dto);

    ExpertiseOrSkillResponseDTO updateExpertiseOrSkill(Integer expertiseOrSkillId,
                                                       ExpertiseOrSkillDTO dto);

    void deleteExpertiseOrSkill(Integer expertiseOrSkillId, Integer personId);

    DocumentFileResponseDTO addProof(Integer expertiseOrSkillId, DocumentFileDTO proof);

    DocumentFileResponseDTO updateProof(DocumentFileDTO updatedProof);

    void deleteProof(Integer proofId, Integer expertiseOrSkillId);
}
