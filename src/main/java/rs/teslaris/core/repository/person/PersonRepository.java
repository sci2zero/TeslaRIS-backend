package rs.teslaris.core.repository.person;

import java.util.List;
import java.util.Optional;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;
import rs.teslaris.core.model.person.Person;
import rs.teslaris.core.model.user.User;

@Repository
public interface PersonRepository extends JpaRepository<Person, Integer> {

    @Query("SELECT p FROM Person p WHERE p.id = :id AND p.approveStatus = 1")
    Optional<Person> findApprovedPersonById(Integer id);

    Optional<Person> findPersonByOldId(Integer oldId);

    @Query("SELECT count(i) > 0 FROM Involvement i JOIN i.personInvolved p WHERE p.id = :personId")
    boolean hasInvolvement(Integer personId);

    @Query("SELECT count(pc) > 0 FROM PersonContribution pc JOIN pc.person p WHERE p.id = :personId")
    boolean hasContribution(Integer personId);

    @Query("SELECT count(u) > 0 FROM User u JOIN u.person p WHERE p.id = :personId")
    boolean isBoundToUser(Integer personId);

    @Query("SELECT p FROM Person p left JOIN p.user pu WHERE p.id = :id AND p.approveStatus = 1")
    Optional<Person> findApprovedPersonByIdWithUser(Integer id);

    @Query("SELECT p FROM Person p WHERE p.scopusAuthorId = :scopusId")
    Optional<Person> findPersonByScopusAuthorId(String scopusId);

    @Query("SELECT u FROM User u WHERE u.person.scopusAuthorId = :scopusId")
    Optional<User> findUserForPersonScopusId(String scopusId);

    @Query("SELECT u.person.id FROM User u WHERE u.id = :userId")
    Optional<Integer> findPersonIdForUserId(Integer userId);

    @Query("SELECT CASE WHEN COUNT(p) > 0 THEN TRUE ELSE FALSE END " +
        "FROM Person p WHERE p.apvnt = :apvnt AND p.id <> :id")
    boolean existsByApvnt(String apvnt, Integer id);

    @Query("SELECT CASE WHEN COUNT(p) > 0 THEN TRUE ELSE FALSE END " +
        "FROM Person p WHERE p.eCrisId = :eCrisId AND p.id <> :id")
    boolean existsByeCrisId(String eCrisId, Integer id);

    @Query("SELECT CASE WHEN COUNT(p) > 0 THEN TRUE ELSE FALSE END " +
        "FROM Person p WHERE p.eNaukaId = :eNaukaId AND p.id <> :id")
    boolean existsByeNaukaId(String eNaukaId, Integer id);

    @Query("SELECT CASE WHEN COUNT(p) > 0 THEN TRUE ELSE FALSE END " +
        "FROM Person p WHERE p.orcid = :orcid AND p.id <> :id")
    boolean existsByOrcid(String orcid, Integer id);

    @Query("SELECT CASE WHEN COUNT(p) > 0 THEN TRUE ELSE FALSE END " +
        "FROM Person p WHERE p.scopusAuthorId = :scopusAuthorId AND p.id <> :id")
    boolean existsByScopusAuthorId(String scopusAuthorId, Integer id);

    @Query(value = "SELECT * FROM persons p WHERE " +
        "p.last_modification >= CURRENT_TIMESTAMP - INTERVAL '1 DAY'", nativeQuery = true)
    Page<Person> findAllModifiedInLast24Hours(Pageable pageable);

    @Query("SELECT i.organisationUnit.id FROM Involvement i WHERE " +
        "i.personInvolved.id = :personId AND " +
        "(i.involvementType = 4 OR i.involvementType = 5)")
    List<Integer> findInstitutionIdsForPerson(Integer personId);

    @Query("SELECT p FROM Person p " +
        "JOIN p.accountingIds aid " +
        "WHERE aid = :id AND p.approveStatus = 1")
    Optional<Person> findApprovedPersonByAccountingId(String id);

}
