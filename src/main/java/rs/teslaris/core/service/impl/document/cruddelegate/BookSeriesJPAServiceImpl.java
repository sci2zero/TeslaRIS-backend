package rs.teslaris.core.service.impl.document.cruddelegate;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Component;
import rs.teslaris.core.model.document.BookSeries;
import rs.teslaris.core.repository.document.BookSeriesRepository;
import rs.teslaris.core.service.impl.JPAServiceImpl;

@Component
public class BookSeriesJPAServiceImpl extends JPAServiceImpl<BookSeries> {


    private final BookSeriesRepository bookSeriesRepository;

    @Autowired
    public BookSeriesJPAServiceImpl(BookSeriesRepository bookSeriesRepository) {
        this.bookSeriesRepository = bookSeriesRepository;
    }

    @Override
    protected JpaRepository<BookSeries, Integer> getEntityRepository() {
        return bookSeriesRepository;
    }
}
