package rs.teslaris.thesislibrary.repository;

import java.util.List;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;
import rs.teslaris.thesislibrary.model.Promotion;

@Repository
public interface PromotionRepository extends JpaRepository<Promotion, Integer> {

    @Query("SELECT p FROM Promotion p WHERE p.finished = :finished")
    List<Promotion> getPromotionsBasedOnStatus(boolean finished);

    @Query("SELECT p FROM Promotion p WHERE p.finished = :finished AND p.institution.id IN :institutionIds")
    List<Promotion> getPromotionsBasedOnStatus(List<Integer> institutionIds, boolean finished);

    @Query("SELECT p FROM Promotion p WHERE " +
        "(:institutionId IS NULL OR p.institution.id = :institutionId) " +
        "AND (:nonFinishedOnly = FALSE OR p.finished = FALSE)")
    Page<Promotion> findAll(Integer institutionId, boolean nonFinishedOnly, Pageable pageable);

    @Query("SELECT COUNT(rbe) > 0 FROM RegistryBookEntry rbe WHERE rbe.promotion.id = :promotionId")
    boolean hasPromotableEntries(Integer promotionId);
}
