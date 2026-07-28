package rs.teslaris.revisioner.controller;

import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import rs.teslaris.revisioner.dto.DataQualityAssessmentDTO;
import rs.teslaris.revisioner.dto.DataQualityProfileDTO;
import rs.teslaris.revisioner.dto.QualityReportResponseDTO;
import rs.teslaris.revisioner.service.interfaces.DataQualityService;

@RestController
@RequestMapping("/api/data-quality")
@RequiredArgsConstructor
public class DataQualityController {

    private final DataQualityService dataQualityService;


    @GetMapping(value = "/report/{entityType}/{entityId}", produces = MediaType.APPLICATION_JSON_VALUE)
    public List<QualityReportResponseDTO> getQualityReportForEntity(@PathVariable String entityType,
                                                                    @PathVariable
                                                                    Integer entityId) {
        return dataQualityService.getQualityReportForEntity(entityType, entityId);
    }

    @GetMapping(value = "/assessments/{entityType}/{entityId}", produces = MediaType.APPLICATION_JSON_VALUE)
    public List<DataQualityAssessmentDTO> findOne(@PathVariable String entityType,
                                                  @PathVariable Integer entityId) {
        return dataQualityService.findLatestAssessmentsForEntity(entityType, entityId);
    }

    @GetMapping(value = "/profiles", produces = MediaType.APPLICATION_JSON_VALUE)
    public List<DataQualityProfileDTO> listAllPolicies() {
        return dataQualityService.listAllDataQualityProfiles();
    }
}
