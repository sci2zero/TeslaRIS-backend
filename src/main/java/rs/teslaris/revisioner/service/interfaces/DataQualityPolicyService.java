package rs.teslaris.revisioner.service.interfaces;

import java.util.List;
import org.springframework.core.io.InputStreamResource;
import org.springframework.web.multipart.MultipartFile;
import rs.teslaris.revisioner.dto.PolicyFileSummaryDTO;
import rs.teslaris.revisioner.dto.PolicyUploadResultDTO;
import rs.teslaris.revisioner.model.qualityassessment.PolicyUploadMode;

public interface DataQualityPolicyService {

    List<PolicyFileSummaryDTO> listPolicies();

    InputStreamResource downloadPolicyVersion(String profileName, String version);

    PolicyUploadResultDTO uploadPolicy(String profileName, PolicyUploadMode mode,
                                       MultipartFile file);
}
