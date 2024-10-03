package rs.teslaris.core.assessment.repository;

import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;
import rs.teslaris.core.assessment.model.IndicatorAccessLevel;
import rs.teslaris.core.assessment.model.OrganisationUnitIndicator;

@Repository
public interface OrganisationUnitIndicatorRepository
    extends JpaRepository<OrganisationUnitIndicator, Integer> {

    @Query("select oui from OrganisationUnitIndicator oui " +
        "where oui.organisationUnit.id = :organisationUnitId and " +
        "oui.indicator.accessLevel <= :accessLevel")
    List<OrganisationUnitIndicator> findIndicatorsForOrganisationUnitAndIndicatorAccessLevel(
        Integer organisationUnitId,
        IndicatorAccessLevel accessLevel);
}
