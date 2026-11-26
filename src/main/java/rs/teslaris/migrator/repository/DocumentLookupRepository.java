package rs.teslaris.migrator.repository;

import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import jakarta.persistence.TypedQuery;
import java.util.List;
import java.util.Objects;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;
import rs.teslaris.core.model.document.DocumentContributionType;

/**
 * Titles, years and authors of the documents of one kind, for matching a migrated document against
 * the existing ones without the search index, which a run without indexing does not fill.
 * <p>
 * A kind is a document entity, optionally narrowed to one value of its subtype attribute.
 */
@Repository
public class DocumentLookupRepository {

    @PersistenceContext
    private EntityManager entityManager;


    // [document id, title content, year] for every title language
    @Transactional(readOnly = true)
    public List<Object[]> findTitles(String entity, String subtypeAttribute, Enum<?> subtype) {
        return narrowed(entityManager.createQuery(
            "SELECT d.id, tt.content, d.documentDate.year FROM " + entity + " d JOIN d.title tt" +
                where(subtypeAttribute), Object[].class), subtype).getResultList();
    }

    // [document id, firstname, lastname, person id] of every author
    @Transactional(readOnly = true)
    public List<Object[]> findAuthors(String entity, String subtypeAttribute, Enum<?> subtype) {
        return narrowed(entityManager.createQuery(
            "SELECT c.document.id, n.firstname, n.lastname, p.id " +
                "FROM PersonDocumentContribution c " +
                "JOIN c.affiliationStatement a " +
                "JOIN a.displayPersonName n " +
                "LEFT JOIN c.person p " +
                "WHERE c.contributionType = :type " +
                "AND c.document.id IN (SELECT d.id FROM " + entity + " d" +
                where(subtypeAttribute) + ")", Object[].class), subtype)
            .setParameter("type", DocumentContributionType.AUTHOR)
            .getResultList();
    }

    private String where(String subtypeAttribute) {
        return Objects.isNull(subtypeAttribute) ? "" :
            " WHERE d." + subtypeAttribute + " = :subtype";
    }

    private <T> TypedQuery<T> narrowed(TypedQuery<T> query, Enum<?> subtype) {
        return Objects.isNull(subtype) ? query : query.setParameter("subtype", subtype);
    }
}
