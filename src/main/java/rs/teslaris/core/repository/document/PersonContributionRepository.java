package rs.teslaris.core.repository.document;

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

    @Query("select pdc from PersonDocumentContribution pdc join pdc.person p where p.id = :personId")
    Page<PersonDocumentContribution> fetchAllPersonDocumentContributions(Integer personId,
                                                                         Pageable pageable);

    @Modifying
    @Query("update PersonEventContribution pec set pec.deleted = true where pec.person.id = :personId")
    void deletePersonEventContributions(Integer personId);

    @Modifying
    @Query("update PersonEventContribution pec set pec.person = null where pec.person.id = :personId")
    void makePersonEventContributionsPointToExternalContributor(Integer personId);

    @Modifying
    @Query("update PersonPublicationSeriesContribution pjc set pjc.deleted = true where pjc.person.id = :personId")
    void deletePersonPublicationsSeriesContributions(Integer personId);

    @Modifying
    @Query("update PersonPublicationSeriesContribution pjc set pjc.person = null where pjc.person.id = :personId")
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
