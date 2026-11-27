package rs.teslaris.migrator.repository;

import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import java.util.List;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;
import rs.teslaris.core.model.document.DocumentContributionType;

/**
 * Titles and author names of every thesis, for matching a migrated thesis against the existing
 * ones without the search index, which a run without indexing does not fill.
 */
@Repository
public class ThesisLookupRepository {

    @PersistenceContext
    private EntityManager entityManager;


    // [thesis id, title content] for every title language
    @Transactional(readOnly = true)
    public List<Object[]> findTitles() {
        return entityManager.createQuery(
                "SELECT t.id, tt.content FROM Thesis t JOIN t.title tt", Object[].class)
            .getResultList();
    }

    // [thesis id, firstname, lastname] of every author
    @Transactional(readOnly = true)
    public List<Object[]> findAuthorNames() {
        return entityManager.createQuery(
                "SELECT c.document.id, n.firstname, n.lastname " +
                    "FROM PersonDocumentContribution c " +
                    "JOIN c.affiliationStatement a " +
                    "JOIN a.displayPersonName n " +
                    "WHERE c.contributionType = :type " +
                    "AND c.document.id IN (SELECT t.id FROM Thesis t)", Object[].class)
            .setParameter("type", DocumentContributionType.AUTHOR)
            .getResultList();
    }
}
