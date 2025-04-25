package rs.teslaris.core.repository.document;

import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;
import rs.teslaris.core.model.document.DocumentFileBackup;

@Repository
public interface DocumentFileBackupRepository extends JpaRepository<DocumentFileBackup, Integer> {

    @Query("SELECT dfb FROM DocumentFileBackup dfb WHERE dfb.institution.id = :institutionId")
    List<DocumentFileBackup> findByInstitution(Integer institutionId);

    Optional<DocumentFileBackup> findByBackupFileName(String backupFileName);
}
