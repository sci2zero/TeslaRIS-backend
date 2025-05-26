package rs.teslaris.core.controller;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.MediaType;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import rs.teslaris.core.annotation.Traceable;
import rs.teslaris.core.dto.document.DocumentFileDTO;
import rs.teslaris.core.dto.document.DocumentFileResponseDTO;
import rs.teslaris.core.indexmodel.DocumentFileIndex;
import rs.teslaris.core.service.interfaces.document.DocumentFileService;
import rs.teslaris.core.util.search.SearchRequestType;
import rs.teslaris.core.util.search.StringUtil;

@RestController
@RequestMapping("/api/document-file")
@RequiredArgsConstructor
@Traceable
public class DocumentFileController {

    private final DocumentFileService documentFileService;


    @GetMapping("/simple-search")
    Page<DocumentFileIndex> searchDocumentFilesSimple(
        @RequestParam("tokens")
        @NotNull(message = "You have to provide a valid search input.") List<String> tokens,
        Pageable pageable) {
        StringUtil.sanitizeTokens(tokens);
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

    @PatchMapping(consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    @PreAuthorize("hasAuthority('EDIT_DOCUMENT_FILES')")
    DocumentFileResponseDTO editDocumentFile(@ModelAttribute @Valid DocumentFileDTO documentFile,
                                             @RequestParam(required = false, defaultValue = "false")
                                             Boolean isProof,
                                             @RequestParam(required = false) Integer documentId) {
        return documentFileService.editDocumentFile(documentFile, !isProof, documentId);
    }
}
