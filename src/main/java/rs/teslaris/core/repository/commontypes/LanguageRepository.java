package rs.teslaris.core.repository.commontypes;

import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;
import rs.teslaris.core.model.commontypes.Language;

@Repository
public interface LanguageRepository extends JpaRepository<Language, Integer> {

    @Query("SELECT l FROM Language l WHERE l.languageCode = :code")
    Optional<Language> getLanguageByLanguageCode(String code);

    @Query("SELECT CASE WHEN COUNT(l) > 0 THEN TRUE ELSE FALSE END " +
        "FROM Language l WHERE l.languageCode = :code AND (:id IS NULL OR l.id <> :id)")
    boolean existsByCode(String code, Integer id);
}
