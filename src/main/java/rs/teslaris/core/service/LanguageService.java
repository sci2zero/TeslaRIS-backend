package rs.teslaris.core.service;

import org.springframework.stereotype.Service;
import rs.teslaris.core.model.commontypes.Language;

@Service
public interface LanguageService extends JPAService<Language> {

    Language findLanguageById(Integer id);
}
