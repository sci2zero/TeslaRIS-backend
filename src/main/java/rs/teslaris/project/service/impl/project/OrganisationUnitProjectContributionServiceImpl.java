package rs.teslaris.project.service.impl.project;

import lombok.RequiredArgsConstructor;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Service;
import rs.teslaris.core.service.impl.JPAServiceImpl;
import rs.teslaris.project.model.project.OrganisationUnitProjectContribution;
import rs.teslaris.project.repository.project.OrganisationUnitProjectContributionRepository;
import rs.teslaris.project.service.interfaces.project.OrganisationUnitProjectContributionService;

@Service
@RequiredArgsConstructor
public class OrganisationUnitProjectContributionServiceImpl
    extends JPAServiceImpl<OrganisationUnitProjectContribution> implements
    OrganisationUnitProjectContributionService {

    private final OrganisationUnitProjectContributionRepository
        organisationUnitProjectContributionRepository;


    @Override
    protected JpaRepository<OrganisationUnitProjectContribution, Integer> getEntityRepository() {
        return organisationUnitProjectContributionRepository;
    }
}
