package rs.teslaris.core.repository.commontypes;

import java.util.List;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;
import rs.teslaris.core.model.commontypes.ResearchArea;

@Repository
public interface ResearchAreaRepository extends JpaRepository<ResearchArea, Integer> {

    @Query("SELECT ra FROM ResearchArea ra WHERE ra.superResearchArea IS NOT NULL " +
        "AND ra.id NOT IN (SELECT r.superResearchArea.id FROM ResearchArea r WHERE r.superResearchArea IS NOT NULL)")
    List<ResearchArea> getAllLeafs();

    @Query("select count(ra) > 0 from ResearchArea ra join ra.superResearchArea ras where ra.superResearchArea.id = :researchAreaId")
    boolean isSuperArea(Integer researchAreaId);

    @Query("select count(p) > 0 from Person p join p.researchAreas ra where ra.id = :researchAreaId")
    boolean isResearchedBySomeone(Integer researchAreaId);

    @Query("select count(m) > 0 from Monograph m join m.researchArea where m.researchArea.id = :researchAreaId")
    boolean isResearchedInMonograph(Integer researchAreaId);

    @Query("select count(t) > 0 from Thesis t join t.researchArea where t.researchArea.id = :researchAreaId")
    boolean isResearchedInThesis(Integer researchAreaId);

    @Query(value =
        "SELECT ra FROM ResearchArea ra LEFT JOIN ra.name name WHERE name.language.languageTag = :languageTag AND " +
            "LOWER(name.content) LIKE LOWER(CONCAT('%', :searchExpression, '%'))",
        countQuery =
            "SELECT count(DISTINCT ra) FROM ResearchArea ra JOIN ra.name n WHERE n.language.languageTag = :languageTag AND " +
                "LOWER(n.content) LIKE LOWER(CONCAT('%', :searchExpression, '%'))")
    Page<ResearchArea> searchResearchAreas(String searchExpression, String languageTag,
                                           Pageable pageable);
}
