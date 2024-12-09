package rs.teslaris.core.assessment.repository;

import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;
import rs.teslaris.core.assessment.model.OrganisationUnitAssessmentClassification;

@Repository
public interface OrganisationUnitAssessmentClassificationRepository extends
    JpaRepository<OrganisationUnitAssessmentClassification, Integer> {

    @Query("select eac from OrganisationUnitAssessmentClassification eac where " +
        "eac.organisationUnit.id = :organisationUnitId order by eac.timestamp desc")
    List<OrganisationUnitAssessmentClassification> findAssessmentClassificationsForOrganisationUnit(
        Integer organisationUnitId);
}
