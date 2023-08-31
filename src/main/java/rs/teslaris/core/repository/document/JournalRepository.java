package rs.teslaris.core.repository.document;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;
import rs.teslaris.core.model.document.PublicationSeries;

@Repository
public interface JournalRepository extends JpaRepository<PublicationSeries, Integer> {

    @Query("select count(p) > 0 from JournalPublication p join p.publicationSeries j where j.id = :publicationSeriesId")
    boolean hasPublication(Integer publicationSeriesId);

    @Query("select count(p) > 0 from Proceedings p join p.publicationSeries j where j.id = :publicationSeriesId")
    boolean hasProceedings(Integer publicationSeriesId);
}
