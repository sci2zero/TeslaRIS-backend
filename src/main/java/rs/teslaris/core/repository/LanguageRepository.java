package rs.teslaris.core.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import rs.teslaris.core.model.commontypes.Language;

@Repository
public interface LanguageRepository extends JpaRepository<Language, Integer> {
}
