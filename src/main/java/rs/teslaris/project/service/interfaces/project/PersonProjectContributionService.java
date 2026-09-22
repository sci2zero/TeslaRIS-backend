package rs.teslaris.project.service.interfaces.project;

import org.springframework.stereotype.Service;
import rs.teslaris.core.service.interfaces.JPAService;
import rs.teslaris.project.dto.project.PersonProjectContributionDTO;
import rs.teslaris.project.model.project.PersonProjectContribution;
import rs.teslaris.project.model.project.Project;

@Service
public interface PersonProjectContributionService extends JPAService<PersonProjectContribution> {
    PersonProjectContribution createContribution(PersonProjectContributionDTO dto,
                                                 Project parent);
}
