package rs.teslaris.thesislibrary.service.interfaces;

import java.io.InputStream;
import java.util.List;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import rs.teslaris.core.indexmodel.DocumentPublicationIndex;
import rs.teslaris.thesislibrary.dto.ThesisReportCountsDTO;
import rs.teslaris.thesislibrary.dto.ThesisReportRequestDTO;

@Service
public interface ThesisLibraryReportingService {

    List<ThesisReportCountsDTO> createThesisCountsReport(ThesisReportRequestDTO request);

    Page<DocumentPublicationIndex> fetchDefendedThesesInPeriod(ThesisReportRequestDTO request,
                                                               Pageable pageable);

    Page<DocumentPublicationIndex> fetchAcceptedThesesInPeriod(ThesisReportRequestDTO request,
                                                               Pageable pageable);

    Page<DocumentPublicationIndex> fetchPublicReviewThesesInPeriod(ThesisReportRequestDTO request,
                                                                   Pageable pageable);

    Page<DocumentPublicationIndex> fetchPubliclyAvailableThesesInPeriod(
        ThesisReportRequestDTO request,
        Pageable pageable);

    InputStream generatePhdLibraryReportDocument(ThesisReportRequestDTO request, String locale);
}
