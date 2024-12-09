package rs.teslaris.core.assessment.repository;

import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;
import rs.teslaris.core.assessment.model.PersonIndicator;
import rs.teslaris.core.model.commontypes.AccessLevel;

@Repository
public interface PersonIndicatorRepository extends JpaRepository<PersonIndicator, Integer> {

    @Query("select pi from PersonIndicator pi where pi.person.id = :personId and pi.indicator.accessLevel <= :accessLevel")
    List<PersonIndicator> findIndicatorsForPersonAndIndicatorAccessLevel(Integer personId,
                                                                         AccessLevel accessLevel);

    @Query("select pi from PersonIndicator pi where pi.indicator.code = :code and pi.person.id = :personId")
    Optional<PersonIndicator> findIndicatorForCodeAndPersonId(String code, Integer personId);
}
