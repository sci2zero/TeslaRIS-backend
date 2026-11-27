package rs.teslaris.core.repository.commontypes;

import java.util.List;
import java.util.Set;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
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

    @Query("SELECT ra.id, n.content FROM ResearchArea ra JOIN ra.name n " +
        "WHERE n.language.languageTag = :languageTag")
    List<Object[]> getResearchAreaNames(String languageTag);

    @Query("SELECT COUNT(ra) > 0 FROM ResearchArea ra JOIN ra.superResearchArea ras WHERE ra.superResearchArea.id = :researchAreaId")
    boolean isSuperArea(Integer researchAreaId);

    @Query("SELECT COUNT(p) > 0 FROM Person p JOIN p.researchAreas ra WHERE ra.id = :researchAreaId")
    boolean isResearchedBySomeone(Integer researchAreaId);

    // Native query, because JPQL on Document makes a UNION ALL of all subclass tables.
    @Query(value = "SELECT COUNT(*) > 0 FROM documents_research_areas " +
        "WHERE research_areas_id = :researchAreaId", nativeQuery = true)
    boolean isResearchedInDocument(Integer researchAreaId);

    // Every other join table that holds research areas. Native, so the project module's
    // tables (projects, fundings, funding calls and programs) need no entity import here.
    @Query(value = """
        SELECT EXISTS (SELECT 1 FROM events_research_areas WHERE research_areas_id = :id)
            OR EXISTS (SELECT 1 FROM organisation_units_research_areas
                       WHERE research_areas_id = :id)
            OR EXISTS (SELECT 1 FROM involvements_research_areas WHERE research_areas_id = :id)
            OR EXISTS (SELECT 1 FROM educations_research_areas WHERE research_areas_id = :id)
            OR EXISTS (SELECT 1 FROM person_contributions_research_areas
                       WHERE research_areas_id = :id)
            OR EXISTS (SELECT 1 FROM expertise_or_skills_research_areas
                       WHERE research_areas_id = :id)
            OR EXISTS (SELECT 1 FROM prizes_research_areas WHERE research_areas_id = :id)
            OR EXISTS (SELECT 1 FROM projects_research_areas WHERE research_areas_id = :id)
            OR EXISTS (SELECT 1 FROM fundings_research_areas WHERE research_areas_id = :id)
            OR EXISTS (SELECT 1 FROM funding_calls_research_areas WHERE research_areas_id = :id)
            OR EXISTS (SELECT 1 FROM funding_programs_research_areas
                       WHERE research_areas_id = :id)
        """, nativeQuery = true)
    boolean isUsedByOtherEntities(@Param("id") Integer researchAreaId);

    // Native, so soft-deleted areas are included and removed from push targets.
    @Query(value = "SELECT * FROM research_area ra WHERE " +
        "(:allTime = TRUE OR ra.last_modification >= CURRENT_TIMESTAMP - INTERVAL '1 DAY') " +
        "ORDER BY ra.id", nativeQuery = true)
    Page<ResearchArea> findAllModified(Pageable pageable, boolean allTime);

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

    @Query(
        value = """
            WITH RECURSIVE research_area_tree AS (
                SELECT ra.id
                FROM research_area ra
                WHERE ra.id = :topLevelResearchAreaId
            
                UNION ALL
            
                SELECT child.id
                FROM research_area child
                JOIN research_area_tree parent
                  ON child.super_research_area_id = parent.id
            )
            SELECT id FROM research_area_tree
            """,
        nativeQuery = true
    )
    Set<Integer> findAllIdsInHierarchy(Integer topLevelResearchAreaId);
}
