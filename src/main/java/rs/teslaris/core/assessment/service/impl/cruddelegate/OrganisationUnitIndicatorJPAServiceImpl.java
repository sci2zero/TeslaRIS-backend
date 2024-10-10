package rs.teslaris.core.assessment.service.impl.cruddelegate;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Component;
import rs.teslaris.core.assessment.model.OrganisationUnitIndicator;
import rs.teslaris.core.assessment.repository.OrganisationUnitIndicatorRepository;
import rs.teslaris.core.service.impl.JPAServiceImpl;

@Component
public class OrganisationUnitIndicatorJPAServiceImpl
    extends JPAServiceImpl<OrganisationUnitIndicator> {

    private final OrganisationUnitIndicatorRepository organisationUnitIndicatorRepository;

    @Autowired
    public OrganisationUnitIndicatorJPAServiceImpl(
        OrganisationUnitIndicatorRepository organisationUnitIndicatorRepository) {
        this.organisationUnitIndicatorRepository = organisationUnitIndicatorRepository;
    }

    @Override
    protected JpaRepository<OrganisationUnitIndicator, Integer> getEntityRepository() {
        return organisationUnitIndicatorRepository;
    }
}