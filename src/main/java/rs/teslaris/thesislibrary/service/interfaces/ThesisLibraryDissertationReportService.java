package rs.teslaris.thesislibrary.service.interfaces;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import rs.teslaris.thesislibrary.dto.ThesisPublicReviewResponseDTO;

@Service
public interface ThesisLibraryDissertationReportService {

    Page<ThesisPublicReviewResponseDTO> fetchPublicReviewDissertations(Integer institutionId,
                                                                       Integer year,
                                                                       Boolean notDefendedOnly,
                                                                       Pageable pageable);
}
