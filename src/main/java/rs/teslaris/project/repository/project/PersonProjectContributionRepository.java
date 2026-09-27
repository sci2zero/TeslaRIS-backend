package rs.teslaris.project.repository.project;

import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;
import rs.teslaris.project.model.project.PersonProjectContribution;

@Repository
public interface PersonProjectContributionRepository
    extends JpaRepository<PersonProjectContribution, Integer> {

    @Query("SELECT ppc.person.id FROM PersonProjectContribution ppc " +
        "WHERE ppc.project.id = :projectId AND ppc.person IS NOT NULL")
    List<Integer> findPersonIdsByProjectId(Integer projectId);
}
