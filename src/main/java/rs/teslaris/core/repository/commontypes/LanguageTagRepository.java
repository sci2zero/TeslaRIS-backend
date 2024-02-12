package rs.teslaris.core.repository.commontypes;

import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import rs.teslaris.core.model.commontypes.LanguageTag;

@Repository
public interface LanguageTagRepository extends JpaRepository<LanguageTag, Integer> {

    Optional<LanguageTag> findLanguageTagByLanguageTag(String languageTag);
}
