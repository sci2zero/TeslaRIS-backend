package rs.teslaris.thesislibrary.repository;

import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;
import rs.teslaris.thesislibrary.model.RegistryBookEntryDraft;

@Repository
public interface RegistryBookEntryDraftRepository
    extends JpaRepository<RegistryBookEntryDraft, Integer> {

    @Query("SELECT d FROM RegistryBookEntryDraft d WHERE " +
        "d.thesis.id = :thesisId")
    Optional<RegistryBookEntryDraft> findByThesisId(Integer thesisId);

    @Modifying
    @Query("DELETE FROM RegistryBookEntryDraft d " +
        "WHERE d.thesis.id = :thesisId")
    void deleteByThesisId(Integer thesisId);
}
