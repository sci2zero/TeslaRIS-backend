package rs.teslaris.core.service.interfaces.document;

import org.springframework.stereotype.Service;
import rs.teslaris.core.model.document.PublicationSeries;
import rs.teslaris.core.service.interfaces.JPAService;

@Service
public interface PublicationSeriesService extends JPAService<PublicationSeries> {

    PublicationSeries findPublicationSeriesByIssn(String eIssn, String printIssn);
}
