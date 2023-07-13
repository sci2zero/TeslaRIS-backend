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
import rs.teslaris.core.dto.document.PublisherDTO;
import rs.teslaris.core.service.interfaces.document.PublisherService;

@RestController
@RequestMapping("/api/publisher")
@RequiredArgsConstructor
public class PublisherController {

    private final PublisherService publisherService;

    @GetMapping
    public Page<PublisherDTO> readAll(Pageable pageable) {
        return publisherService.readAllPublishers(pageable);
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    @PreAuthorize("hasAuthority('EDIT_PUBLISHERS')")
    @Idempotent
    public PublisherDTO createPublisher(@RequestBody @Valid PublisherDTO publisherDTO) {
        var newPublisher = publisherService.createPublisher(publisherDTO);
        publisherDTO.setId(newPublisher.getId());
        return publisherDTO;
    }

    @PutMapping("/{publisherId}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    @PreAuthorize("hasAuthority('EDIT_PUBLISHERS')")
    public void updatePublisher(@PathVariable Integer publisherId,
                                @RequestBody @Valid PublisherDTO publisherDTO) {
        publisherService.updatePublisher(publisherDTO, publisherId);
    }

    @DeleteMapping("/{publisherId}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    @PreAuthorize("hasAuthority('EDIT_PUBLISHERS')")
    public void deletePublisher(@PathVariable Integer publisherId) {
        publisherService.deletePublisher(publisherId);
    }
}
