package rs.teslaris.core.repository.person;

import java.util.Optional;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;
import rs.teslaris.core.model.person.Person;
import rs.teslaris.core.repository.JPASoftDeleteRepository;

@Repository
public interface PersonRepository extends JPASoftDeleteRepository<Person> {

    @Deprecated(forRemoval = true)
    @Query("select p from Person p where p.id = :id and p.approveStatus = 1 and p.deleted = false")
    Optional<Person> findApprovedPersonById(Integer id);


    @Query("select p from Person p where p.id = :id and p.approveStatus = 1 and p.deleted = false")
    Optional<Person> findApprovedPersonByIdAndDeletedIsFalse(Integer id);
}
