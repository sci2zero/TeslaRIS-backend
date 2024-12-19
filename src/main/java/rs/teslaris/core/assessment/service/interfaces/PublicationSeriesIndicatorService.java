package rs.teslaris.core.assessment.service.interfaces;

import java.util.List;
import org.springframework.stereotype.Service;
import rs.teslaris.core.assessment.dto.PublicationSeriesIndicatorResponseDTO;
import rs.teslaris.core.model.commontypes.AccessLevel;

@Service
public interface PublicationSeriesIndicatorService {

    List<PublicationSeriesIndicatorResponseDTO> getIndicatorsForPublicationSeries(
        Integer publicationSeriesId,
        AccessLevel accessLevel);

    void loadPublicationSeriesIndicatorsFromWOSCSVFiles();
}
