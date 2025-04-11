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

    @Query("SELECT p FROM Promotion p WHERE p.finished = false")
    List<Promotion> getNonFinishedPromotions();

    @Query("SELECT p FROM Promotion p WHERE p.finished = false AND p.institution.id = :institutionId")
    List<Promotion> getNonFinishedPromotions(Integer institutionId);

    @Query("SELECT p FROM Promotion p WHERE p.institution.id = :institutionId")
    Page<Promotion> findAll(Integer institutionId, Pageable pageable);

    @Query("SELECT COUNT(rbe) > 0 FROM RegistryBookEntry rbe WHERE rbe.promotion.id = :promotionId")
    boolean hasPromotableEntries(Integer promotionId);
}
