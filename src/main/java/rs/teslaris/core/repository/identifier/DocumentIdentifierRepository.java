package rs.teslaris.core.repository.identifier;

import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;
import rs.teslaris.core.model.commontypes.AccessLevel;
import rs.teslaris.core.model.identifier.DocumentIdentifier;

@Repository
public interface DocumentIdentifierRepository extends JpaRepository<DocumentIdentifier, Integer> {

    @Query("SELECT di FROM DocumentIdentifier di " +
        "WHERE di.document.id = :documentId AND di.identifier.accessLevel <= :accessLevel")
    List<DocumentIdentifier> findIdentifiersForDocumentAndIdentifierAccessLevel(Integer documentId,
                                                                                AccessLevel accessLevel);
}
