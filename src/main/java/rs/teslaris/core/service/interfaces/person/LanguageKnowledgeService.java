package rs.teslaris.core.service.interfaces.person;

import org.springframework.stereotype.Service;
import rs.teslaris.core.dto.document.DocumentFileDTO;
import rs.teslaris.core.dto.document.DocumentFileResponseDTO;
import rs.teslaris.core.dto.person.LanguageKnowledgeRequestDTO;
import rs.teslaris.core.dto.person.LanguageKnowledgeResponseDTO;
import rs.teslaris.core.model.person.LanguageKnowledge;
import rs.teslaris.core.service.interfaces.JPAService;

@Service
public interface LanguageKnowledgeService extends JPAService<LanguageKnowledge> {

    LanguageKnowledgeResponseDTO addLanguageKnowledge(Integer personId,
                                                      LanguageKnowledgeRequestDTO dto);

    LanguageKnowledgeResponseDTO updateLanguageKnowledge(Integer languageKnowledgeId,
                                                         LanguageKnowledgeRequestDTO dto);

    void deleteLanguageKnowledge(Integer languageKnowledgeId, Integer personId);

    DocumentFileResponseDTO addProof(Integer languageKnowledgeId, DocumentFileDTO proof);

    DocumentFileResponseDTO updateProof(DocumentFileDTO updatedProof);

    void deleteProof(Integer proofId, Integer languageKnowledgeId);
}
