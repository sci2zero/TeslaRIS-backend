package rs.teslaris.core.assessment.service.interfaces;

import java.time.LocalDateTime;
import java.util.List;
import org.springframework.stereotype.Service;
import rs.teslaris.core.assessment.dto.IFTableResponseDTO;
import rs.teslaris.core.assessment.dto.PublicationSeriesIndicatorResponseDTO;
import rs.teslaris.core.assessment.model.EntityIndicatorSource;
import rs.teslaris.core.model.commontypes.AccessLevel;

@Service
public interface PublicationSeriesIndicatorService {

    List<PublicationSeriesIndicatorResponseDTO> getIndicatorsForPublicationSeries(
        Integer publicationSeriesId,
        AccessLevel accessLevel);

    void loadPublicationSeriesIndicatorsFromWOSCSVFiles();

    void loadPublicationSeriesIndicatorsFromSCImagoCSVFiles();

    void loadPublicationSeriesIndicatorsFromErihPlusCSVFiles();

    void loadPublicationSeriesIndicatorsFromSlavistCSVFiles();

    void scheduleIndicatorLoading(LocalDateTime dateTime,
                                  EntityIndicatorSource entityIndicatorSource, Integer userId);

    void computeFiveYearIFRank(List<Integer> classificationYears);

    void scheduleIF5RankComputation(LocalDateTime timeToRun, List<Integer> classificationYears,
                                    Integer userId);

    IFTableResponseDTO getIFTableContent(Integer publicationSeriesId, Integer fromYear,
                                         Integer toYear);
}
