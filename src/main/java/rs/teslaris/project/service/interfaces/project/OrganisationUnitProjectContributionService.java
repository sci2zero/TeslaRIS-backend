package rs.teslaris.project.service.interfaces.project;

import org.springframework.stereotype.Service;
import rs.teslaris.core.service.interfaces.JPAService;
import rs.teslaris.project.model.project.OrganisationUnitProjectContribution;

import java.util.List;

@Service
public interface OrganisationUnitProjectContributionService
    extends JPAService<OrganisationUnitProjectContribution> {

    List<OrganisationUnitProjectContribution> getOrganisationUnitsByIds(List<Integer> id);
}
