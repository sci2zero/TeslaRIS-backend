package rs.teslaris.thesislibrary.controller;

import jakarta.validation.Valid;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import rs.teslaris.core.dto.commontypes.MultilingualContentDTO;
import rs.teslaris.core.indexmodel.DocumentPublicationIndex;
import rs.teslaris.core.util.Triple;
import rs.teslaris.thesislibrary.dto.ThesisSearchRequestDTO;
import rs.teslaris.thesislibrary.service.interfaces.ThesisSearchService;

@RestController
@RequestMapping("/api/thesis-library/search")
@RequiredArgsConstructor
public class ThesisLibrarySearchController {

    private final ThesisSearchService thesisSearchService;


    @GetMapping("/fields")
    @PreAuthorize("hasAuthority('PERFORM_THESIS_SEARCH')")
    public List<Triple<String, List<MultilingualContentDTO>, String>> getSearchFields(
        @RequestParam("export") Boolean onlyExportFields) {
        return thesisSearchService.getSearchFields(onlyExportFields);
    }

    @PostMapping("/simple")
    @PreAuthorize("hasAuthority('PERFORM_THESIS_SEARCH')")
    public Page<DocumentPublicationIndex> performSimpleSearch(
        @RequestBody @Valid ThesisSearchRequestDTO searchRequest, Pageable pageable) {
        return thesisSearchService.performSimpleThesisSearch(searchRequest, pageable);
    }

    @PostMapping("/advanced")
    @PreAuthorize("hasAuthority('PERFORM_THESIS_SEARCH')")
    public Page<DocumentPublicationIndex> performAdvancedSearch(
        @RequestBody @Valid ThesisSearchRequestDTO searchRequest, Pageable pageable) {
        return thesisSearchService.performAdvancedThesisSearch(searchRequest, pageable);
    }
}
