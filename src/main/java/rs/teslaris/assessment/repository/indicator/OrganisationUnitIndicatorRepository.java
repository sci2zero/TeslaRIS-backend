package rs.teslaris.assessment.repository.indicator;

import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;
import rs.teslaris.assessment.model.indicator.EntityIndicatorSource;
import rs.teslaris.assessment.model.indicator.OrganisationUnitIndicator;
import rs.teslaris.core.model.commontypes.AccessLevel;

@Repository
public interface OrganisationUnitIndicatorRepository
    extends JpaRepository<OrganisationUnitIndicator, Integer> {

    @Query("SELECT oui FROM OrganisationUnitIndicator oui " +
        "WHERE oui.organisationUnit.id = :organisationUnitId AND " +
        "oui.indicator.accessLevel <= :accessLevel")
    List<OrganisationUnitIndicator> findIndicatorsForOrganisationUnitAndIndicatorAccessLevel(
        Integer organisationUnitId,
        AccessLevel accessLevel);

    @Transactional
    @Query("SELECT oui FROM OrganisationUnitIndicator oui " +
        "WHERE oui.indicator.code = :code AND oui.organisationUnit.id = :organisationUnitId")
    Optional<OrganisationUnitIndicator> findIndicatorForCodeAndOrganisationUnitId(String code,
                                                                                  Integer organisationUnitId);

    @Query("SELECT oui FROM OrganisationUnitIndicator oui " +
        "WHERE oui.indicator.code = :code AND " +
        "oui.source = :source AND " +
        "oui.organisationUnit.id = :organisationUnitId")
    Optional<OrganisationUnitIndicator> findIndicatorForCodeAndSourceAndOrganisationUnitId(
        String code,
        EntityIndicatorSource source,
        Integer organisationUnitId
    );
}
