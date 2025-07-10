package rs.teslaris.thesislibrary.service.interfaces;

import java.util.List;
import org.springframework.stereotype.Service;
import rs.teslaris.core.model.document.ThesisType;
import rs.teslaris.core.service.interfaces.JPAService;
import rs.teslaris.thesislibrary.dto.PublicReviewPageContentDTO;
import rs.teslaris.thesislibrary.model.PublicReviewPageContent;

@Service
public interface PublicReviewPageContentService extends JPAService<PublicReviewPageContent> {

    List<PublicReviewPageContentDTO> readPageContentConfigurationForInstitution(
        Integer institutionId);

    List<PublicReviewPageContentDTO> readPageContentConfigurationForInstitutionAndType(
        Integer institutionId, ThesisType thesisType);

    void savePageConfiguration(List<PublicReviewPageContentDTO> contentConfiguration,
                               Integer institutionId);
}
