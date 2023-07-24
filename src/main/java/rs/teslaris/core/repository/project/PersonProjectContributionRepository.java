package rs.teslaris.core.repository.project;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import rs.teslaris.core.model.project.PersonProjectContribution;

@Repository
public interface PersonProjectContributionRepository
    extends JpaRepository<PersonProjectContribution, Integer> {
}
