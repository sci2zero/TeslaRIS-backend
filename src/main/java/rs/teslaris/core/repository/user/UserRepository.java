package rs.teslaris.core.repository.user;

import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;
import rs.teslaris.core.assessment.model.Commission;
import rs.teslaris.core.model.institution.OrganisationUnit;
import rs.teslaris.core.model.user.User;

@Repository
public interface UserRepository extends JpaRepository<User, Integer> {

    Optional<User> findByEmail(String email);

    @Query("select u from User u left join fetch u.organisationUnit ou where u.id = :userId")
    Optional<User> findByIdWithOrganisationUnit(Integer userId);

    @Query("select u from User u join u.person p where p.id = :personId")
    Optional<User> findForResearcher(Integer personId);

    @Query("select count(u) > 0 from User u where u.person.id = :personId")
    boolean personAlreadyBinded(Integer personId);

    @Query("select u.id from User u where u.canTakeRole = true")
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
}
