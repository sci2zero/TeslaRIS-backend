package rs.teslaris.core.repository.commontypes;

import java.util.Optional;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;
import rs.teslaris.core.model.commontypes.LanguageTag;

@Repository
public interface LanguageTagRepository extends JpaRepository<LanguageTag, Integer> {

    Optional<LanguageTag> findLanguageTagByLanguageTag(String languageTag);

    @Query(value =
        "SELECT lt FROM LanguageTag lt WHERE LOWER(lt.languageTag) LIKE LOWER(CONCAT('%', :searchExpression, '%')) OR " +
            "LOWER(lt.display) LIKE LOWER(CONCAT('%', :searchExpression, '%'))",
        countQuery =
            "SELECT count(lt) FROM LanguageTag lt WHERE LOWER(lt.languageTag) LIKE LOWER(CONCAT('%', :searchExpression, '%')) OR " +
                "LOWER(lt.display) LIKE LOWER(CONCAT('%', :searchExpression, '%'))")
    Page<LanguageTag> searchLanguageTags(String searchExpression, Pageable pageable);

    @Query("SELECT count(mc) > 0 FROM MultiLingualContent mc WHERE mc.language.id = :languageTagId")
    boolean isUsedInContent(Integer languageTagId);
}
