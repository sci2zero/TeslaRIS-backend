package rs.teslaris.project.service.interfaces.project;

import org.springframework.stereotype.Service;
import rs.teslaris.core.service.interfaces.JPAService;
import rs.teslaris.project.dto.project.OrganisationUnitProjectContributionDTO;
import rs.teslaris.project.model.project.OrganisationUnitProjectContribution;
import rs.teslaris.project.model.project.Project;

@Service
public interface OrganisationUnitProjectContributionService
    extends JPAService<OrganisationUnitProjectContribution> {

    OrganisationUnitProjectContribution createContribution(OrganisationUnitProjectContributionDTO dto, Project project);

}
