package rs.teslaris.thesislibrary.service.interfaces;

import java.util.List;
import org.springframework.core.io.InputStreamResource;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import rs.teslaris.core.indexmodel.DocumentPublicationIndex;
import rs.teslaris.core.util.functional.Pair;
import rs.teslaris.thesislibrary.dto.NotAddedToPromotionThesesRequestDTO;
import rs.teslaris.thesislibrary.dto.ThesisReportCountsDTO;
import rs.teslaris.thesislibrary.dto.ThesisReportRequestDTO;

@Service
public interface ThesisLibraryReportingService {

    List<ThesisReportCountsDTO> createThesisCountsReport(ThesisReportRequestDTO request);

    Page<DocumentPublicationIndex> fetchDefendedThesesInPeriod(ThesisReportRequestDTO request,
                                                               Pageable pageable);

    Page<DocumentPublicationIndex> fetchNotDefendedThesesInPeriod(ThesisReportRequestDTO request,
                                                                  Pageable pageable);

    Page<DocumentPublicationIndex> fetchAcceptedThesesInPeriod(ThesisReportRequestDTO request,
                                                               Pageable pageable);

    Page<DocumentPublicationIndex> fetchPublicReviewThesesInPeriod(ThesisReportRequestDTO request,
                                                                   Pageable pageable);

    Page<DocumentPublicationIndex> fetchPubliclyAvailableThesesInPeriod(
        ThesisReportRequestDTO request,
        Pageable pageable);

    Page<DocumentPublicationIndex> fetchClosedAccessThesesInPeriod(ThesisReportRequestDTO request,
                                                                   Pageable pageable);

    Pair<InputStreamResource, Integer> generatePhdLibraryReportDocument(
        ThesisReportRequestDTO request,
        String locale);

    Page<DocumentPublicationIndex> fetchDefendedThesesInPeriodNotSentToPromotion(
        NotAddedToPromotionThesesRequestDTO request, Integer libraryInstitutionId,
        Pageable pageable);
}
