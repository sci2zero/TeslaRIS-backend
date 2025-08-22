package rs.teslaris.assessment.repository.indicator;

import jakarta.persistence.LockModeType;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;
import rs.teslaris.assessment.model.indicator.EntityIndicatorSource;
import rs.teslaris.assessment.model.indicator.PersonIndicator;
import rs.teslaris.core.model.commontypes.AccessLevel;

@Repository
public interface PersonIndicatorRepository extends JpaRepository<PersonIndicator, Integer> {

    @Query("SELECT pi FROM PersonIndicator pi " +
        "WHERE pi.person.id = :personId AND pi.indicator.accessLevel <= :accessLevel")
    List<PersonIndicator> findIndicatorsForPersonAndIndicatorAccessLevel(Integer personId,
                                                                         AccessLevel accessLevel);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("SELECT pi FROM PersonIndicator pi " +
        "WHERE pi.indicator.code = :code AND pi.person.id = :personId")
    Optional<PersonIndicator> findIndicatorForCodeAndPersonId(String code, Integer personId);

    @Query("""
            SELECT pi FROM PersonIndicator pi
            WHERE pi.indicator.code = :code
              AND pi.source = :source
              AND pi.person.id = :personId
              AND (
                   (:fromYear IS NULL AND pi.fromDate IS NULL)
                   OR (:fromYear IS NOT NULL AND extract(year from pi.fromDate) = :fromYear)
              )
        """)
    Optional<PersonIndicator> findIndicatorForCodeAndSourceAndFromDateAndPersonId(String code,
                                                                                  EntityIndicatorSource source,
                                                                                  Integer fromYear,
                                                                                  Integer personId
    );
}
