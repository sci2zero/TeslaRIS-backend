package rs.teslaris.core.service.impl.document;

import jakarta.annotation.PostConstruct;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.function.Function;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import rs.teslaris.core.indexmodel.BookSeriesIndex;
import rs.teslaris.core.indexmodel.JournalIndex;
import rs.teslaris.core.indexmodel.PublicationSeriesIndex;
import rs.teslaris.core.indexrepository.BookSeriesIndexRepository;
import rs.teslaris.core.indexrepository.JournalIndexRepository;
import rs.teslaris.core.model.document.PublicationSeries;
import rs.teslaris.core.repository.document.BookSeriesRepository;
import rs.teslaris.core.repository.document.JournalRepository;
import rs.teslaris.core.service.interfaces.document.PublicationSeriesLookupService;
import rs.teslaris.core.util.exceptionhandling.exception.NotFoundException;

@Service
@RequiredArgsConstructor
public class PublicationSeriesLookupServiceImpl implements PublicationSeriesLookupService {

    private final BookSeriesIndexRepository bookSeriesIndexRepository;

    private final JournalIndexRepository journalIndexRepository;

    private final JournalRepository journalRepository;

    private final BookSeriesRepository bookSeriesRepository;

    private Map<Class<? extends PublicationSeriesIndex>,
        Function<Integer, Optional<? extends PublicationSeriesIndex>>> indexFinders;

    private Map<Class<? extends PublicationSeriesIndex>,
        Function<Integer, Optional<? extends PublicationSeries>>> seriesFinders;


    @PostConstruct
    private void initMaps() {
        indexFinders = Map.of(
            JournalIndex.class, journalIndexRepository::findJournalIndexByDatabaseId,
            BookSeriesIndex.class, bookSeriesIndexRepository::findBookSeriesIndexByDatabaseId
        );

        seriesFinders = Map.of(
            JournalIndex.class, journalRepository::findById,
            BookSeriesIndex.class, bookSeriesRepository::findById
        );
    }

    @Override
    public PublicationSeries fastPublicationSeriesLookup(Integer publicationSeriesId) {
        var index = getPublicationSeriesIndex(publicationSeriesId);

        if (Objects.isNull(index)) {
            throw throwNotFoundException();
        }

        return fastPublicationSeriesLookup(index);
    }

    @Override
    public PublicationSeries fastPublicationSeriesLookup(PublicationSeriesIndex index) {
        return Optional.ofNullable(seriesFinders.get(index.getClass()))
            .flatMap(finder -> finder.apply(index.getDatabaseId()))
            .orElseThrow(this::throwNotFoundException);
    }

    @Override
    public PublicationSeriesIndex getPublicationSeriesIndex(Integer publicationSeriesId) {
        return indexFinders.values().stream()
            .map(finder -> finder.apply(publicationSeriesId))
            .filter(Optional::isPresent)
            .map(Optional::get)
            .findFirst()
            .orElse(null);
    }

    private NotFoundException throwNotFoundException() {
        return new NotFoundException(
            "Publication series with given ID is not present in the database.");
    }
}
