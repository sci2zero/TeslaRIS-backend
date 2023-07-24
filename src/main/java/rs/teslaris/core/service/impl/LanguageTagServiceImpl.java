package rs.teslaris.core.service.impl;

import lombok.RequiredArgsConstructor;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Service;
import rs.teslaris.core.exception.NotFoundException;
import rs.teslaris.core.model.commontypes.LanguageTag;
import rs.teslaris.core.repository.commontypes.LanguageTagRepository;
import rs.teslaris.core.service.LanguageTagService;

@Service
@RequiredArgsConstructor
public class LanguageTagServiceImpl extends JPAServiceImpl<LanguageTag>
    implements LanguageTagService {

    private final LanguageTagRepository languageTagRepository;

    @Override
    protected JpaRepository<LanguageTag, Integer> getEntityRepository() {
        return languageTagRepository;
    }

    @Override
    @Deprecated(forRemoval = true)
    public LanguageTag findLanguageTagById(Integer languageTagId) {
        return languageTagRepository.findById(languageTagId)
            .orElseThrow(() -> new NotFoundException("Language tag with given ID does not exist."));
    }
}
