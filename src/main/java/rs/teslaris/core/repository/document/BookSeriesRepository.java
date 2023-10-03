package rs.teslaris.core.repository.document;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;
import rs.teslaris.core.model.document.BookSeries;

@Repository
public interface BookSeriesRepository extends JpaRepository<BookSeries, Integer> {

    @Query("select count(p) > 0 from Proceedings p join p.publicationSeries bs where bs.id = :publicationSeriesId")
    boolean hasProceedings(Integer publicationSeriesId);
}
