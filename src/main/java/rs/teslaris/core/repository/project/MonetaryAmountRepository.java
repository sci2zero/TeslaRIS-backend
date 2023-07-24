package rs.teslaris.core.repository.project;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import rs.teslaris.core.model.project.MonetaryAmount;

@Repository
public interface MonetaryAmountRepository extends JpaRepository<MonetaryAmount, Integer> {
}
