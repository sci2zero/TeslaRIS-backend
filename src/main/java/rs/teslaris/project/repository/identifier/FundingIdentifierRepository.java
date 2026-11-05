package rs.teslaris.project.repository.identifier;

import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;
import rs.teslaris.core.model.commontypes.AccessLevel;
import rs.teslaris.project.model.identifier.FundingIdentifier;

@Repository
public interface FundingIdentifierRepository extends JpaRepository<FundingIdentifier, Integer> {

    @Query("SELECT fi FROM FundingIdentifier fi JOIN FETCH fi.identifier i " +
        "WHERE fi.funding.id = :fundingId AND i.accessLevel <= :accessLevel " +
        "ORDER BY i.code, fi.value")
    List<FundingIdentifier> findIdentifiersForFundingAndIdentifierAccessLevel(Integer fundingId,
                                                                              AccessLevel accessLevel);
}
