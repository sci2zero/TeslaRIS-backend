package rs.teslaris.core.repository.person;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import rs.teslaris.core.model.person.Membership;
import rs.teslaris.core.repository.JPASoftDeleteRepository;

@Repository
public interface MembershipRepository extends JPASoftDeleteRepository<Membership> {

}
