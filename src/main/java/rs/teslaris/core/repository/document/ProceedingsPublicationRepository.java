package rs.teslaris.core.repository.document;

import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;
import rs.teslaris.core.model.document.ProceedingsPublication;

@Repository
public interface ProceedingsPublicationRepository
    extends JpaRepository<ProceedingsPublication, Integer> {

    @Query("SELECT DISTINCT pp FROM ProceedingsPublication pp " +
        "JOIN pp.event e " +
        "WHERE e.id = :eventId AND :authorId in (select c.person.id from pp.contributors c where c.contributionType = 0)")
    List<ProceedingsPublication> findProceedingsPublicationsForEventId(Integer eventId,
                                                                       Integer authorId);
}
