package rs.teslaris.core.controller;

import java.util.List;
import javax.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;
import rs.teslaris.core.dto.document.DocumentFileDTO;
import rs.teslaris.core.service.DocumentPublicationService;

@RestController
@RequestMapping("/api/document")
@RequiredArgsConstructor
public class DocumentPublicationController {

    private final DocumentPublicationService documentPublicationService;


    @PatchMapping("/{documentId}/approval")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    void updateDocumentApprovalStatus(@PathVariable Integer documentId,
                                      @RequestParam Boolean isApproved) {
        documentPublicationService.updateDocumentApprovalStatus(documentId, isApproved);
    }

    @PatchMapping("/{documentId}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    void addDocumentFile(@PathVariable Integer documentId,
                         @ModelAttribute @Valid DocumentFileDTO documentFile,
                         @RequestParam Boolean isProof) {
        documentPublicationService.addDocumentFile(documentId, List.of(documentFile), isProof);
    }

    @DeleteMapping("/{documentId}/{documentFileId}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    void deleteDocumentFile(@PathVariable Integer documentId, @PathVariable Integer documentFileId,
                            @RequestParam Boolean isProof) {
        documentPublicationService.deleteDocumentFile(documentId, documentFileId, isProof);
    }
}
