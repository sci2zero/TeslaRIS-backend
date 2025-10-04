package rs.teslaris.assessment.repository.indicator;

import jakarta.persistence.LockModeType;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;
import rs.teslaris.assessment.model.indicator.EntityIndicatorSource;
import rs.teslaris.assessment.model.indicator.EventIndicator;
import rs.teslaris.core.model.commontypes.AccessLevel;

@Repository
public interface EventIndicatorRepository extends JpaRepository<EventIndicator, Integer> {

    @Query("SELECT ei FROM EventIndicator ei " +
        "WHERE ei.event.id = :eventId AND ei.indicator.accessLevel <= :accessLevel")
    List<EventIndicator> findIndicatorsForEventAndIndicatorAccessLevel(Integer eventId,
                                                                       AccessLevel accessLevel);

    @Query("SELECT ei FROM EventIndicator ei WHERE ei.event.id = :eventId")
    Page<EventIndicator> findIndicatorsForEvent(Integer eventId, Pageable pageable);

    @Transactional
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("SELECT ei FROM EventIndicator ei WHERE " +
        "ei.event.id = :eventId AND " +
        "ei.indicator.code = :code")
    Optional<EventIndicator> findIndicatorsForCodeAndEventId(String code, Integer eventId);

    @Query("SELECT ei " +
        "FROM EventIndicator ei " +
        "WHERE ei.event.id = :eventId " +
        "AND ei.indicator.code = :indicatorCode " +
        "AND ei.source = :source " +
        "AND ei.fromDate = :date")
    Optional<EventIndicator> existsByEventIdAndSourceAndYear(Integer eventId,
                                                             EntityIndicatorSource source,
                                                             LocalDate date, String indicatorCode);
}
