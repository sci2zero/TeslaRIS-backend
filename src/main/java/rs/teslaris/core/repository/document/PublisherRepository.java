package rs.teslaris.core.repository.document;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;
import rs.teslaris.core.model.document.Publisher;

@Repository
public interface PublisherRepository extends JpaRepository<Publisher, Integer> {

    @Query("select count(d) > 0 from Dataset d join d.publisher p where p.id = :publisherId")
    boolean hasPublishedDataset(Integer publisherId);

    @Query("select count(pa) > 0 from Patent pa join pa.publisher p where p.id = :publisherId")
    boolean hasPublishedPatent(Integer publisherId);

    @Query("select count(pr) > 0 from Proceedings pr join pr.publisher p where p.id = :publisherId")
    boolean hasPublishedProceedings(Integer publisherId);

    @Query("select count(s) > 0 from Software s join s.publisher p where p.id = :publisherId")
    boolean hasPublishedSoftware(Integer publisherId);

    @Query("select count(t) > 0 from Thesis t join t.publisher p where p.id = :publisherId")
    boolean hasPublishedThesis(Integer publisherId);
}
