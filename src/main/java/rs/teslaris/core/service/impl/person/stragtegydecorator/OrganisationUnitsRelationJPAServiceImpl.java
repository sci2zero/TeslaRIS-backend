package rs.teslaris.core.service.impl.person.stragtegydecorator;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Component;
import rs.teslaris.core.model.institution.OrganisationUnitsRelation;
import rs.teslaris.core.repository.person.OrganisationUnitsRelationRepository;
import rs.teslaris.core.service.impl.JPAServiceImpl;

@Component
public class OrganisationUnitsRelationJPAServiceImpl
    extends JPAServiceImpl<OrganisationUnitsRelation> {

    private final OrganisationUnitsRelationRepository organisationUnitsRelationRepository;

    @Autowired
    public OrganisationUnitsRelationJPAServiceImpl(
        OrganisationUnitsRelationRepository organisationUnitsRelationRepository) {
        this.organisationUnitsRelationRepository = organisationUnitsRelationRepository;
    }

    @Override
    protected JpaRepository<OrganisationUnitsRelation, Integer> getEntityRepository() {
        return organisationUnitsRelationRepository;
    }
}
