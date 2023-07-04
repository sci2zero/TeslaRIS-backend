package rs.teslaris.core.repository.document;

import org.springframework.stereotype.Repository;
import rs.teslaris.core.model.document.PersonEventContribution;
import rs.teslaris.core.repository.JPASoftDeleteRepository;

@Repository
public interface PersonEventContributionRepository
    extends JPASoftDeleteRepository<PersonEventContribution> {
}
