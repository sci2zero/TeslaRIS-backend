package rs.teslaris.core.repository.person;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;
import rs.teslaris.core.model.person.DeclinedDocumentClaim;

@Repository
public interface DeclinedDocumentClaimRepository
    extends JpaRepository<DeclinedDocumentClaim, Integer> {

    @Query("SELECT COUNT(ddc) = 0 FROM DeclinedDocumentClaim ddc " +
        "WHERE ddc.person.id = :personId AND ddc.document.id = :documentId")
    boolean canBeClaimedByPerson(Integer personId, Integer documentId);
}
