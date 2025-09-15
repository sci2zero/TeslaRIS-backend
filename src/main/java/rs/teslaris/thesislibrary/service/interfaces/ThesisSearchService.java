package rs.teslaris.thesislibrary.service.interfaces;

import java.util.List;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import rs.teslaris.core.dto.commontypes.MultilingualContentDTO;
import rs.teslaris.core.indexmodel.DocumentPublicationIndex;
import rs.teslaris.core.util.functional.Pair;
import rs.teslaris.core.util.functional.Triple;
import rs.teslaris.core.util.search.SearchRequestType;
import rs.teslaris.thesislibrary.dto.ThesisSearchRequestDTO;

@Service
public interface ThesisSearchService {

    Page<DocumentPublicationIndex> performSimpleThesisSearch(ThesisSearchRequestDTO searchRequest,
                                                             Pageable pageable);

    Page<DocumentPublicationIndex> performAdvancedThesisSearch(
        ThesisSearchRequestDTO searchRequest, Pageable pageable);

    List<Triple<String, List<MultilingualContentDTO>, String>> getSearchFields(
        Boolean onlyExportFields);

    List<Pair<String, Long>> performWordCloudSearch(ThesisSearchRequestDTO searchRequest,
                                                    SearchRequestType queryType,
                                                    boolean foreignLanguage);
}
