package rs.teslaris.core.repository.person;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;
import rs.teslaris.core.model.person.LanguageKnowledge;

@Repository
public interface LanguageKnowledgeRepository extends JpaRepository<LanguageKnowledge, Integer> {

    @Query("SELECT COUNT(lk) > 0 FROM LanguageKnowledge lk " +
        "WHERE lk.person.id = :personId AND lk.language.id = :languageId " +
        "AND (:excludeId IS NULL OR lk.id <> :excludeId)")
    boolean existsForPersonWithLanguage(Integer personId, Integer languageId, Integer excludeId);

    @Query("SELECT COUNT(lk) > 0 FROM LanguageKnowledge lk " +
        "WHERE lk.person.id = :personId AND lk.motherTongue = TRUE " +
        "AND (:excludeId IS NULL OR lk.id <> :excludeId)")
    boolean existsMotherTongueForPerson(Integer personId, Integer excludeId);
}
