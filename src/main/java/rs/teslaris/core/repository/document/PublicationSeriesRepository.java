package rs.teslaris.core.repository.document;

import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;
import rs.teslaris.core.model.document.PublicationSeries;

@Repository
public interface PublicationSeriesRepository extends JpaRepository<PublicationSeries, Integer> {

    @Query("select count(p) > 0 from Proceedings p join p.publicationSeries bs where bs.id = :publicationSeriesId")
    boolean hasProceedings(Integer publicationSeriesId);

    Optional<PublicationSeries> findPublicationSeriesByeISSNOrPrintISSN(String eISSN,
                                                                        String printISSN);
}
