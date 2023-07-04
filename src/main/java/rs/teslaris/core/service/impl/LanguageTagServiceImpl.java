package rs.teslaris.core.service.impl;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import rs.teslaris.core.exception.NotFoundException;
import rs.teslaris.core.model.commontypes.LanguageTag;
import rs.teslaris.core.repository.JPASoftDeleteRepository;
import rs.teslaris.core.repository.commontypes.LanguageTagRepository;
import rs.teslaris.core.service.LanguageTagService;

@Service
@RequiredArgsConstructor
public class LanguageTagServiceImpl extends JPAServiceImpl<LanguageTag> implements LanguageTagService {

    private final LanguageTagRepository languageTagRepository;

    @Override
    protected JPASoftDeleteRepository<LanguageTag> getEntityRepository() {
        return languageTagRepository;
    }

    @Override
    @Deprecated(forRemoval = true)
    public LanguageTag findLanguageTagById(Integer languageTagId) {
        return languageTagRepository.findByIdAndDeletedIsFalse(languageTagId)
            .orElseThrow(() -> new NotFoundException("Language tag with given ID does not exist."));
    }
}
