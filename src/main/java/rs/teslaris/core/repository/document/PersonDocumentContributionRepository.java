package rs.teslaris.core.repository.document;

import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;
import rs.teslaris.core.model.document.PersonDocumentContribution;

@Repository
public interface PersonDocumentContributionRepository
    extends JpaRepository<PersonDocumentContribution, Integer> {

    @Query("select pdc from PersonDocumentContribution pdc where pdc.document.id = :documentId and pdc.person is null")
    List<PersonDocumentContribution> findUnmanagedContributionsForDocument(Integer documentId);
}
