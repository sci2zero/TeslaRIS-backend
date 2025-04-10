package rs.teslaris.thesislibrary.repository;

import java.util.Optional;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;
import rs.teslaris.thesislibrary.model.RegistryBookEntry;

@Repository
public interface RegistryBookEntryRepository extends JpaRepository<RegistryBookEntry, Integer> {

    @Query("SELECT rbe FROM RegistryBookEntry rbe WHERE rbe.promotion.id = :promotionId")
    Page<RegistryBookEntry> getBookEntriesForPromotion(Integer promotionId, Pageable pageable);

    @Query("SELECT rbe FROM RegistryBookEntry rbe WHERE rbe.promotion IS NULL")
    Page<RegistryBookEntry> getNonPromotedBookEntries(Pageable pageable);

    @Query("SELECT MAX(rbe.registryBookNumber) FROM RegistryBookEntry rbe")
    Integer getLastRegistryBookNumber();

    Optional<RegistryBookEntry> findByAttendanceIdentifier(String attendanceIdentifier);

    @Query("SELECT COUNT(rbe) > 0 FROM RegistryBookEntry rbe WHERE rbe.thesis.id = :thesisId")
    boolean hasThesisRegistryBookEntry(Integer thesisId);
}
