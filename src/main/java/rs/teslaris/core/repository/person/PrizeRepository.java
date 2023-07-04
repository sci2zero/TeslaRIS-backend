package rs.teslaris.core.repository.person;

import org.springframework.stereotype.Repository;
import rs.teslaris.core.model.person.Prize;
import rs.teslaris.core.repository.JPASoftDeleteRepository;

@Repository
public interface PrizeRepository extends JPASoftDeleteRepository<Prize> {
}
