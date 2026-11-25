package rs.teslaris.core.controller.person;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.DeleteMapping;
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
import rs.teslaris.core.annotation.PersonEditCheck;
import rs.teslaris.core.annotation.Traceable;
import rs.teslaris.core.dto.document.DocumentFileDTO;
import rs.teslaris.core.dto.document.DocumentFileResponseDTO;
import rs.teslaris.core.dto.person.LanguageKnowledgeRequestDTO;
import rs.teslaris.core.dto.person.LanguageKnowledgeResponseDTO;
import rs.teslaris.core.service.interfaces.person.LanguageKnowledgeService;

@Validated
@RestController
@RequestMapping("/api/language-knowledge")
@RequiredArgsConstructor
@Traceable
public class LanguageKnowledgeController {

    private final LanguageKnowledgeService languageKnowledgeService;

    @PostMapping("/{personId}")
    @PreAuthorize("hasAuthority('EDIT_PERSON_INFORMATION')")
    @PersonEditCheck
    @Idempotent
    public LanguageKnowledgeResponseDTO addLanguageKnowledge(
        @RequestBody @Valid LanguageKnowledgeRequestDTO languageKnowledge,
        @PathVariable Integer personId) {
        return languageKnowledgeService.addLanguageKnowledge(personId, languageKnowledge);
    }

    @PutMapping("/{personId}/{languageKnowledgeId}")
    @PreAuthorize("hasAuthority('EDIT_PERSON_INFORMATION')")
    @PersonEditCheck
    public LanguageKnowledgeResponseDTO updateLanguageKnowledge(
        @RequestBody @Valid LanguageKnowledgeRequestDTO languageKnowledge,
        @PathVariable Integer languageKnowledgeId) {
        return languageKnowledgeService.updateLanguageKnowledge(languageKnowledgeId,
            languageKnowledge);
    }

    @DeleteMapping("/{personId}/{languageKnowledgeId}")
    @PreAuthorize("hasAuthority('EDIT_PERSON_INFORMATION')")
    @PersonEditCheck
    public void deleteLanguageKnowledge(@PathVariable Integer languageKnowledgeId,
                                        @PathVariable Integer personId) {
        languageKnowledgeService.deleteLanguageKnowledge(languageKnowledgeId, personId);
    }

    @PatchMapping(value = "/{personId}/{languageKnowledgeId}", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    @PreAuthorize("hasAuthority('EDIT_PERSON_INFORMATION')")
    @PersonEditCheck
    @Idempotent
    public DocumentFileResponseDTO addProof(@ModelAttribute @Valid DocumentFileDTO proof,
                                            @PathVariable Integer languageKnowledgeId) {
        return languageKnowledgeService.addProof(languageKnowledgeId, proof);
    }

    @PatchMapping(value = "/{personId}", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    @PreAuthorize("hasAuthority('EDIT_PERSON_INFORMATION')")
    @PersonEditCheck
    @Idempotent
    public DocumentFileResponseDTO updateLanguageKnowledgeProof(
        @ModelAttribute @Valid DocumentFileDTO proof) {
        return languageKnowledgeService.updateProof(proof);
    }

    @DeleteMapping("/{personId}/{languageKnowledgeId}/{proofId}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    @PreAuthorize("hasAuthority('EDIT_PERSON_INFORMATION')")
    @PersonEditCheck
    public void deleteLanguageKnowledgeProof(@PathVariable Integer languageKnowledgeId,
                                             @PathVariable Integer proofId) {
        languageKnowledgeService.deleteProof(proofId, languageKnowledgeId);
    }
}
