package rs.teslaris.core.repository.commontypes;

import org.springframework.stereotype.Repository;
import rs.teslaris.core.model.commontypes.Country;
import rs.teslaris.core.repository.JPASoftDeleteRepository;

@Repository
public interface CountryRepository extends JPASoftDeleteRepository<Country> {
}
