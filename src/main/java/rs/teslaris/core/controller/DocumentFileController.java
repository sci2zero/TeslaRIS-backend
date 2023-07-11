package rs.teslaris.core.controller;

import javax.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;
import rs.teslaris.core.dto.document.DocumentFileDTO;
import rs.teslaris.core.service.interfaces.document.DocumentFileService;

@RestController
@RequestMapping("/api/document-file")
@RequiredArgsConstructor
public class DocumentFileController {

    private final DocumentFileService documentFileService;

    @PutMapping
    @ResponseStatus(HttpStatus.NO_CONTENT)
    @PreAuthorize("hasAuthority('EDIT_DOCUMENT_PROOFS')")
    void editDocumentFile(@RequestBody @Valid DocumentFileDTO documentFile) {
        documentFileService.editDocumentFile(documentFile);
    }
}
