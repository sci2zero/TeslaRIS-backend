package rs.teslaris.core.service.impl.commontypes;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import rs.teslaris.core.util.exceptionhandling.exception.NotFoundException;
import rs.teslaris.core.model.commontypes.Language;
import rs.teslaris.core.repository.commontypes.LanguageRepository;
import rs.teslaris.core.service.interfaces.commontypes.LanguageService;

@Service
@RequiredArgsConstructor
public class LanguageServiceImpl implements LanguageService {

    private final LanguageRepository languageRepository;

    @Override
    public Language findLanguageById(Integer id) {
        return languageRepository.findById(id)
            .orElseThrow(() -> new NotFoundException("Language with given ID does not exist."));
    }
}
