package rs.teslaris.core.repository.person;

import org.springframework.stereotype.Repository;
import rs.teslaris.core.model.person.ExpertiseOrSkill;
import rs.teslaris.core.repository.JPASoftDeleteRepository;

@Repository
public interface ExpertiseOrSkillRepository extends JPASoftDeleteRepository<ExpertiseOrSkill> {
}
