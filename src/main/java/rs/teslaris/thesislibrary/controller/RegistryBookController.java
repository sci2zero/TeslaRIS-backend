package rs.teslaris.thesislibrary.controller;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;
import rs.teslaris.core.annotation.Idempotent;
import rs.teslaris.thesislibrary.dto.PhdThesisPrePopulatedDataDTO;
import rs.teslaris.thesislibrary.dto.RegistryBookEntryDTO;
import rs.teslaris.thesislibrary.service.interfaces.RegistryBookService;

@RestController
@RequestMapping("/api/registry-book")
@RequiredArgsConstructor
public class RegistryBookController {

    private final RegistryBookService registryBookService;


    @GetMapping("/for-promotion/{promotionId}")
    public Page<RegistryBookEntryDTO> getRegistryBookEntriesForPromotion(
        @PathVariable Integer promotionId, Pageable pageable) {
        return registryBookService.getRegistryBookEntriesForPromotion(promotionId, pageable);
    }


    @GetMapping("/non-promoted")
    public Page<RegistryBookEntryDTO> getNonPromotedRegistryBookEntries(Pageable pageable) {
        return registryBookService.getNonPromotedRegistryBookEntries(pageable);
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    @Idempotent
    public RegistryBookEntryDTO createRegistryBookEntry(
        @RequestBody @Valid RegistryBookEntryDTO registryBookEntryDTO) {
        var newRegistryBookEntry =
            registryBookService.createRegistryBookEntry(registryBookEntryDTO);
        registryBookEntryDTO.setId(newRegistryBookEntry.getId());

        return registryBookEntryDTO;
    }

    @PutMapping("/{registryBookEntryId}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void updateRegistryBookEntry(@PathVariable Integer registryBookEntryId,
                                        @RequestBody
                                        @Valid RegistryBookEntryDTO registryBookEntryDTO) {
        registryBookService.updateRegistryBookEntry(registryBookEntryId, registryBookEntryDTO);
    }

    @DeleteMapping("/{registryBookEntryId}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void deleteRegistryBookEntry(@PathVariable Integer registryBookEntryId) {
        registryBookService.deleteRegistryBookEntry(registryBookEntryId);
    }

    @GetMapping("/pre-populate/{thesisId}")
    public PhdThesisPrePopulatedDataDTO getEntryPrePopulatedData(@PathVariable Integer thesisId) {
        return registryBookService.getPrePopulatedPHDThesisInformation(thesisId);
    }

    @PatchMapping("/add/{registryBookEntryId}/{promotionId}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void addRegistryBookEntryForPromotion(@PathVariable Integer registryBookEntryId,
                                                 @PathVariable Integer promotionId) {
        registryBookService.addToPromotion(registryBookEntryId, promotionId);
    }

    @PatchMapping("/remove/{registryBookEntryId}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void removeRegistryBookEntryFromPromotion(@PathVariable Integer registryBookEntryId) {
        registryBookService.removeFromPromotion(registryBookEntryId);
    }
}
