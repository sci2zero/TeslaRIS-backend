package rs.teslaris.core.assessment.repository;

import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;
import rs.teslaris.core.assessment.model.OrganisationUnitIndicator;
import rs.teslaris.core.model.commontypes.AccessLevel;

@Repository
public interface OrganisationUnitIndicatorRepository
    extends JpaRepository<OrganisationUnitIndicator, Integer> {

    @Query("select oui from OrganisationUnitIndicator oui " +
        "where oui.organisationUnit.id = :organisationUnitId and " +
        "oui.indicator.accessLevel <= :accessLevel")
    List<OrganisationUnitIndicator> findIndicatorsForOrganisationUnitAndIndicatorAccessLevel(
        Integer organisationUnitId,
        AccessLevel accessLevel);

    @Query("select oui from OrganisationUnitIndicator oui where oui.indicator.code = :code and oui.organisationUnit.id = :organisationUnitId")
    Optional<OrganisationUnitIndicator> findIndicatorForCodeAndOrganisationUnitId(String code,
                                                                                  Integer organisationUnitId);
}
