package rs.teslaris.thesislibrary.controller;

import jakarta.validation.Valid;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import rs.teslaris.core.annotation.Traceable;
import rs.teslaris.core.dto.commontypes.MultilingualContentDTO;
import rs.teslaris.core.indexmodel.DocumentPublicationIndex;
import rs.teslaris.core.util.exceptionhandling.exception.MissingDataException;
import rs.teslaris.core.util.functional.Pair;
import rs.teslaris.core.util.functional.Triple;
import rs.teslaris.core.util.search.SearchRequestType;
import rs.teslaris.thesislibrary.dto.ThesisSearchRequestDTO;
import rs.teslaris.thesislibrary.service.interfaces.ThesisSearchService;

@RestController
@RequestMapping("/api/thesis-library/search")
@RequiredArgsConstructor
@Traceable
public class ThesisLibrarySearchController {

    private final ThesisSearchService thesisSearchService;


    @GetMapping("/fields")
    public List<Triple<String, List<MultilingualContentDTO>, String>> getSearchFields(
        @RequestParam("export") Boolean onlyExportFields) {
        return thesisSearchService.getSearchFields(onlyExportFields);
    }

    @PostMapping("/simple")
    public Page<DocumentPublicationIndex> performSimpleSearch(
        @RequestBody @Valid ThesisSearchRequestDTO searchRequest, Pageable pageable) {
        return thesisSearchService.performSimpleThesisSearch(searchRequest, pageable);
    }

    @PostMapping("/advanced")
    public Page<DocumentPublicationIndex> performAdvancedSearch(
        @RequestBody @Valid ThesisSearchRequestDTO searchRequest, Pageable pageable) {
        return thesisSearchService.performAdvancedThesisSearch(searchRequest, pageable);
    }

    @PostMapping("/wordcloud/{queryType}")
    public List<Pair<String, Long>> performWordCloudSearch(
        @RequestBody @Valid ThesisSearchRequestDTO searchRequest, @PathVariable String queryType,
        @RequestParam boolean foreignLanguage) {
        SearchRequestType searchType = switch (queryType) {
            case "simple" -> SearchRequestType.SIMPLE;
            case "advanced" -> SearchRequestType.ADVANCED;
            default -> throw new MissingDataException("Missing valid query type in path request.");
        };

        return thesisSearchService.performWordCloudSearch(searchRequest, searchType,
            foreignLanguage);
    }
}
