package rs.teslaris.core.repository.document;

import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;
import rs.teslaris.core.model.document.PublicationSeries;

@Repository
public interface PublicationSeriesRepository extends JpaRepository<PublicationSeries, Integer> {

    @Query("SELECT COUNT(p) > 0 FROM Proceedings p " +
        "JOIN p.publicationSeries bs " +
        "WHERE bs.id = :publicationSeriesId")
    boolean hasProceedings(Integer publicationSeriesId);

    @Modifying
    @Query("UPDATE Proceedings p SET p.publicationSeries = null WHERE p.publicationSeries.id = :publicationSeriesId")
    void unbindProceedings(Integer publicationSeriesId);


    @Query("SELECT CASE WHEN COUNT(p) > 0 THEN TRUE ELSE FALSE END " +
        "FROM PublicationSeries p WHERE (p.eISSN = :eISSN OR p.printISSN = :eISSN) AND p.id <> :id")
    boolean existsByeISSN(String eISSN, Integer id);

    @Query("SELECT CASE WHEN COUNT(p) > 0 THEN TRUE ELSE FALSE END " +
        "FROM PublicationSeries p WHERE (p.printISSN = :printISSN OR p.eISSN = :printISSN) AND p.id <> :id")
    boolean existsByPrintISSN(String printISSN, Integer id);

    @Query("SELECT ps FROM PublicationSeries ps WHERE (ps.printISSN = :printISSN AND " +
        "ps.eISSN = :eISSN) OR (ps.eISSN = :printISSN AND ps.printISSN = :eISSN)")
    List<PublicationSeries> findPublicationSeriesByeISSNOrPrintISSN(String eISSN,
                                                                    String printISSN);

}
