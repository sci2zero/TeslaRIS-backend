package rs.teslaris.core.repository.person;

import java.util.Collection;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;
import rs.teslaris.core.model.person.ExpertiseOrSkill;

@Repository
public interface ExpertiseOrSkillRepository extends JpaRepository<ExpertiseOrSkill, Integer> {

    @Query("SELECT COUNT(e) > 0 FROM ExpertiseOrSkill e JOIN e.name n " +
        "WHERE e.person.id = :personId " +
        "AND LOWER(TRIM(n.content)) IN :names")
    boolean existsForPersonWithName(Integer personId, Collection<String> names);
}
