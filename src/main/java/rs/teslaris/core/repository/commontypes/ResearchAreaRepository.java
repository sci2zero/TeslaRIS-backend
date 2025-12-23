package rs.teslaris.core.repository.commontypes;

import java.util.List;
import java.util.Set;
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

    @Query("SELECT ra FROM ResearchArea ra WHERE ra.id IN :researchAreaIds")
    List<ResearchArea> getResearchAreaByIdsIn(Set<Integer> researchAreaIds);

    @Query("SELECT ra FROM ResearchArea ra WHERE ra.superResearchArea.id = :parentId")
    List<ResearchArea> getChildResearchAreas(Integer parentId);

    @Query("SELECT ra FROM ResearchArea ra WHERE ra.superResearchArea IS NULL")
    List<ResearchArea> getTopLevelResearchAreas();

    @Query("SELECT COUNT(ra) > 0 FROM ResearchArea ra JOIN ra.superResearchArea ras WHERE ra.superResearchArea.id = :researchAreaId")
    boolean isSuperArea(Integer researchAreaId);

    @Query("SELECT COUNT(p) > 0 FROM Person p JOIN p.researchAreas ra WHERE ra.id = :researchAreaId")
    boolean isResearchedBySomeone(Integer researchAreaId);

    @Query("SELECT COUNT(m) > 0 FROM Monograph m JOIN m.researchArea WHERE m.researchArea.id = :researchAreaId")
    boolean isResearchedInMonograph(Integer researchAreaId);

    @Query(value =
        "SELECT ra FROM ResearchArea ra LEFT JOIN ra.name name WHERE " +
            "(name.language.languageTag = :languageTag AND " +
            "(LOWER(name.content) LIKE LOWER(CONCAT('%', :searchExpression, '%')) OR " +
            "ra.processedName LIKE CONCAT('%', :searchExpression, '%')))",
        countQuery =
            "SELECT count(DISTINCT ra) FROM ResearchArea ra JOIN ra.name n WHERE " +
                "(n.language.languageTag = :languageTag AND " +
                "(LOWER(n.content) LIKE LOWER(CONCAT('%', :searchExpression, '%')) OR " +
                "ra.processedName LIKE CONCAT('%', :searchExpression, '%')))")
    Page<ResearchArea> searchResearchAreas(String searchExpression, String languageTag,
                                           Pageable pageable);
}
