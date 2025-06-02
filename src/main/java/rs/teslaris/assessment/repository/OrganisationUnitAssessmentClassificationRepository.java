package rs.teslaris.assessment.repository;

import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;
import rs.teslaris.assessment.model.OrganisationUnitAssessmentClassification;

@Repository
public interface OrganisationUnitAssessmentClassificationRepository extends
    JpaRepository<OrganisationUnitAssessmentClassification, Integer> {

    @Query("SELECT eac FROM OrganisationUnitAssessmentClassification eac WHERE " +
        "eac.organisationUnit.id = :organisationUnitId ORDER BY eac.timestamp DESC")
    List<OrganisationUnitAssessmentClassification> findAssessmentClassificationsForOrganisationUnit(
        Integer organisationUnitId);
}
