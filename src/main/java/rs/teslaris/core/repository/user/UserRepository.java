package rs.teslaris.core.repository.user;

import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;
import rs.teslaris.assessment.model.Commission;
import rs.teslaris.core.model.institution.OrganisationUnit;
import rs.teslaris.core.model.user.User;

@Repository
public interface UserRepository extends JpaRepository<User, Integer> {

    Optional<User> findByEmail(String email);

    @Query("SELECT u FROM User u LEFT JOIN FETCH u.organisationUnit ou WHERE u.id = :userId")
    Optional<User> findByIdWithOrganisationUnit(Integer userId);

    @Query("SELECT u.commission.id FROM User u WHERE u.id = :userId")
    Optional<Integer> findCommissionIdForUser(Integer userId);

    @Query("SELECT u.organisationUnit.id FROM User u WHERE u.id = :userId")
    Integer findOrganisationUnitIdForUser(Integer userId);

    @Query("SELECT u FROM User u JOIN u.person p WHERE p.id = :personId")
    Optional<User> findForResearcher(Integer personId);

    @Query("SELECT COUNT(u) > 0 FROM User u WHERE u.person.id = :personId")
    boolean personAlreadyBinded(Integer personId);

    @Query("SELECT u.id FROM User u WHERE u.canTakeRole = true")
    List<Integer> getIdsOfUsersWhoAllowedAccountTakeover();

    @Query("""
            SELECT DISTINCT u.commission
            FROM User u
            JOIN u.organisationUnit ou
            LEFT JOIN FETCH u.commission.relations rel
            LEFT JOIN FETCH rel.targetCommissions
            WHERE u.commission IS NOT NULL
            AND ou.id = :organisationUnitId
        """)
    List<Commission> findUserCommissionForOrganisationUnit(Integer organisationUnitId);


    @Query("SELECT ou.id FROM User u JOIN u.organisationUnit ou WHERE u.commission.id = :commissionId")
    Integer findOUIdForCommission(Integer commissionId);

    @Query("SELECT ou FROM User u JOIN u.organisationUnit ou WHERE u.commission.id = :commissionId")
    Optional<OrganisationUnit> findOUForCommission(Integer commissionId);

    @Query("SELECT u from User u where u.authority.name = 'COMMISSION'")
    List<User> findAllCommissionUsers();
}
