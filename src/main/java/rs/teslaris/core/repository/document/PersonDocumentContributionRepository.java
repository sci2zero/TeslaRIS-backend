package rs.teslaris.core.repository.document;

import jakarta.persistence.LockModeType;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;
import rs.teslaris.core.model.document.PersonDocumentContribution;

@Repository
public interface PersonDocumentContributionRepository
    extends JpaRepository<PersonDocumentContribution, Integer> {

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("SELECT pdc FROM PersonDocumentContribution pdc " +
        "WHERE pdc.document.id = :documentId AND pdc.person IS null")
    List<PersonDocumentContribution> findUnmanagedContributionsForDocument(Integer documentId);
}
