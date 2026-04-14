package rs.teslaris.core.repository.person;

import java.util.List;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;
import rs.teslaris.core.model.person.EmploymentPositionHierarchy;

@Repository
public interface EmploymentPositionRepository
    extends JpaRepository<EmploymentPositionHierarchy, Integer> {

    @Query("SELECT ep FROM EmploymentPositionHierarchy ep " +
        "WHERE ep.superEmploymentPosition.id = :parentId AND " +
        "(:schemeName IS NULL OR ep.schemeName = :schemeName)")
    List<EmploymentPositionHierarchy> getChildEmploymentPositions(Integer parentId,
                                                                  String schemeName);

    @Query("SELECT ep FROM EmploymentPositionHierarchy ep " +
        "WHERE ep.superEmploymentPosition IS NULL AND " +
        "ep.schemeName = :schemeName")
    List<EmploymentPositionHierarchy> getTopLevelEmploymentPositions(String schemeName);

    @Query(value =
        "SELECT ep FROM EmploymentPositionHierarchy ep LEFT JOIN ep.name name WHERE " +
            "(name.language.languageTag = :languageTag AND " +
            "(LOWER(name.content) LIKE LOWER(CONCAT('%', :searchExpression, '%')) OR " +
            "ep.processedName LIKE CONCAT('%', :searchExpression, '%')))",
        countQuery =
            "SELECT count(DISTINCT ep) FROM EmploymentPositionHierarchy ep JOIN ep.name n WHERE " +
                "(n.language.languageTag = :languageTag AND " +
                "(LOWER(n.content) LIKE LOWER(CONCAT('%', :searchExpression, '%')) OR " +
                "ep.processedName LIKE CONCAT('%', :searchExpression, '%')))")
    Page<EmploymentPositionHierarchy> searchEmploymentPositions(String searchExpression,
                                                                String languageTag,
                                                                Pageable pageable);
}
