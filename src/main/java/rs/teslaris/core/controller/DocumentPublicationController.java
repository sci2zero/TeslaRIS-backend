package rs.teslaris.core.controller;

import java.util.List;
import javax.validation.Valid;
import javax.validation.constraints.NotNull;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;
import rs.teslaris.core.annotation.Idempotent;
import rs.teslaris.core.annotation.PublicationEditCheck;
import rs.teslaris.core.dto.document.DocumentFileDTO;
import rs.teslaris.core.indexmodel.DocumentPublicationIndex;
import rs.teslaris.core.service.interfaces.document.DocumentPublicationService;
import rs.teslaris.core.util.search.SearchRequestType;

@RestController
@RequestMapping("/api/document")
@RequiredArgsConstructor
public class DocumentPublicationController {

    private final DocumentPublicationService documentPublicationService;


    @GetMapping("/simple-search")
    public Page<DocumentPublicationIndex> simpleSearch(
        @RequestParam("tokens")
        @NotNull(message = "You have to provide a valid search input.") List<String> tokens,
        Pageable pageable) {
        return documentPublicationService.searchDocumentPublications(tokens, pageable,
            SearchRequestType.SIMPLE);
    }

    @GetMapping("/advanced-search")
    public Page<DocumentPublicationIndex> advancedSearch(
        @RequestParam("tokens")
        @NotNull(message = "You have to provide a valid search input.") List<String> tokens,
        Pageable pageable) {
        return documentPublicationService.searchDocumentPublications(tokens, pageable,
            SearchRequestType.ADVANCED);
    }

    @PatchMapping("/{publicationId}/approval")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    @PreAuthorize("hasAuthority('APPROVE_DOCUMENT')")
    void updateDocumentApprovalStatus(@PathVariable Integer publicationId,
                                      @RequestParam Boolean isApproved) {
        documentPublicationService.updateDocumentApprovalStatus(publicationId, isApproved);
    }

    @PatchMapping("/{publicationId}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    @PublicationEditCheck
    @Idempotent
    void addDocumentFile(@PathVariable Integer publicationId,
                         @ModelAttribute @Valid DocumentFileDTO documentFile,
                         @RequestParam Boolean isProof) {
        documentPublicationService.addDocumentFile(publicationId, List.of(documentFile), isProof);
    }

    @DeleteMapping("/{publicationId}/{documentFileId}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    @PublicationEditCheck
    void deleteDocumentFile(@PathVariable Integer publicationId,
                            @PathVariable Integer documentFileId, @RequestParam Boolean isProof) {
        documentPublicationService.deleteDocumentFile(publicationId, documentFileId, isProof);
    }
}
