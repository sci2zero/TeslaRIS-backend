package rs.teslaris.core.controller;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;
import rs.teslaris.core.annotation.Idempotent;
import rs.teslaris.core.annotation.PublicationEditCheck;
import rs.teslaris.core.dto.document.DocumentFileDTO;
import rs.teslaris.core.dto.document.DocumentFileResponseDTO;
import rs.teslaris.core.dto.document.ThesisDTO;
import rs.teslaris.core.dto.document.ThesisResponseDTO;
import rs.teslaris.core.model.document.ThesisAttachmentType;
import rs.teslaris.core.service.interfaces.document.ThesisService;

@RestController
@RequestMapping("/api/thesis")
@RequiredArgsConstructor
public class ThesisController {

    private final ThesisService thesisService;

    @GetMapping("/{documentId}")
    public ThesisResponseDTO readThesis(
        @PathVariable Integer documentId) {
        return thesisService.readThesisById(documentId);
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    @PublicationEditCheck("CREATE")
    @Idempotent
    public ThesisDTO createThesis(@RequestBody @Valid ThesisDTO thesis) {
        var savedThesis = thesisService.createThesis(thesis, true);
        thesis.setId(savedThesis.getId());
        return thesis;
    }

    @PutMapping("/{documentId}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    @PublicationEditCheck
    public void editThesis(@PathVariable Integer documentId,
                           @RequestBody @Valid ThesisDTO thesis) {
        thesisService.editThesis(documentId, thesis);
    }

    @DeleteMapping("/{documentId}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    @PublicationEditCheck
    public void deleteThesis(@PathVariable Integer documentId) {
        thesisService.deleteThesis(documentId);
    }

    @PatchMapping("/{attachmentType}/{documentId}")
    @PreAuthorize("hasAuthority('MANAGE_THESIS_ATTACHMENTS')")
    @PublicationEditCheck
    @Idempotent
    public DocumentFileResponseDTO addThesisAttachment(@PathVariable
                                                       ThesisAttachmentType attachmentType,
                                                       @PathVariable Integer documentId,
                                                       @ModelAttribute
                                                       @Valid DocumentFileDTO file) {
        return thesisService.addThesisAttachment(documentId, file, attachmentType);
    }

    @PatchMapping("/put-on-public-review/{documentId}")
    @PreAuthorize("hasAuthority('PUT_THESIS_ON_PUBLIC_REVIEW')")
    @PublicationEditCheck
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void putOnPublicReview(@PathVariable Integer documentId) {
        thesisService.putOnPublicReview(documentId);
    }

    @PatchMapping("/remove-from-public-review/{documentId}")
    @PreAuthorize("hasAuthority('REMOVE_THESIS_FROM_PUBLIC_REVIEW')")
    @PublicationEditCheck
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void removeFromPublicReview(@PathVariable Integer documentId) {
        thesisService.removeFromPublicReview(documentId);
    }

    @DeleteMapping("/{attachmentType}/{documentId}/{documentFileId}")
    @PreAuthorize("hasAuthority('DELETE_THESIS_ATTACHMENTS')")
    @PublicationEditCheck
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void deleteThesisAttachment(@PathVariable
                                       ThesisAttachmentType attachmentType,
                                       @PathVariable Integer documentId,
                                       @PathVariable Integer documentFileId) {
        thesisService.deleteThesisAttachment(documentId, documentFileId, attachmentType);
    }
}
