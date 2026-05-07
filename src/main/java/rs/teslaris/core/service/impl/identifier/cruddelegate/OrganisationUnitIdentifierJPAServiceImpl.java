package rs.teslaris.core.service.impl.identifier.cruddelegate;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Component;
import rs.teslaris.core.model.identifier.OrganisationUnitIdentifier;
import rs.teslaris.core.repository.identifier.OrganisationUnitIdentifierRepository;
import rs.teslaris.core.service.impl.JPAServiceImpl;

@Component
public class OrganisationUnitIdentifierJPAServiceImpl
    extends JPAServiceImpl<OrganisationUnitIdentifier> {

    private final OrganisationUnitIdentifierRepository organisationUnitIdentifierRepository;


    @Autowired
    public OrganisationUnitIdentifierJPAServiceImpl(
        OrganisationUnitIdentifierRepository organisationUnitIdentifierRepository) {
        this.organisationUnitIdentifierRepository = organisationUnitIdentifierRepository;
    }

    @Override
    protected JpaRepository<OrganisationUnitIdentifier, Integer> getEntityRepository() {
        return organisationUnitIdentifierRepository;
    }
}
