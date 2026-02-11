package rs.teslaris.core.repository.person;

import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;
import rs.teslaris.core.model.person.PersonFieldVisibility;

@Repository
public interface PersonFieldVisibilityRepository
    extends JpaRepository<PersonFieldVisibility, Integer> {

    @Query("SELECT pfv FROM PersonFieldVisibility pfv WHERE pfv.person.id = :personId")
    Optional<PersonFieldVisibility> getFieldVisibilityConfiguration(Integer personId);
}
