package rs.teslaris.thesislibrary.service.interfaces;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import rs.teslaris.thesislibrary.dto.ThesisPublicReviewResponseDTO;
import rs.teslaris.thesislibrary.model.PublicReviewType;

@Service
public interface ThesisLibraryDissertationReportService {

    Page<ThesisPublicReviewResponseDTO> fetchPublicReviewDissertations(Integer institutionId,
                                                                       Integer year,
                                                                       Boolean notDefendedOnly,
                                                                       Integer userInstitutionId,
                                                                       PublicReviewType publicReviewType,
                                                                       Pageable pageable);
}
