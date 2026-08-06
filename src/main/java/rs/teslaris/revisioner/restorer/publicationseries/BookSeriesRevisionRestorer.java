package rs.teslaris.revisioner.restorer.publicationseries;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import rs.teslaris.core.dto.document.BookSeriesResponseDTO;
import rs.teslaris.core.indexmodel.EntityType;
import rs.teslaris.core.service.interfaces.document.BookSeriesService;
import rs.teslaris.revisioner.restorer.RevisionRestorer;

@Component
@RequiredArgsConstructor
public class BookSeriesRevisionRestorer implements RevisionRestorer<BookSeriesResponseDTO> {

    private final BookSeriesService bookSeriesService;


    @Override
    public String entityType() {
        return EntityType.BOOK_SERIES.name();
    }

    @Override
    public Class<BookSeriesResponseDTO> dtoClass() {
        return BookSeriesResponseDTO.class;
    }

    @Override
    public void restore(Integer entityId, BookSeriesResponseDTO dto) {
        bookSeriesService.updateBookSeries(entityId, dto);
    }
}
