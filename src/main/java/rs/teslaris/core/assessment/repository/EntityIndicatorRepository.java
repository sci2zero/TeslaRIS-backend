package rs.teslaris.core.assessment.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import rs.teslaris.core.assessment.model.EntityIndicator;

public interface EntityIndicatorRepository extends JpaRepository<EntityIndicator, Integer> {

    @Query("select ei from EntityIndicator ei where ei.user.id = :userId")
    EntityIndicator findByUserId(Integer userId);

    @Query("select count(ei) > 0 from EntityIndicator ei where " +
        "ei.id = :entityIndicatorId and ei.user.id = :userId")
    boolean isUserTheOwnerOfEntityIndicator(Integer userId, Integer entityIndicatorId);
}
