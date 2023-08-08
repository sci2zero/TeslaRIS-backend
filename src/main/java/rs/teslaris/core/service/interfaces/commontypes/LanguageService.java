package rs.teslaris.core.service.interfaces.commontypes;

import org.springframework.stereotype.Service;
import rs.teslaris.core.model.commontypes.Language;
import rs.teslaris.core.service.JPAService;

@Service
public interface LanguageService extends JPAService<Language> {

    Language findLanguageById(Integer id);
}
