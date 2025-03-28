package rs.teslaris.thesislibrary.controller;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import rs.teslaris.core.indexmodel.DocumentPublicationIndex;
import rs.teslaris.thesislibrary.dto.ThesisSearchRequestDTO;
import rs.teslaris.thesislibrary.service.interfaces.ThesisSearchService;

@RestController
@RequestMapping("/api/thesis-library/search")
@RequiredArgsConstructor
public class ThesisLibrarySearchController {

    private final ThesisSearchService thesisSearchService;

    @PostMapping("/simple")
    public Page<DocumentPublicationIndex> performSimpleSearch(
        @RequestBody @Valid ThesisSearchRequestDTO searchRequest, Pageable pageable) {
        return thesisSearchService.performSimpleThesisSearch(searchRequest, pageable);
    }

    @PostMapping("/advanced")
    public Page<DocumentPublicationIndex> performAdvancedSearch(
        @RequestBody @Valid ThesisSearchRequestDTO searchRequest, Pageable pageable) {
        return thesisSearchService.performSimpleThesisSearch(searchRequest, pageable);
    }
}
