package rs.teslaris.core.service.impl.commontypes;

import lombok.RequiredArgsConstructor;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Service;
import rs.teslaris.core.annotation.Traceable;
import rs.teslaris.core.model.commontypes.Language;
import rs.teslaris.core.repository.commontypes.LanguageRepository;
import rs.teslaris.core.service.impl.JPAServiceImpl;
import rs.teslaris.core.service.interfaces.commontypes.LanguageService;
import rs.teslaris.core.util.exceptionhandling.exception.NotFoundException;

@Service
@RequiredArgsConstructor
@Traceable
public class LanguageServiceImpl extends JPAServiceImpl<Language> implements LanguageService {

    private final LanguageRepository languageRepository;

    @Override
    protected JpaRepository getEntityRepository() {
        return languageRepository;
    }

    @Override
    public Language findLanguageById(Integer id) {
        return languageRepository.findById(id)
            .orElseThrow(() -> new NotFoundException("Language with given ID does not exist."));
    }

}
