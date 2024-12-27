package rs.teslaris.core.repository.person;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;
import rs.teslaris.core.model.person.DeclinedDocumentClaim;

@Repository
public interface DeclinedDocumentClaimRepository
    extends JpaRepository<DeclinedDocumentClaim, Integer> {

    @Query("select count(ddc) = 0 from DeclinedDocumentClaim ddc where ddc.person.id = :personId and ddc.document.id = :documentId")
    boolean canBeClaimedByPerson(Integer personId, Integer documentId);
}
