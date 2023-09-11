package rs.teslaris.core.controller;

import javax.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;
import rs.teslaris.core.annotation.Idempotent;
import rs.teslaris.core.dto.document.BookSeriesDTO;
import rs.teslaris.core.dto.document.BookSeriesResponseDTO;
import rs.teslaris.core.service.interfaces.document.BookSeriesService;

@RestController
@RequestMapping("/api/book-series")
@RequiredArgsConstructor
public class BookSeriesController {

    private final BookSeriesService bookSeriesService;

    @GetMapping
    public Page<BookSeriesResponseDTO> readAll(Pageable pageable) {
        return bookSeriesService.readAllBookSeries(pageable);
    }

    @GetMapping("/{bookSeriesId}")
    public BookSeriesResponseDTO readBookSeries(@PathVariable Integer bookSeriesId) {
        return bookSeriesService.readBookSeries(bookSeriesId);
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    @PreAuthorize("hasAuthority('EDIT_PUBLICATION_SERIES')")
    @Idempotent
    public BookSeriesDTO createBookSeries(@RequestBody @Valid BookSeriesDTO bookSeriesDTO) {
        var createdBookSeries = bookSeriesService.createBookSeries(bookSeriesDTO);
        bookSeriesDTO.setId(createdBookSeries.getId());
        return bookSeriesDTO;
    }

    @PutMapping("/{bookSeriesId}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    @PreAuthorize("hasAuthority('EDIT_PUBLICATION_SERIES')")
    public void updateBookSeries(@RequestBody @Valid BookSeriesDTO bookSeriesDTO,
                                 @PathVariable Integer bookSeriesId) {
        bookSeriesService.updateBookSeries(bookSeriesDTO, bookSeriesId);
    }

    @DeleteMapping("/{bookSeriesId}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    @PreAuthorize("hasAuthority('EDIT_PUBLICATION_SERIES')")
    public void deleteBookSeries(@PathVariable Integer bookSeriesId) {
        bookSeriesService.deleteBookSeries(bookSeriesId);
    }
}
