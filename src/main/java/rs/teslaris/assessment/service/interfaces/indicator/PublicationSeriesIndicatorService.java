package rs.teslaris.assessment.service.interfaces.indicator;

import java.time.LocalDateTime;
import java.util.List;
import org.springframework.stereotype.Service;
import rs.teslaris.assessment.dto.IFTableResponseDTO;
import rs.teslaris.assessment.dto.indicator.PublicationSeriesIndicatorResponseDTO;
import rs.teslaris.assessment.model.indicator.EntityIndicatorSource;
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

    void computeFiveYearIFAndJciRank(List<Integer> classificationYears);

    void scheduleIF5AndJCIRankComputation(LocalDateTime timeToRun,
                                          List<Integer> classificationYears,
                                          Integer userId);

    IFTableResponseDTO getIFTableContent(Integer publicationSeriesId, Integer fromYear,
                                         Integer toYear);
}
