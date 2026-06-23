package rs.teslaris.revisioner.hydrator.publicationseries;

import java.util.ArrayList;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;
import rs.teslaris.core.dto.document.BookSeriesResponseDTO;
import rs.teslaris.core.indexmodel.EntityType;
import rs.teslaris.core.service.interfaces.commontypes.CountryService;
import rs.teslaris.core.service.interfaces.commontypes.LanguageService;
import rs.teslaris.revisioner.hydrator.RevisionHydrator;

@Component
public class BookSeriesRevisionHydrator extends RevisionHydrator<BookSeriesResponseDTO> {

    private final LanguageService languageService;


    @Autowired
    public BookSeriesRevisionHydrator(
        CountryService countryService, LanguageService languageService) {
        super(countryService);
        this.languageService = languageService;
    }

    @Override
    public String entityType() {
        return EntityType.BOOK_SERIES.name();
    }

    @Override
    @Transactional(readOnly = true)
    public void hydrate(BookSeriesResponseDTO dto) {
        dto.setLanguageTagNames(new ArrayList<>());
        dto.getLanguageIds()
            .forEach(languageId -> dto.getLanguageTagNames().add(
                languageService.findOne(languageId).getLanguageCode()));
    }
}
