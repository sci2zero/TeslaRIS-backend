package rs.teslaris.assessment.repository;

import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;
import rs.teslaris.assessment.model.DocumentIndicator;
import rs.teslaris.core.model.commontypes.AccessLevel;

@Repository
public interface DocumentIndicatorRepository extends JpaRepository<DocumentIndicator, Integer> {

    @Query("select di from DocumentIndicator di where di.document.id = :documentId and di.indicator.accessLevel <= :accessLevel")
    List<DocumentIndicator> findIndicatorsForDocumentAndIndicatorAccessLevel(Integer documentId,
                                                                             AccessLevel accessLevel);

    @Query("select di from DocumentIndicator di where di.indicator.code = :code and di.document.id = :documentId")
    Optional<DocumentIndicator> findIndicatorForCodeAndDocumentId(String code, Integer documentId);
}
