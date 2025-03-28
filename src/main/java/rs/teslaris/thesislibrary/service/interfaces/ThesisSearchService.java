package rs.teslaris.thesislibrary.service.interfaces;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import rs.teslaris.core.indexmodel.DocumentPublicationIndex;
import rs.teslaris.thesislibrary.dto.ThesisSearchRequestDTO;

@Service
public interface ThesisSearchService {

    Page<DocumentPublicationIndex> performSimpleThesisSearch(ThesisSearchRequestDTO searchRequest,
                                                             Pageable pageable);

    Page<DocumentPublicationIndex> performAdvancedThesisSearch(
        ThesisSearchRequestDTO searchRequest, Pageable pageable);
}
