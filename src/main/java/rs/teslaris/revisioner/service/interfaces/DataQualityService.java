package rs.teslaris.revisioner.service.interfaces;

import java.util.List;
import org.springframework.stereotype.Service;
import rs.teslaris.revisioner.dto.DataQualityAssessmentDTO;
import rs.teslaris.revisioner.dto.DataQualityProfileDTO;
import rs.teslaris.revisioner.dto.QualityReportResponseDTO;

@Service
public interface DataQualityService {

    List<QualityReportResponseDTO> getQualityReportForEntity(String entityType, Integer entityId);

    List<DataQualityAssessmentDTO> findLatestAssessmentsForEntity(String entityType,
                                                                  Integer entityId);

    List<DataQualityAssessmentDTO> findAssessmentsForEntityVersion(String entityType,
                                                                   Integer entityId,
                                                                   Integer majorVersion,
                                                                   Integer minorVersion);

    List<DataQualityProfileDTO> listAllDataQualityProfiles();
}
