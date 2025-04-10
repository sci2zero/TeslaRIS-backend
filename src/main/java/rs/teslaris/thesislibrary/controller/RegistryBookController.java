package rs.teslaris.thesislibrary.controller;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
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


    @GetMapping("/can-add/{thesisId}")
    @PreAuthorize("hasAuthority('ADD_TO_REGISTRY_BOOK')")
    public boolean getCanAdd(@PathVariable Integer thesisId) {
        return !registryBookService.hasThesisRegistryBookEntry(thesisId);
    }

    @GetMapping("/{registryBookEntryId}")
    @PreAuthorize("hasAuthority('UPDATE_REGISTRY_BOOK')")
    public RegistryBookEntryDTO getRegistryBookEntriesForPromotion(
        @PathVariable Integer registryBookEntryId) {
        return registryBookService.readRegistryBookEntry(registryBookEntryId);
    }

    @GetMapping("/for-promotion/{promotionId}")
    @PreAuthorize("hasAnyAuthority('UPDATE_REGISTRY_BOOK', 'REMOVE_FROM_PROMOTION')")
    public Page<RegistryBookEntryDTO> getRegistryBookEntriesForPromotion(
        @PathVariable Integer promotionId, Pageable pageable) {
        return registryBookService.getRegistryBookEntriesForPromotion(promotionId, pageable);
    }

    @GetMapping("/non-promoted")
    @PreAuthorize("hasAuthority('ADD_TO_PROMOTION')")
    public Page<RegistryBookEntryDTO> getNonPromotedRegistryBookEntries(Pageable pageable) {
        return registryBookService.getNonPromotedRegistryBookEntries(pageable);
    }

    @PostMapping("/{thesisId}")
    @PreAuthorize("hasAuthority('ADD_TO_REGISTRY_BOOK')")
    @ResponseStatus(HttpStatus.CREATED)
    @Idempotent
    public RegistryBookEntryDTO createRegistryBookEntry(
        @RequestBody @Valid RegistryBookEntryDTO registryBookEntryDTO,
        @PathVariable Integer thesisId) {
        var newRegistryBookEntry =
            registryBookService.createRegistryBookEntry(registryBookEntryDTO, thesisId);
        registryBookEntryDTO.setId(newRegistryBookEntry.getId());

        return registryBookEntryDTO;
    }

    @PutMapping("/{registryBookEntryId}")
    @PreAuthorize("hasAuthority('UPDATE_REGISTRY_BOOK')")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void updateRegistryBookEntry(@PathVariable Integer registryBookEntryId,
                                        @RequestBody
                                        @Valid RegistryBookEntryDTO registryBookEntryDTO) {
        registryBookService.updateRegistryBookEntry(registryBookEntryId, registryBookEntryDTO);
    }

    @DeleteMapping("/{registryBookEntryId}")
    @PreAuthorize("hasAuthority('REMOVE_FROM_REGISTRY_BOOK')")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void deleteRegistryBookEntry(@PathVariable Integer registryBookEntryId) {
        registryBookService.deleteRegistryBookEntry(registryBookEntryId);
    }

    @GetMapping("/pre-populate/{thesisId}")
    @PreAuthorize("hasAuthority('ADD_TO_REGISTRY_BOOK')")
    public PhdThesisPrePopulatedDataDTO getEntryPrePopulatedData(@PathVariable Integer thesisId) {
        return registryBookService.getPrePopulatedPHDThesisInformation(thesisId);
    }

    @PatchMapping("/add/{registryBookEntryId}/{promotionId}")
    @PreAuthorize("hasAuthority('ADD_TO_PROMOTION')")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void addRegistryBookEntryForPromotion(@PathVariable Integer registryBookEntryId,
                                                 @PathVariable Integer promotionId) {
        registryBookService.addToPromotion(registryBookEntryId, promotionId);
    }

    @PatchMapping("/remove/{registryBookEntryId}")
    @PreAuthorize("hasAuthority('REMOVE_FROM_PROMOTION')")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void removeRegistryBookEntryFromPromotion(@PathVariable Integer registryBookEntryId) {
        registryBookService.removeFromPromotion(registryBookEntryId);
    }

    @PatchMapping("/remove-attendance/{attendanceId}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void removeRegistryBookEntryFromPromotion(@PathVariable String attendanceId) {
        registryBookService.removeFromPromotion(attendanceId);
    }

    @PatchMapping("/promote-all/{promotionId}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void promoteAllFromPromotion(@PathVariable Integer promotionId) {
        registryBookService.promoteAll(promotionId);
    }
}
