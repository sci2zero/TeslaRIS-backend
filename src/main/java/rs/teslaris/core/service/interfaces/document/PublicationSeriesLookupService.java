package rs.teslaris.core.service.interfaces.document;

import org.springframework.stereotype.Service;
import rs.teslaris.core.indexmodel.PublicationSeriesIndex;
import rs.teslaris.core.model.document.PublicationSeries;

@Service
public interface PublicationSeriesLookupService {

    PublicationSeries fastPublicationSeriesLookup(Integer publicationSeriesId);

    PublicationSeries fastPublicationSeriesLookup(PublicationSeriesIndex index);

    PublicationSeriesIndex getPublicationSeriesIndex(Integer publicationSeriesId);
}
