package rs.teslaris.project.service.impl.identifier.cruddelegate;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Component;
import rs.teslaris.core.service.impl.JPAServiceImpl;
import rs.teslaris.project.model.identifier.FundingIdentifier;
import rs.teslaris.project.repository.identifier.FundingIdentifierRepository;

@Component
public class FundingIdentifierJPAServiceImpl extends JPAServiceImpl<FundingIdentifier> {

    private final FundingIdentifierRepository fundingIdentifierRepository;


    @Autowired
    public FundingIdentifierJPAServiceImpl(
        FundingIdentifierRepository fundingIdentifierRepository) {
        this.fundingIdentifierRepository = fundingIdentifierRepository;
    }

    @Override
    protected JpaRepository<FundingIdentifier, Integer> getEntityRepository() {
        return fundingIdentifierRepository;
    }
}
