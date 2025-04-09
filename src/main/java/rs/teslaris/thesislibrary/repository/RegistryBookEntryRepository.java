package rs.teslaris.thesislibrary.repository;

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
}
