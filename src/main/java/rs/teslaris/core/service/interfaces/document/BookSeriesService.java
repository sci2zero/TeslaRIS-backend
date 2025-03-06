package rs.teslaris.core.service.interfaces.document;

import java.util.List;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import rs.teslaris.core.dto.document.BookSeriesDTO;
import rs.teslaris.core.dto.document.BookSeriesResponseDTO;
import rs.teslaris.core.indexmodel.BookSeriesIndex;
import rs.teslaris.core.indexmodel.DocumentPublicationIndex;
import rs.teslaris.core.model.document.BookSeries;

@Service
public interface BookSeriesService {

    Page<BookSeriesResponseDTO> readAllBookSeries(Pageable pageable);

    Page<BookSeriesIndex> searchBookSeries(List<String> tokens, Pageable pageable);

    Page<DocumentPublicationIndex> findPublicationsForBookSeries(Integer bookSeriesId,
                                                                 Pageable pageable);

    BookSeriesResponseDTO readBookSeries(Integer journalId);

    BookSeries findBookSeriesById(Integer bookSeriesId);

    BookSeries createBookSeries(BookSeriesDTO bookSeriesDTO, Boolean index);

    void updateBookSeries(Integer bookSeriesId, BookSeriesDTO bookSeriesDTO);

    void deleteBookSeries(Integer bookSeriesId);

    void forceDeleteBookSeries(Integer journalId);

    void reindexBookSeries();

    boolean isIdentifierInUse(String identifier, Integer publicationSeriesId);
}
