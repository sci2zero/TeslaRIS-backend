package rs.teslaris.core.repository.identifier;

import java.util.Collection;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;
import rs.teslaris.core.model.commontypes.AccessLevel;
import rs.teslaris.core.model.identifier.PersonIdentifier;
import rs.teslaris.core.model.person.Person;

@Repository
public interface PersonIdentifierRepository extends JpaRepository<PersonIdentifier, Integer> {

    @Query("SELECT pi FROM PersonIdentifier pi JOIN FETCH pi.identifier i " +
        "WHERE pi.person.id = :personId AND i.accessLevel <= :accessLevel " +
        "ORDER BY i.code, pi.value")
    List<PersonIdentifier> findIdentifiersForPersonAndIdentifierAccessLevel(Integer personId,
                                                                            AccessLevel accessLevel);

    @Query("SELECT pi FROM PersonIdentifier pi JOIN FETCH pi.identifier i " +
        "WHERE pi.person.id IN :personIds AND i.accessLevel <= :accessLevel " +
        "ORDER BY i.code, pi.value")
    List<PersonIdentifier> findIdentifiersForPersonsAndIdentifierAccessLevel(
        Collection<Integer> personIds, AccessLevel accessLevel);

    @Query("SELECT p FROM PersonIdentifier pi JOIN pi.person p WHERE pi.value = :value")
    List<Person> findPersonsByIdentifierValue(String value);

    @Query("SELECT CASE WHEN COUNT(pi) > 0 THEN TRUE ELSE FALSE END FROM PersonIdentifier pi " +
        "WHERE pi.value = :value AND (:personId IS NULL OR pi.person.id <> :personId)")
    boolean existsByValue(String value, Integer personId);
}
