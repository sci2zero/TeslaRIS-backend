package rs.teslaris.core.repository.commontypes;

import org.springframework.stereotype.Repository;
import rs.teslaris.core.model.commontypes.Language;
import rs.teslaris.core.repository.CRUDRepository;

@Repository
public interface LanguageRepository extends CRUDRepository<Language> {
}
