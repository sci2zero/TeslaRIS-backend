package rs.teslaris.core.repository.document;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;
import rs.teslaris.core.model.document.Publisher;

@Repository
public interface PublisherRepository extends JpaRepository<Publisher, Integer> {

    @Query("SELECT COUNT(d) > 0 FROM Dataset d JOIN d.publisher p WHERE p.id = :publisherId")
    boolean hasPublishedDataset(Integer publisherId);

    @Query("SELECT COUNT(pa) > 0 FROM Patent pa JOIN pa.publisher p WHERE p.id = :publisherId")
    boolean hasPublishedPatent(Integer publisherId);

    @Query("SELECT COUNT(pr) > 0 FROM Proceedings pr JOIN pr.publisher p WHERE p.id = :publisherId")
    boolean hasPublishedProceedings(Integer publisherId);

    @Query("SELECT COUNT(s) > 0 FROM Software s JOIN s.publisher p WHERE p.id = :publisherId")
    boolean hasPublishedSoftware(Integer publisherId);

    @Query("SELECT COUNT(t) > 0 FROM Thesis t JOIN t.publisher p WHERE p.id = :publisherId")
    boolean hasPublishedThesis(Integer publisherId);

    @Modifying
    @Query("UPDATE Dataset d SET d.publisher = null WHERE d.publisher.id = :publisherId")
    void unbindDataset(Integer publisherId);

    @Modifying
    @Query("UPDATE Patent p SET p.publisher = null WHERE p.publisher.id = :publisherId")
    void unbindPatent(Integer publisherId);

    @Modifying
    @Query("UPDATE Proceedings p SET p.publisher = null WHERE p.publisher.id = :publisherId")
    void unbindProceedings(Integer publisherId);

    @Modifying
    @Query("UPDATE Software s SET s.publisher = null WHERE s.publisher.id = :publisherId")
    void unbindSoftware(Integer publisherId);

    @Modifying
    @Query("UPDATE Thesis t SET t.publisher = null WHERE t.publisher.id = :publisherId")
    void unbindThesis(Integer publisherId);
}
