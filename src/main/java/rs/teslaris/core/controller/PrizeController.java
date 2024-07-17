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
import rs.teslaris.core.dto.person.PrizeDTO;
import rs.teslaris.core.dto.person.PrizeResponseDTO;
import rs.teslaris.core.dto.person.involvement.PersonCollectionEntitySwitchListDTO;
import rs.teslaris.core.service.interfaces.person.PrizeService;

@Validated
@RestController
@RequestMapping("/api/prize")
@RequiredArgsConstructor
public class PrizeController {

    private final PrizeService prizeService;

    @PostMapping("/{personId}")
    @PreAuthorize("hasAuthority('EDIT_PERSON_INFORMATION')")
    @PersonEditCheck
    @Idempotent
    public PrizeResponseDTO addPrize(
        @RequestBody PrizeDTO prize, @PathVariable Integer personId) {
        return prizeService.addPrize(personId, prize);
    }

    @PutMapping("/{personId}/{prizeId}")
    @PreAuthorize("hasAuthority('EDIT_PERSON_INFORMATION')")
    @PersonEditCheck
    public PrizeResponseDTO updatePrize(
        @RequestBody PrizeDTO prize,
        @PathVariable Integer prizeId) {
        return prizeService.updatePrize(prizeId, prize);
    }

    @DeleteMapping("/{personId}/{prizeId}")
    @PreAuthorize("hasAuthority('EDIT_PERSON_INFORMATION')")
    @PersonEditCheck
    public void deletePrize(@PathVariable Integer prizeId,
                            @PathVariable Integer personId) {
        prizeService.deletePrize(prizeId, personId);
    }

    @PatchMapping(value = "/{personId}/{prizeId}", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    @PreAuthorize("hasAuthority('EDIT_PERSON_INFORMATION')")
    @PersonEditCheck
    @Idempotent
    public DocumentFileResponseDTO addProof(@ModelAttribute @Valid DocumentFileDTO proof,
                                            @PathVariable Integer prizeId) {
        return prizeService.addProof(prizeId, proof);
    }

    @PatchMapping(value = "/{personId}", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    @PreAuthorize("hasAuthority('EDIT_PERSON_INFORMATION')")
    @PersonEditCheck
    @Idempotent
    public DocumentFileResponseDTO updatePrizeProof(
        @ModelAttribute @Valid DocumentFileDTO proof) {
        return prizeService.updateProof(proof);
    }

    @DeleteMapping("/{personId}/{prizeId}/{proofId}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    @PreAuthorize("hasAuthority('EDIT_PERSON_INFORMATION')")
    @PersonEditCheck
    public void deletePrizeProof(@PathVariable Integer prizeId,
                                 @PathVariable Integer proofId) {
        prizeService.deleteProof(proofId, prizeId);
    }

    @PatchMapping("/merge/person/source/{sourcePersonId}/target/{targetPersonId}")
    @PreAuthorize("hasAuthority('MERGE_PERSON_METADATA')")
    public void switchInvolvementsToOtherPerson(@PathVariable Integer sourcePersonId,
                                                @PathVariable Integer targetPersonId,
                                                @RequestBody
                                                PersonCollectionEntitySwitchListDTO prizeSwitchList) {
        prizeService.switchPrizes(prizeSwitchList.getEntityIds(), sourcePersonId, targetPersonId);
    }
}
