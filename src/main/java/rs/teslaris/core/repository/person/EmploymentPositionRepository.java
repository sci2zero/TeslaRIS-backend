package rs.teslaris.core.repository.person;

import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;
import rs.teslaris.core.model.person.EmploymentPositionHierarchy;

@Repository
public interface EmploymentPositionRepository
    extends JpaRepository<EmploymentPositionHierarchy, Integer> {

    @Query("SELECT ep FROM EmploymentPositionHierarchy ep " +
        "WHERE ep.superEmploymentPosition.id = :parentId")
    List<EmploymentPositionHierarchy> getChildEmploymentPositions(Integer parentId);

    @Query("SELECT ep FROM EmploymentPositionHierarchy ep " +
        "WHERE ep.superEmploymentPosition IS NULL")
    List<EmploymentPositionHierarchy> getTopLevelEmploymentPositions();
}
