package rs.teslaris.revisioner.controller;

import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.core.io.InputStreamResource;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;
import rs.teslaris.core.util.search.StringUtil;
import rs.teslaris.revisioner.dto.PolicyFileSummaryDTO;
import rs.teslaris.revisioner.dto.PolicyUploadResultDTO;
import rs.teslaris.revisioner.model.qualityassessment.PolicyUploadMode;
import rs.teslaris.revisioner.service.interfaces.DataQualityPolicyService;

@RestController
@RequestMapping("/api/data-quality/policies")
@RequiredArgsConstructor
public class DataQualityPolicyController {

    private final DataQualityPolicyService dataQualityPolicyService;


    @GetMapping(produces = MediaType.APPLICATION_JSON_VALUE)
    @PreAuthorize("hasAnyAuthority('MANAGE_DATA_QUALITY_POLICY', 'ASSESS_DATA_QUALITY')")
    public List<PolicyFileSummaryDTO> listPolicies() {
        return dataQualityPolicyService.listPolicies();
    }

    @GetMapping("/{profileName}/{version}/download")
    @PreAuthorize("hasAnyAuthority('MANAGE_DATA_QUALITY_POLICY', 'ASSESS_DATA_QUALITY')")
    public ResponseEntity<InputStreamResource> downloadPolicyVersion(
        @PathVariable String profileName,
        @PathVariable String version) {
        return ResponseEntity.ok()
            .contentType(MediaType.APPLICATION_JSON)
            .header(HttpHeaders.CONTENT_DISPOSITION,
                StringUtil.contentDisposition(
                    profileName.toLowerCase() + "-" + version + ".json"))
            .body(dataQualityPolicyService.downloadPolicyVersion(profileName, version));
    }

    @PostMapping(value = "/{profileName}", produces = MediaType.APPLICATION_JSON_VALUE)
    @PreAuthorize("hasAuthority('MANAGE_DATA_QUALITY_POLICY')")
    public PolicyUploadResultDTO uploadPolicy(@PathVariable String profileName,
                                              @RequestParam PolicyUploadMode mode,
                                              @RequestParam("file") MultipartFile file) {
        return dataQualityPolicyService.uploadPolicy(profileName, mode, file);
    }
}
