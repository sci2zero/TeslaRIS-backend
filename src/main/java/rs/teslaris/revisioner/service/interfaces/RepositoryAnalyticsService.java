package rs.teslaris.revisioner.service.interfaces;

import jakarta.annotation.Nullable;
import java.time.LocalDate;
import java.util.List;
import org.springframework.stereotype.Service;
import rs.teslaris.revisioner.dto.EntityTypeQualityDTO;

@Service
public interface RepositoryAnalyticsService {

    List<EntityTypeQualityDTO> getQualityByEntityType(String profileName,
                                                      Integer organisationUnitId,
                                                      @Nullable LocalDate assessmentDate);
}
