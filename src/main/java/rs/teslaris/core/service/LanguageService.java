package rs.teslaris.core.service;

import org.springframework.stereotype.Service;
import rs.teslaris.core.model.Language;

@Service
public interface LanguageService {

    Language findLanguageById(Integer id);
}
