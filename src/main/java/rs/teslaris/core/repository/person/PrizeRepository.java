package rs.teslaris.core.repository.person;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import rs.teslaris.core.model.person.Prize;

@Repository
public interface PrizeRepository extends JpaRepository<Prize, Integer> {
}
