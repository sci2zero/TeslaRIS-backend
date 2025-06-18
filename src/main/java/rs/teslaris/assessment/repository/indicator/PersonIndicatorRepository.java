package rs.teslaris.assessment.repository.indicator;

import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;
import rs.teslaris.assessment.model.indicator.PersonIndicator;
import rs.teslaris.core.model.commontypes.AccessLevel;

@Repository
public interface PersonIndicatorRepository extends JpaRepository<PersonIndicator, Integer> {

    @Query("SELECT pi FROM PersonIndicator pi " +
        "WHERE pi.person.id = :personId AND pi.indicator.accessLevel <= :accessLevel")
    List<PersonIndicator> findIndicatorsForPersonAndIndicatorAccessLevel(Integer personId,
                                                                         AccessLevel accessLevel);

    @Query("SELECT pi FROM PersonIndicator pi " +
        "WHERE pi.indicator.code = :code AND pi.person.id = :personId")
    Optional<PersonIndicator> findIndicatorForCodeAndPersonId(String code, Integer personId);
}
