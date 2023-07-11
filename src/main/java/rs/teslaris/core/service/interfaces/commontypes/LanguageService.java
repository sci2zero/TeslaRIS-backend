package rs.teslaris.core.service.interfaces.commontypes;

import org.springframework.stereotype.Service;
import rs.teslaris.core.model.commontypes.Language;

@Service
public interface LanguageService {

    Language findLanguageById(Integer id);
}
