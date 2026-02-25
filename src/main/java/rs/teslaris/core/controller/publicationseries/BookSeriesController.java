package rs.teslaris.core.controller.publicationseries;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;
import rs.teslaris.core.annotation.Idempotent;
import rs.teslaris.core.annotation.Traceable;
import rs.teslaris.core.dto.document.BookSeriesDTO;
import rs.teslaris.core.dto.document.BookSeriesResponseDTO;
import rs.teslaris.core.indexmodel.BookSeriesIndex;
import rs.teslaris.core.indexmodel.DocumentPublicationIndex;
import rs.teslaris.core.indexmodel.EntityType;
import rs.teslaris.core.service.interfaces.document.BookSeriesService;
import rs.teslaris.core.service.interfaces.document.DeduplicationService;
import rs.teslaris.core.util.search.StringUtil;
import rs.teslaris.core.util.signposting.FairSignpostingL1Utility;
import rs.teslaris.core.util.signposting.FairSignpostingL2Utility;
import rs.teslaris.core.util.signposting.LinksetFormat;

@RestController
@RequestMapping("/api/book-series")
@RequiredArgsConstructor
@Traceable
public class BookSeriesController {

    private final BookSeriesService bookSeriesService;

    private final DeduplicationService deduplicationService;


    @GetMapping("/{bookSeriesId}/can-edit")
    @PreAuthorize("hasAuthority('EDIT_PUBLICATION_SERIES')")
    public boolean canEditBookSeries() {
        return true;
    }

    @GetMapping
    public Page<BookSeriesResponseDTO> readAll(Pageable pageable) {
        return bookSeriesService.readAllBookSeries(pageable);
    }

    @GetMapping("/simple-search")
    Page<BookSeriesIndex> searchBookSeries(
        @RequestParam("tokens")
        @NotNull(message = "You have to provide a valid search input.") List<String> tokens,
        Pageable pageable) {
        StringUtil.sanitizeTokens(tokens);
        return bookSeriesService.searchBookSeries(tokens, pageable);
    }

    @GetMapping("/{bookSeriesId}")
    public ResponseEntity<BookSeriesResponseDTO> readBookSeries(
        @PathVariable Integer bookSeriesId) {
        var dto = bookSeriesService.readBookSeries(bookSeriesId);

        var headers = new HttpHeaders();
        FairSignpostingL1Utility.addHeadersForPublicationSeries(headers, dto, "/api/book-series");

        return ResponseEntity.ok()
            .headers(headers)
            .body(dto);
    }

    @GetMapping("/linkset/{bookSeriesId}/{linksetFormat}")
    public ResponseEntity<String> getBookSeriesLinkset(@PathVariable Integer bookSeriesId,
                                                       @PathVariable LinksetFormat linksetFormat) {
        var dto = bookSeriesService.readBookSeries(bookSeriesId);

        var headers = new HttpHeaders();
        headers.add(HttpHeaders.CONTENT_TYPE, linksetFormat.getValue());
        return ResponseEntity.ok()
            .headers(headers)
            .body(FairSignpostingL2Utility.createLinksForPublicationSeries(dto, linksetFormat));
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    @PreAuthorize("hasAuthority('CREATE_BOOK_SERIES')")
    @Idempotent
    public BookSeriesDTO createBookSeries(@RequestBody @Valid BookSeriesDTO bookSeriesDTO) {
        var createdBookSeries = bookSeriesService.createBookSeries(bookSeriesDTO, true);
        bookSeriesDTO.setId(createdBookSeries.getId());
        return bookSeriesDTO;
    }

    @PutMapping("/{bookSeriesId}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    @PreAuthorize("hasAuthority('EDIT_PUBLICATION_SERIES')")
    public void updateBookSeries(@RequestBody @Valid BookSeriesDTO bookSeriesDTO,
                                 @PathVariable Integer bookSeriesId) {
        bookSeriesService.updateBookSeries(bookSeriesId, bookSeriesDTO);
    }

    @DeleteMapping("/{bookSeriesId}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    @PreAuthorize("hasAuthority('EDIT_PUBLICATION_SERIES')")
    public void deleteBookSeries(@PathVariable Integer bookSeriesId) {
        bookSeriesService.deleteBookSeries(bookSeriesId);
        deduplicationService.deleteSuggestion(bookSeriesId, EntityType.BOOK_SERIES);
    }

    @DeleteMapping("/force/{bookSeriesId}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    @PreAuthorize("hasAuthority('FORCE_DELETE_ENTITIES')")
    public void forceDeleteBookSeries(@PathVariable Integer bookSeriesId) {
        bookSeriesService.forceDeleteBookSeries(bookSeriesId);
        deduplicationService.deleteSuggestion(bookSeriesId, EntityType.BOOK_SERIES);
    }

    @GetMapping("/publications/{bookSeriesId}")
    public Page<DocumentPublicationIndex> findProceedingsForBookSeries(
        @PathVariable Integer bookSeriesId, Pageable pageable) {
        return bookSeriesService.findPublicationsForBookSeries(bookSeriesId, pageable);
    }

    @GetMapping("/identifier-usage/{bookSeriesId}")
    @PreAuthorize("hasAuthority('EDIT_PUBLICATION_SERIES')")
    public boolean checkIdentifierUsage(@PathVariable Integer bookSeriesId,
                                        @RequestParam String identifier) {
        return bookSeriesService.isIdentifierInUse(identifier, bookSeriesId);
    }
}
