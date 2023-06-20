package rs.teslaris.core.service;

import org.springframework.stereotype.Service;
import rs.teslaris.core.model.commontypes.Language;

@Service
public interface LanguageService extends CRUDService {

    Language findLanguageById(Integer id);
}
