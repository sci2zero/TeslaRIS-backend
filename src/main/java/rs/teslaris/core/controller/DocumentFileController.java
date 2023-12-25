package rs.teslaris.core.controller;

import java.util.List;
import javax.validation.Valid;
import javax.validation.constraints.NotNull;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;
import rs.teslaris.core.dto.document.DocumentFileDTO;
import rs.teslaris.core.indexmodel.DocumentFileIndex;
import rs.teslaris.core.service.interfaces.document.DocumentFileService;
import rs.teslaris.core.util.search.SearchRequestType;

@RestController
@RequestMapping("/api/document-file")
@RequiredArgsConstructor
public class DocumentFileController {

    private final DocumentFileService documentFileService;


    @GetMapping("/simple-search")
    Page<DocumentFileIndex> searchDocumentFilesSimple(
        @RequestParam("tokens")
        @NotNull(message = "You have to provide a valid search input.") List<String> tokens,
        Pageable pageable) {
        return documentFileService.searchDocumentFiles(tokens, pageable,
            SearchRequestType.SIMPLE);
    }

    @GetMapping("/advanced-search")
    Page<DocumentFileIndex> searchDocumentFilesAdvanced(
        @RequestParam("tokens")
        @NotNull(message = "You have to provide a valid search input.") List<String> tokens,
        Pageable pageable) {
        return documentFileService.searchDocumentFiles(tokens, pageable,
            SearchRequestType.ADVANCED);
    }

    @PutMapping
    @ResponseStatus(HttpStatus.NO_CONTENT)
    @PreAuthorize("hasAuthority('EDIT_DOCUMENT_PROOFS')")
    void editDocumentFile(@RequestBody @Valid DocumentFileDTO documentFile) {
        documentFileService.editDocumentFile(documentFile);
    }
}
