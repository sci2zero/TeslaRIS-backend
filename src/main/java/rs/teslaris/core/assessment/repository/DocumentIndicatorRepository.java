package rs.teslaris.core.assessment.repository;

import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;
import rs.teslaris.core.assessment.model.DocumentIndicator;
import rs.teslaris.core.model.commontypes.AccessLevel;

@Repository
public interface DocumentIndicatorRepository extends JpaRepository<DocumentIndicator, Integer> {

    @Query("select di from DocumentIndicator di where di.document.id = :documentId and di.indicator.accessLevel <= :accessLevel")
    List<DocumentIndicator> findIndicatorsForDocumentAndIndicatorAccessLevel(Integer documentId,
                                                                             AccessLevel accessLevel);

    @Query("select di from DocumentIndicator di where di.indicator.code = :code and di.document.id = :documentId")
    DocumentIndicator findIndicatorForCodeAndDocumentId(String code, Integer documentId);
}
