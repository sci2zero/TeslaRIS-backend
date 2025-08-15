package rs.teslaris.core.repository.institution;

import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;
import rs.teslaris.core.model.institution.InstitutionDefaultSubmissionContent;

@Repository
public interface InstitutionDefaultSubmissionContentRepository
    extends JpaRepository<InstitutionDefaultSubmissionContent, Integer> {

    @Query("SELECT c FROM InstitutionDefaultSubmissionContent c WHERE " +
        "c.institution.id = :institutionId")
    Optional<InstitutionDefaultSubmissionContent> getDefaultContentForInstitution(
        Integer institutionId);
}
