package rs.teslaris.assessment.service.impl.cruddelegate;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Component;
import rs.teslaris.assessment.model.indicator.OrganisationUnitIndicator;
import rs.teslaris.assessment.repository.indicator.OrganisationUnitIndicatorRepository;
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
