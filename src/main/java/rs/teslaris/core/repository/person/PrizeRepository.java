package rs.teslaris.core.repository.person;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import rs.teslaris.core.model.person.Prize;
import rs.teslaris.core.repository.JPASoftDeleteRepository;

@Repository
public interface PrizeRepository extends JPASoftDeleteRepository<Prize> {
}
