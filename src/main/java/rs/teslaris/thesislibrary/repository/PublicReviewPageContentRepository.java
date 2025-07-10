package rs.teslaris.thesislibrary.repository;

import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;
import rs.teslaris.core.model.document.ThesisType;
import rs.teslaris.thesislibrary.model.PublicReviewPageContent;

@Repository
public interface PublicReviewPageContentRepository
    extends JpaRepository<PublicReviewPageContent, Integer> {

    @Query("SELECT c FROM PublicReviewPageContent c WHERE c.institution.id = :institutionId")
    List<PublicReviewPageContent> getConfigurationForInstitution(Integer institutionId);

    @Query("SELECT c FROM PublicReviewPageContent c WHERE " +
        "c.institution.id = :institutionId AND " +
        ":thesisType MEMBER OF c.thesisTypes")
    List<PublicReviewPageContent> getConfigurationForInstitutionAndThesisType(Integer institutionId,
                                                                              ThesisType thesisType);

    @Modifying
    @Query("DELETE FROM PublicReviewPageContent c WHERE c.institution.id = :institutionId")
    void deleteAllContentForInstitution(Integer institutionId);
}
