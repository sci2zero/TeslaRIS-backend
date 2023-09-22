package rs.teslaris.core.service.impl.document;

import java.util.HashSet;
import javax.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Service;
import rs.teslaris.core.converter.document.BookSeriesConverter;
import rs.teslaris.core.dto.document.BookSeriesDTO;
import rs.teslaris.core.dto.document.BookSeriesResponseDTO;
import rs.teslaris.core.model.document.BookSeries;
import rs.teslaris.core.repository.document.BookSeriesRepository;
import rs.teslaris.core.service.impl.JPAServiceImpl;
import rs.teslaris.core.service.interfaces.commontypes.LanguageTagService;
import rs.teslaris.core.service.interfaces.commontypes.MultilingualContentService;
import rs.teslaris.core.service.interfaces.document.BookSeriesService;
import rs.teslaris.core.service.interfaces.person.PersonContributionService;
import rs.teslaris.core.util.exceptionhandling.exception.BookSeriesReferenceConstraintViolationException;

@Service
@RequiredArgsConstructor
@Transactional
public class BookSeriesServiceImpl extends JPAServiceImpl<BookSeries> implements BookSeriesService {

    private final BookSeriesRepository bookSeriesRepository;

    private final MultilingualContentService multilingualContentService;

    private final LanguageTagService languageTagService;

    private final PersonContributionService personContributionService;

    @Override
    protected JpaRepository<BookSeries, Integer> getEntityRepository() {
        return bookSeriesRepository;
    }

    @Override
    public Page<BookSeriesResponseDTO> readAllBookSeries(Pageable pageable) {
        return bookSeriesRepository.findAll(pageable).map(BookSeriesConverter::toDTO);
    }

    @Override
    public BookSeriesResponseDTO readBookSeries(Integer bookSeriesId) {
        return BookSeriesConverter.toDTO(findOne(bookSeriesId));
    }

    @Override
    public BookSeries createBookSeries(BookSeriesDTO bookSeriesDTO) {
        var bookSeries = new BookSeries();
        bookSeries.setLanguages(new HashSet<>());

        setCommonFields(bookSeries, bookSeriesDTO);

        return bookSeriesRepository.save(bookSeries);
    }

    @Override
    public void updateBookSeries(BookSeriesDTO bookSeriesDTO, Integer bookSeriesId) {
        var bookSeriesToUpdate = findOne(bookSeriesId);
        bookSeriesToUpdate.getLanguages().clear();

        setCommonFields(bookSeriesToUpdate, bookSeriesDTO);

        bookSeriesRepository.save(bookSeriesToUpdate);
    }

    @Override
    public void deleteBookSeries(Integer bookSeriesId) {
        if (bookSeriesRepository.hasProceedings(bookSeriesId)) {
            throw new BookSeriesReferenceConstraintViolationException(
                "BookSeries with given ID is already in use.");
        }

        this.delete(bookSeriesId);
    }

    private void setCommonFields(BookSeries bookSeries, BookSeriesDTO bookSeriesDTO) {
        bookSeries.setTitle(
            multilingualContentService.getMultilingualContent(bookSeriesDTO.getTitle()));
        bookSeries.setNameAbbreviation(
            multilingualContentService.getMultilingualContent(bookSeriesDTO.getNameAbbreviation()));

        bookSeries.setEISSN(bookSeriesDTO.getEISSN());
        bookSeries.setPrintISSN(bookSeriesDTO.getPrintISSN());

        personContributionService.setPersonPublicationSeriesContributionsForBookSeries(bookSeries,
            bookSeriesDTO);

        bookSeriesDTO.getLanguageTagIds().forEach(languageTagId -> {
            bookSeries.getLanguages()
                .add(languageTagService.findLanguageTagById(languageTagId));
        });
    }
}
