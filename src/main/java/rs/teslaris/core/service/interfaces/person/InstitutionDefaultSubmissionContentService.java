package rs.teslaris.core.service.interfaces.person;

import org.springframework.stereotype.Service;
import rs.teslaris.core.dto.institution.InstitutionDefaultSubmissionContentDTO;
import rs.teslaris.core.model.institution.InstitutionDefaultSubmissionContent;
import rs.teslaris.core.service.interfaces.JPAService;

@Service
public interface InstitutionDefaultSubmissionContentService
    extends JPAService<InstitutionDefaultSubmissionContent> {

    InstitutionDefaultSubmissionContentDTO readInstitutionDefaultContentForUser(Integer userId);

    InstitutionDefaultSubmissionContentDTO readInstitutionDefaultContent(Integer institutionId);

    void saveConfiguration(Integer institutionId, InstitutionDefaultSubmissionContentDTO content);
}
