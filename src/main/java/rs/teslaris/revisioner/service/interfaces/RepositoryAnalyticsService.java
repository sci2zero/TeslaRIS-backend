package rs.teslaris.revisioner.service.interfaces;

import jakarta.annotation.Nullable;
import java.time.LocalDate;
import java.util.List;
import org.springframework.core.io.InputStreamResource;
import org.springframework.stereotype.Service;
import rs.teslaris.revisioner.dto.DimensionQualityDTO;
import rs.teslaris.revisioner.dto.EntityTypeQualityDTO;
import rs.teslaris.revisioner.dto.RepositoryOverviewDTO;

@Service
public interface RepositoryAnalyticsService {

    RepositoryOverviewDTO getOverview(String profileName, Integer organisationUnitId,
                                      @Nullable LocalDate assessmentDate);

    List<EntityTypeQualityDTO> getQualityByEntityType(String profileName,
                                                      Integer organisationUnitId,
                                                      @Nullable LocalDate assessmentDate);

    List<DimensionQualityDTO> getQualityByDimension(String profileName,
                                                    Integer organisationUnitId,
                                                    @Nullable LocalDate assessmentDate);

    InputStreamResource exportOverview(String profileName, Integer organisationUnitId,
                                       @Nullable LocalDate assessmentDate, String language);

    InputStreamResource exportQualityByEntityType(String profileName, Integer organisationUnitId,
                                                  @Nullable LocalDate assessmentDate,
                                                  String language);

    InputStreamResource exportQualityByDimension(String profileName, Integer organisationUnitId,
                                                 @Nullable LocalDate assessmentDate,
                                                 String language);
}
