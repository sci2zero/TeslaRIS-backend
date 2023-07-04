package rs.teslaris.core.repository.project;

import org.springframework.stereotype.Repository;
import rs.teslaris.core.model.project.PersonProjectContribution;
import rs.teslaris.core.repository.JPASoftDeleteRepository;

@Repository
public interface PersonProjectContributionRepository
    extends JPASoftDeleteRepository<PersonProjectContribution> {
}
