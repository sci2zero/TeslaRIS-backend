package rs.teslaris.core.service.interfaces.document;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import rs.teslaris.core.dto.document.BookSeriesDTO;
import rs.teslaris.core.dto.document.BookSeriesResponseDTO;
import rs.teslaris.core.model.document.BookSeries;
import rs.teslaris.core.service.interfaces.JPAService;

@Service
public interface BookSeriesService extends JPAService<BookSeries> {

    Page<BookSeriesResponseDTO> readAllBookSeries(Pageable pageable);

    BookSeriesResponseDTO readBookSeries(Integer journalId);

    BookSeries createBookSeries(BookSeriesDTO bookSeriesDTO);

    void updateBookSeries(BookSeriesDTO bookSeriesDTO, Integer bookSeriesId);

    void deleteBookSeries(Integer bookSeriesId);
}
