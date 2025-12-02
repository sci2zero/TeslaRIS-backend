package rs.teslaris.thesislibrary.service.interfaces;

import org.springframework.stereotype.Service;
import rs.teslaris.core.service.interfaces.JPAService;
import rs.teslaris.thesislibrary.dto.RegistryBookEntryDTO;
import rs.teslaris.thesislibrary.model.RegistryBookEntryDraft;

@Service
public interface RegistryBookDraftService extends JPAService<RegistryBookEntryDraft> {

    RegistryBookEntryDTO fetchRegistryBookEntryDraft(Integer thesisId);

    void saveRegistryBookEntryDraft(RegistryBookEntryDTO registryBookEntryDTO, Integer thesisId);

    void deleteDraftsForThesis(Integer thesisId);
}
