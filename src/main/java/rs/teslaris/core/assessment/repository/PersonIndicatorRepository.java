package rs.teslaris.core.assessment.repository;

import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;
import rs.teslaris.core.assessment.model.IndicatorAccessLevel;
import rs.teslaris.core.assessment.model.PersonIndicator;

@Repository
public interface PersonIndicatorRepository extends JpaRepository<PersonIndicator, Integer> {

    @Query("select pi from PersonIndicator pi where pi.person.id = :personId and pi.indicator.accessLevel <= :accessLevel")
    List<PersonIndicator> findIndicatorsForPersonAndIndicatorAccessLevel(Integer personId,
                                                                         IndicatorAccessLevel accessLevel);
}
