package rs.teslaris.core.service.interfaces.document;

import org.springframework.stereotype.Service;
import rs.teslaris.core.dto.document.PublicationSeriesDTO;
import rs.teslaris.core.model.document.PublicationSeries;

@Service
public interface PublicationSeriesService {

    PublicationSeries findPublicationSeriesById(Integer id);

    void setPublicationSeriesCommonFields(PublicationSeries publicationSeries,
                                          PublicationSeriesDTO publicationSeriesDTO);
}
