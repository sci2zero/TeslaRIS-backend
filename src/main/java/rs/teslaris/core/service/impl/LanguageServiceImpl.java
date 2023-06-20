package rs.teslaris.core.service.impl;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import rs.teslaris.core.exception.NotFoundException;
import rs.teslaris.core.model.commontypes.Language;
import rs.teslaris.core.repository.CRUDRepository;
import rs.teslaris.core.repository.commontypes.LanguageRepository;
import rs.teslaris.core.service.LanguageService;

@Service
@RequiredArgsConstructor
public class LanguageServiceImpl extends JPAServiceImpl implements LanguageService {

    private final LanguageRepository languageRepository;


    @Override
    protected CRUDRepository getEntityRepository() {
        return languageRepository;
    }

    @Override
    public Language findLanguageById(Integer id) {
        return languageRepository.findById(id)
            .orElseThrow(() -> new NotFoundException("Language with given ID does not exist."));
    }

}
