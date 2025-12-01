package rs.teslaris.thesislibrary.service.impl;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import jakarta.annotation.Nullable;
import lombok.RequiredArgsConstructor;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import rs.teslaris.core.annotation.Traceable;
import rs.teslaris.core.service.impl.JPAServiceImpl;
import rs.teslaris.core.service.interfaces.document.ThesisService;
import rs.teslaris.core.util.exceptionhandling.exception.LoadingException;
import rs.teslaris.core.util.exceptionhandling.exception.StorageException;
import rs.teslaris.thesislibrary.dto.RegistryBookEntryDTO;
import rs.teslaris.thesislibrary.model.RegistryBookEntryDraft;
import rs.teslaris.thesislibrary.repository.RegistryBookEntryDraftRepository;
import rs.teslaris.thesislibrary.service.interfaces.RegistryBookDraftService;

@Service
@RequiredArgsConstructor
@Traceable
public class RegistryBookDraftServiceImpl extends JPAServiceImpl<RegistryBookEntryDraft>
    implements RegistryBookDraftService {

    private final RegistryBookEntryDraftRepository registryBookEntryDraftRepository;

    private final ThesisService thesisService;

    private final ObjectMapper draftMapper = new ObjectMapper()
        .setSerializationInclusion(JsonInclude.Include.NON_NULL)
        .registerModule(new JavaTimeModule())
        .configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false)
        .configure(DeserializationFeature.FAIL_ON_MISSING_CREATOR_PROPERTIES, false)
        .configure(DeserializationFeature.FAIL_ON_NULL_CREATOR_PROPERTIES, false);


    @Override
    protected JpaRepository<RegistryBookEntryDraft, Integer> getEntityRepository() {
        return registryBookEntryDraftRepository;
    }

    @Override
    @Nullable
    @Transactional(readOnly = true)
    public RegistryBookEntryDTO fetchRegistryBookEntryDraft(Integer thesisId) {
        var optionalDraft = registryBookEntryDraftRepository.findByThesisId(thesisId);

        if (optionalDraft.isEmpty()) {
            return null;
        }

        try {
            return draftMapper.readValue(optionalDraft.get().getDraftData(),
                RegistryBookEntryDTO.class);
        } catch (JsonProcessingException e) {
            throw new LoadingException("failedToParseDraftMessage"); // should never happen
        }
    }

    @Override
    @Transactional
    public void saveRegistryBookEntryDraft(RegistryBookEntryDTO registryBookEntryDTO,
                                           Integer thesisId) {
        String jsonDraft;
        try {
            jsonDraft = draftMapper.writeValueAsString(registryBookEntryDTO);
        } catch (JsonProcessingException e) {
            throw new StorageException("failedToStoreDraftMessage");
        }

        var thesis = thesisService.getThesisById(thesisId);
        registryBookEntryDraftRepository.deleteByThesisId(thesisId);

        var newDraft = new RegistryBookEntryDraft();
        newDraft.setThesis(thesis);
        newDraft.setDraftData(jsonDraft);

        registryBookEntryDraftRepository.save(newDraft);
    }
}
