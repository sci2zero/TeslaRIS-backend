package rs.teslaris.core.controller;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;
import java.util.List;
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
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;
import rs.teslaris.core.annotation.Idempotent;
import rs.teslaris.core.dto.document.PublisherBasicAdditionDTO;
import rs.teslaris.core.dto.document.PublisherDTO;
import rs.teslaris.core.indexmodel.PublisherIndex;
import rs.teslaris.core.service.interfaces.document.PublisherService;
import rs.teslaris.core.util.search.StringUtil;

@RestController
@RequestMapping("/api/publisher")
@RequiredArgsConstructor
public class PublisherController {

    private final PublisherService publisherService;


    @GetMapping("/{publisherId}/can-edit")
    @PreAuthorize("hasAuthority('EDIT_PUBLISHERS')")
    public boolean canEditPublisher() {
        return true;
    }

    @GetMapping
    public Page<PublisherDTO> readAll(Pageable pageable) {
        return publisherService.readAllPublishers(pageable);
    }

    @GetMapping("/{publisherId}")
    public PublisherDTO readPublisher(@PathVariable Integer publisherId) {
        return publisherService.readPublisherById(publisherId);
    }

    @GetMapping("/simple-search")
    public Page<PublisherIndex> searchPublishers(
        @RequestParam("tokens")
        @NotNull(message = "You have to provide a valid search input.") List<String> tokens,
        Pageable pageable
    ) {
        StringUtil.sanitizeTokens(tokens);
        return publisherService.searchPublishers(tokens, pageable);
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    @PreAuthorize("hasAnyAuthority('CREATE_PUBLISHER', 'EDIT_PUBLISHERS')")
    @Idempotent
    public PublisherDTO createPublisher(@RequestBody @Valid PublisherDTO publisherDTO) {
        var newPublisher = publisherService.createPublisher(publisherDTO, true);
        publisherDTO.setId(newPublisher.getId());
        return publisherDTO;
    }

    @PostMapping("/basic")
    @ResponseStatus(HttpStatus.CREATED)
    @PreAuthorize("hasAuthority('EDIT_PUBLISHERS')")
    @Idempotent
    public PublisherBasicAdditionDTO createPublisher(
        @RequestBody @Valid PublisherBasicAdditionDTO publisherDTO) {
        var newPublisher = publisherService.createPublisher(publisherDTO);
        publisherDTO.setId(newPublisher.getId());
        return publisherDTO;
    }

    @PutMapping("/{publisherId}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    @PreAuthorize("hasAuthority('EDIT_PUBLISHERS')")
    public void updatePublisher(@PathVariable Integer publisherId,
                                @RequestBody @Valid PublisherDTO publisherDTO) {
        publisherService.editPublisher(publisherId, publisherDTO);
    }

    @DeleteMapping("/{publisherId}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    @PreAuthorize("hasAuthority('EDIT_PUBLISHERS')")
    public void deletePublisher(@PathVariable Integer publisherId) {
        publisherService.deletePublisher(publisherId);
    }

    @DeleteMapping("/force/{publisherId}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    @PreAuthorize("hasAuthority('FORCE_DELETE_ENTITIES')")
    public void forceDeletePublisher(@PathVariable Integer publisherId) {
        publisherService.forceDeletePublisher(publisherId);
    }
}
