package rs.teslaris.core.repository.person;

import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;
import rs.teslaris.core.model.person.Person;

@Repository
public interface PersonRepository extends JpaRepository<Person, Integer> {

    @Query("select p from Person p where p.id = :id and p.approveStatus = 1")
    Optional<Person> findApprovedPersonById(Integer id);

    Optional<Person> findPersonByOldId(Integer oldId);

    @Query("select count(i) > 0 from Involvement i join i.personInvolved p where p.id = :personId")
    boolean hasInvolvement(Integer personId);

    @Query("select count(pc) > 0 from PersonContribution pc join pc.person p where p.id = :personId")
    boolean hasContribution(Integer personId);

    @Query("select count(u) > 0 from User u join u.person p where p.id = :personId")
    boolean isBoundToUser(Integer personId);

    @Query("select p from Person p left join p.user pu where p.id = :id and p.approveStatus = 1")
    Optional<Person> findApprovedPersonByIdWithUser(Integer id);
}
