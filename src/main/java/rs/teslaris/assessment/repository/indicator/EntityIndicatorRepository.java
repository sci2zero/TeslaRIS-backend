package rs.teslaris.assessment.repository.indicator;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import rs.teslaris.assessment.model.indicator.EntityIndicator;

public interface EntityIndicatorRepository extends JpaRepository<EntityIndicator, Integer> {

    @Query("SELECT ei FROM EntityIndicator ei WHERE ei.user.id = :userId")
    EntityIndicator findByUserId(Integer userId);

    @Query("SELECT COUNT(ei) > 0 FROM EntityIndicator ei WHERE " +
        "ei.id = :entityIndicatorId AND ei.user.id = :userId")
    boolean isUserTheOwnerOfEntityIndicator(Integer userId, Integer entityIndicatorId);
}
