package rs.teslaris.core.service.interfaces.document;

import java.util.List;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import rs.teslaris.core.dto.document.BookSeriesDTO;
import rs.teslaris.core.dto.document.BookSeriesResponseDTO;
import rs.teslaris.core.indexmodel.BookSeriesIndex;
import rs.teslaris.core.model.document.BookSeries;

@Service
public interface BookSeriesService {

    Page<BookSeriesResponseDTO> readAllBookSeries(Pageable pageable);

    Page<BookSeriesIndex> searchBookSeries(List<String> tokens, Pageable pageable);

    BookSeriesResponseDTO readBookSeries(Integer journalId);

    BookSeries createBookSeries(BookSeriesDTO bookSeriesDTO);

    void updateBookSeries(BookSeriesDTO bookSeriesDTO, Integer bookSeriesId);

    void deleteBookSeries(Integer bookSeriesId);
}
