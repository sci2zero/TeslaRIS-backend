package rs.teslaris.core.repository.user;

import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;
import rs.teslaris.core.model.user.User;
import rs.teslaris.core.repository.JPASoftDeleteRepository;

@Repository
public interface UserRepository extends JPASoftDeleteRepository<User> {

    Optional<User> findByEmail(String email);

    @Query("select u from User u join fetch u.organisationUnit where id = :userId")
    Optional<User> findByIdWithOrganisationUnit(Integer userId);
}
