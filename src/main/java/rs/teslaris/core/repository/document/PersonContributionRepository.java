package rs.teslaris.core.repository.document;

import java.util.List;
import java.util.Optional;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;
import rs.teslaris.core.model.document.PersonContribution;
import rs.teslaris.core.model.document.PersonDocumentContribution;

@Repository
public interface PersonContributionRepository extends JpaRepository<PersonContribution, Integer> {

    @Query("SELECT pdc FROM PersonDocumentContribution pdc " +
        "JOIN pdc.person p " +
        "WHERE p.id = :personId")
    Page<PersonDocumentContribution> fetchAllPersonDocumentContributions(Integer personId,
                                                                         Pageable pageable);

    @Query("SELECT pdc.document.id FROM PersonDocumentContribution pdc " +
        "JOIN pdc.person p " +
        "WHERE p.id = :personId AND " +
        "NOT EXISTS (SELECT 1 FROM pdc.institutions inst WHERE inst.id = :institutionId)")
    List<Integer> fetchAllDocumentsWhereInstitutionIsNotListed(Integer personId,
                                                               Integer institutionId);

    @Query("SELECT pdc FROM PersonDocumentContribution pdc " +
        "WHERE pdc.person.id = :personId AND " +
        "pdc.document.id = :documentId")
    Optional<PersonDocumentContribution> fetchPersonDocumentContributionOnDocument(Integer personId,
                                                                                   Integer documentId);

    @Modifying
    @Query("UPDATE PersonEventContribution pec " +
        "SET pec.deleted = true WHERE pec.person.id = :personId")
    void deletePersonEventContributions(Integer personId);

    @Modifying
    @Query("UPDATE PersonEventContribution pec " +
        "SET pec.person = null WHERE pec.person.id = :personId")
    void makePersonEventContributionsPointToExternalContributor(Integer personId);

    @Modifying
    @Query("UPDATE PersonPublicationSeriesContribution pjc " +
        "SET pjc.deleted = true WHERE pjc.person.id = :personId")
    void deletePersonPublicationsSeriesContributions(Integer personId);

    @Modifying
    @Query("UPDATE PersonPublicationSeriesContribution pjc " +
        "SET pjc.person = null WHERE pjc.person.id = :personId")
    void makePersonPublicationsSeriesContributionsPointToExternalContributor(Integer personId);

    @Modifying
    @Query(value =
        "DELETE FROM person_contributions_institutions " +
            "WHERE person_contribution_id IN (" +
            "    SELECT id FROM person_contributions WHERE person_id = :personId " +
            ");",
        nativeQuery = true)
    void deleteInstitutionsForForPersonContributions(Integer personId);
}
