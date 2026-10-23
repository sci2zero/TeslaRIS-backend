package rs.teslaris.project.repository.funding;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;
import rs.teslaris.project.model.funding.Funding;

@Repository
public interface FundingRepository extends JpaRepository<Funding, Integer> {

    @Query("SELECT CASE WHEN COUNT(f) > 0 THEN TRUE ELSE FALSE END " +
        "FROM Funding f WHERE f.doi = :doi AND (:id IS NULL OR f.id <> :id)")
    boolean existsByDoi(String doi, Integer id);
}
