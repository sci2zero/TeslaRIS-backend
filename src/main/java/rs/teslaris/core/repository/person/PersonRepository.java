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

    @Query("SELECT p FROM Person p LEFT JOIN FETCH p.otherNames " +
        "WHERE p.id = :id AND p.approveStatus = 1")
    Optional<Person> findApprovedByIdWithOtherNames(Integer id);

    @Query(value = "SELECT * FROM persons WHERE " +
        "old_ids @> to_jsonb(array[cast(?1 as int)])", nativeQuery = true)
    Optional<Person> findPersonByOldIdsContains(Integer oldId);

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

    @Query("SELECT u FROM User u WHERE " +
        "u.person.scopusAuthorId = :identifier OR " +
        "u.person.openAlexId = :identifier OR " +
        "u.person.orcid = :identifier OR " +
        "u.person.webOfScienceResearcherId = :identifier")
    Optional<User> findUserForPersonIdentifier(String identifier);

    @Query("SELECT u FROM User u WHERE " +
        "u.person.orcid = :orcid")
    Optional<User> findUserForOrcid(String orcid);

    @Query("SELECT p FROM Person p WHERE " +
        "p.orcid = :orcid")
    Optional<Person> findPersonForOrcid(String orcid);

    @Query("SELECT p FROM Person p WHERE " +
        "(p.scopusAuthorId = :identifier OR " +
        "p.apvnt = :identifier OR " +
        "p.eCrisId = :identifier OR " +
        "p.eNaukaId = :identifier OR " +
        "p.openAlexId = :identifier OR " +
        "p.orcid = :identifier OR " +
        "p.webOfScienceResearcherId = :identifier)")
    Optional<Person> findPersonForIdentifier(String identifier);

    @Query("SELECT u.person.id FROM User u WHERE u.id = :userId")
    Optional<Integer> findPersonIdForUserId(Integer userId);

    @Query("SELECT CASE WHEN COUNT(p) > 0 THEN TRUE ELSE FALSE END " +
        "FROM Person p WHERE p.apvnt = :apvnt AND (:id IS NULL OR p.id <> :id)")
    boolean existsByApvnt(String apvnt, Integer id);

    @Query("SELECT CASE WHEN COUNT(p) > 0 THEN TRUE ELSE FALSE END " +
        "FROM Person p WHERE p.eCrisId = :eCrisId AND (:id IS NULL OR p.id <> :id)")
    boolean existsByeCrisId(String eCrisId, Integer id);

    @Query("SELECT CASE WHEN COUNT(p) > 0 THEN TRUE ELSE FALSE END " +
        "FROM Person p WHERE p.eNaukaId = :eNaukaId AND (:id IS NULL OR p.id <> :id)")
    boolean existsByeNaukaId(String eNaukaId, Integer id);

    @Query("SELECT CASE WHEN COUNT(p) > 0 THEN TRUE ELSE FALSE END " +
        "FROM Person p WHERE p.orcid = :orcid AND (:id IS NULL OR p.id <> :id)")
    boolean existsByOrcid(String orcid, Integer id);

    @Query("SELECT CASE WHEN COUNT(p) > 0 THEN TRUE ELSE FALSE END " +
        "FROM Person p WHERE p.scopusAuthorId = :scopusAuthorId AND (:id IS NULL OR p.id <> :id)")
    boolean existsByScopusAuthorId(String scopusAuthorId, Integer id);

    @Query("SELECT CASE WHEN COUNT(p) > 0 THEN TRUE ELSE FALSE END " +
        "FROM Person p WHERE p.openAlexId = :openAlexId AND (:id IS NULL OR p.id <> :id)")
    boolean existsByOpenAlexId(String openAlexId, Integer id);

    @Query("SELECT CASE WHEN COUNT(p) > 0 THEN TRUE ELSE FALSE END " +
        "FROM Person p WHERE p.webOfScienceResearcherId = :webOfScienceResearcherId AND " +
        "(:id IS NULL OR p.id <> :id)")
    boolean existsByWebOfScienceId(String webOfScienceResearcherId, Integer id);

    @Query(value = "SELECT * FROM persons p WHERE " +
        "(:allTime = TRUE OR p.last_modification >= CURRENT_TIMESTAMP - INTERVAL '1 DAY') AND " +
        "p.approve_status = 1", nativeQuery = true)
    Page<Person> findAllModified(Pageable pageable, boolean allTime);

    @Query("SELECT i.organisationUnit.id FROM Involvement i WHERE " +
        "i.personInvolved.id = :personId AND " +
        "(i.involvementType = 4 OR i.involvementType = 5)")
    List<Integer> findInstitutionIdsForPerson(Integer personId);

    @Query(value = "SELECT * FROM persons p " +
        "WHERE p.accounting_ids @> to_jsonb(?1) " +
        "AND p.approve_status = 1",
        nativeQuery = true)
    Optional<Person> findApprovedPersonByAccountingId(String accountingId);

    @Query("SELECT p FROM Person p ORDER BY " +
        "CASE WHEN p.dateOfLastIndicatorHarvest IS NULL THEN 0 ELSE 1 END, " +
        "p.dateOfLastIndicatorHarvest ASC")
    Page<Person> findPersonsByLRUHarvest(Pageable pageable);

    @Query(value = "SELECT * FROM persons p WHERE p.id = :personId", nativeQuery = true)
    Optional<Person> findRaw(Integer personId);

    @Query("SELECT p.profilePhoto.imageServerName " +
        "FROM Person p " +
        "WHERE p.id IN :personId " +
        "AND p.profilePhoto IS NOT NULL " +
        "AND p.profilePhoto.imageServerName IS NOT NULL")
    Optional<String> findProfileImageByPersonId(Integer personId);
}
