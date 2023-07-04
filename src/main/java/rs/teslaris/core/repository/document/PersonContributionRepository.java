package rs.teslaris.core.repository.document;

import org.springframework.stereotype.Repository;
import rs.teslaris.core.model.document.PersonContribution;
import rs.teslaris.core.repository.JPASoftDeleteRepository;

@Repository
public interface PersonContributionRepository extends JPASoftDeleteRepository<PersonContribution> {
}
