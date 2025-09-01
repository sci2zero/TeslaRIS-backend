package rs.teslaris.assessment.repository.indicator;

import jakarta.persistence.LockModeType;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;
import rs.teslaris.assessment.model.indicator.DocumentIndicator;
import rs.teslaris.assessment.model.indicator.EntityIndicatorSource;
import rs.teslaris.core.model.commontypes.AccessLevel;

@Repository
public interface DocumentIndicatorRepository extends JpaRepository<DocumentIndicator, Integer> {

    @Query("SELECT di FROM DocumentIndicator di " +
        "WHERE di.document.id = :documentId AND di.indicator.accessLevel <= :accessLevel")
    List<DocumentIndicator> findIndicatorsForDocumentAndIndicatorAccessLevel(Integer documentId,
                                                                             AccessLevel accessLevel);

    @Transactional
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("SELECT di FROM DocumentIndicator di " +
        "WHERE di.indicator.code = :code AND di.document.id = :documentId")
    Optional<DocumentIndicator> findIndicatorForCodeAndDocumentId(String code, Integer documentId);

    @Query("SELECT di FROM DocumentIndicator di " +
        "WHERE di.indicator.code = :code AND " +
        "di.source = :source AND " +
        "di.document.id = :documentId")
    Optional<DocumentIndicator> findIndicatorForCodeAndSourceDocumentId(String code,
                                                                        EntityIndicatorSource source,
                                                                        Integer documentId);
}
