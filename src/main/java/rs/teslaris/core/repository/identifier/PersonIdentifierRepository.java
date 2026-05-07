package rs.teslaris.core.repository.identifier;

import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;
import rs.teslaris.core.model.commontypes.AccessLevel;
import rs.teslaris.core.model.identifier.PersonIdentifier;

@Repository
public interface PersonIdentifierRepository extends JpaRepository<PersonIdentifier, Integer> {

    @Query("SELECT pi FROM PersonIdentifier pi " +
        "WHERE pi.person.id = :personId AND pi.identifier.accessLevel <= :accessLevel")
    List<PersonIdentifier> findIdentifiersForPersonAndIdentifierAccessLevel(Integer personId,
                                                                            AccessLevel accessLevel);
}
