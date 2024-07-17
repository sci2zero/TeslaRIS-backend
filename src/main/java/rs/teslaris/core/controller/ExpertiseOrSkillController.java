package rs.teslaris.core.controller;

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
import rs.teslaris.core.dto.document.DocumentFileDTO;
import rs.teslaris.core.dto.document.DocumentFileResponseDTO;
import rs.teslaris.core.dto.person.ExpertiseOrSkillDTO;
import rs.teslaris.core.dto.person.ExpertiseOrSkillResponseDTO;
import rs.teslaris.core.dto.person.involvement.PersonCollectionEntitySwitchListDTO;
import rs.teslaris.core.service.interfaces.person.ExpertiseOrSkillService;

@Validated
@RestController
@RequestMapping("/api/expertise-or-skill")
@RequiredArgsConstructor
public class ExpertiseOrSkillController {

    private final ExpertiseOrSkillService expertiseOrSkillService;

    @PostMapping("/{personId}")
    @PreAuthorize("hasAuthority('EDIT_PERSON_INFORMATION')")
    @PersonEditCheck
    @Idempotent
    public ExpertiseOrSkillResponseDTO addExpertiseOrSkill(
        @RequestBody ExpertiseOrSkillDTO expertiseOrSkill, @PathVariable Integer personId) {
        return expertiseOrSkillService.addExpertiseOrSkill(personId, expertiseOrSkill);
    }

    @PutMapping("/{personId}/{expertiseOrSkillId}")
    @PreAuthorize("hasAuthority('EDIT_PERSON_INFORMATION')")
    @PersonEditCheck
    public ExpertiseOrSkillResponseDTO updateExpertiseOrSkill(
        @RequestBody ExpertiseOrSkillDTO expertiseOrSkill,
        @PathVariable Integer expertiseOrSkillId) {
        return expertiseOrSkillService.updateExpertiseOrSkill(expertiseOrSkillId, expertiseOrSkill);
    }

    @DeleteMapping("/{personId}/{expertiseOrSkillId}")
    @PreAuthorize("hasAuthority('EDIT_PERSON_INFORMATION')")
    @PersonEditCheck
    public void deleteExpertiseOrSkill(@PathVariable Integer expertiseOrSkillId,
                                       @PathVariable Integer personId) {
        expertiseOrSkillService.deleteExpertiseOrSkill(expertiseOrSkillId, personId);
    }

    @PatchMapping(value = "/{personId}/{expertiseOrSkillId}", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    @PreAuthorize("hasAuthority('EDIT_PERSON_INFORMATION')")
    @PersonEditCheck
    @Idempotent
    public DocumentFileResponseDTO addProof(@ModelAttribute @Valid DocumentFileDTO proof,
                                            @PathVariable Integer expertiseOrSkillId) {
        return expertiseOrSkillService.addProof(expertiseOrSkillId, proof);
    }

    @PatchMapping(value = "/{personId}", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    @PreAuthorize("hasAuthority('EDIT_PERSON_INFORMATION')")
    @PersonEditCheck
    @Idempotent
    public DocumentFileResponseDTO updateExpertiseOrSkillProof(
        @ModelAttribute @Valid DocumentFileDTO proof) {
        return expertiseOrSkillService.updateProof(proof);
    }

    @DeleteMapping("/{personId}/{expertiseOrSkillId}/{proofId}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    @PreAuthorize("hasAuthority('EDIT_PERSON_INFORMATION')")
    @PersonEditCheck
    public void deleteExpertiseOrSkillProof(@PathVariable Integer expertiseOrSkillId,
                                            @PathVariable Integer proofId) {
        expertiseOrSkillService.deleteProof(proofId, expertiseOrSkillId);
    }

    @PatchMapping("/merge/person/source/{sourcePersonId}/target/{targetPersonId}")
    @PreAuthorize("hasAuthority('MERGE_PERSON_METADATA')")
    public void switchInvolvementsToOtherPerson(@PathVariable Integer sourcePersonId,
                                                @PathVariable Integer targetPersonId,
                                                @RequestBody
                                                PersonCollectionEntitySwitchListDTO skillSwitchList) {
        expertiseOrSkillService.switchSkills(skillSwitchList.getEntityIds(), sourcePersonId,
            targetPersonId);
    }
}
