package rs.teslaris.core.repository.document;

import java.util.Optional;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;
import rs.teslaris.core.model.document.BookSeries;

@Repository
public interface BookSeriesRepository extends JpaRepository<BookSeries, Integer> {

    @Query(value = "SELECT *, 0 AS clazz_ FROM book_series WHERE " +
        "old_ids @> to_jsonb(array[cast(?1 as int)])", nativeQuery = true)
    Optional<BookSeries> findBookSeriesByOldIdsContains(Integer oldId);

    @Query(value = "SELECT * FROM book_series bs WHERE bs.id = :bookSeriesId",
        nativeQuery = true)
    Optional<BookSeries> findRaw(Integer bookSeriesId);

    @Query(value = "SELECT * FROM book_series bs WHERE " +
        "(:allTime = TRUE OR bs.last_modification >= CURRENT_TIMESTAMP - INTERVAL '1 DAY') " +
        " ORDER BY bs.id",
        nativeQuery = true)
    Page<BookSeries> findAllModified(Pageable pageable, boolean allTime);
}
