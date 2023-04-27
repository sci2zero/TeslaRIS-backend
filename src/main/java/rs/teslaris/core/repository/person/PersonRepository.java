package rs.teslaris.core.repository.person;

import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;
import rs.teslaris.core.model.person.Person;

@Repository
public interface PersonRepository extends JpaRepository<Person, Integer> {

    @Query("select p from Person p join fetch p.otherNames join fetch p.biography join fetch p.keyword where p.id = :id")
    Optional<Person> findPersonWithIdWithBasicInfo(Integer id);
}
