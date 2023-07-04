package rs.teslaris.core.service.impl;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import rs.teslaris.core.exception.NotFoundException;
import rs.teslaris.core.model.commontypes.Language;
import rs.teslaris.core.repository.JPASoftDeleteRepository;
import rs.teslaris.core.repository.commontypes.LanguageRepository;
import rs.teslaris.core.service.LanguageService;

@Service
@RequiredArgsConstructor
public class LanguageServiceImpl extends JPAServiceImpl<Language> implements LanguageService {

    private final LanguageRepository languageRepository;

    @Override
    protected JPASoftDeleteRepository getEntityRepository() {
        return languageRepository;
    }

    @Override
    @Deprecated(forRemoval = true)
    public Language findLanguageById(Integer id) {
        return languageRepository.findByIdAndDeletedIsFalse(id)
            .orElseThrow(() -> new NotFoundException("Language with given ID does not exist."));
    }

}
