package rs.teslaris.core.repository.user;

import java.util.Optional;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;
import rs.teslaris.core.model.user.User;
import rs.teslaris.core.repository.JPASoftDeleteRepository;

@Repository
public interface UserRepository extends JPASoftDeleteRepository<User> {

    @Deprecated(forRemoval = true)
    Optional<User> findByEmail(String email);


    Optional<User> findByEmailAndDeletedIsFalse(String email);

    @Deprecated(forRemoval = true)
    @Query("select u from User u join fetch u.organisationUnit ou where id = :userId and u.deleted = false and ou.deleted = false")
    Optional<User> findByIdWithOrganisationUnit(Integer userId);


    @Query("select u from User u join fetch u.organisationUnit ou where id = :userId and u.deleted = false and ou.deleted = false")
    Optional<User> findByIdWithOrganisationUnitAndDeletedIsFalse(Integer userId);
}
