package rs.teslaris.thesislibrary.service.interfaces;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import rs.teslaris.core.service.interfaces.JPAService;
import rs.teslaris.thesislibrary.dto.PhdThesisPrePopulatedDataDTO;
import rs.teslaris.thesislibrary.dto.RegistryBookEntryDTO;
import rs.teslaris.thesislibrary.model.RegistryBookEntry;

@Service
public interface RegistryBookService extends JPAService<RegistryBookEntry> {

    Page<RegistryBookEntryDTO> getNonPromotedRegistryBookEntries(Pageable pageable);

    Page<RegistryBookEntryDTO> getRegistryBookEntriesForPromotion(Integer promotionId,
                                                                  Pageable pageable);

    RegistryBookEntry createRegistryBookEntry(RegistryBookEntryDTO registryBookEntryDTO);

    void updateRegistryBookEntry(Integer registryBookEntryId,
                                 RegistryBookEntryDTO registryBookEntryDTO);

    void deleteRegistryBookEntry(Integer registryBookEntryId);

    PhdThesisPrePopulatedDataDTO getPrePopulatedPHDThesisInformation(Integer thesisId);

    void addToPromotion(Integer registryBookEntryId, Integer promotionId);

    void removeFromPromotion(Integer registryBookEntryId);
}
