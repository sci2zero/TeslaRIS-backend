package rs.teslaris.core.service.interfaces.commontypes;

import java.util.List;
import org.springframework.stereotype.Service;
import rs.teslaris.core.dto.commontypes.LanguageResponseDTO;
import rs.teslaris.core.model.commontypes.Language;
import rs.teslaris.core.service.interfaces.JPAService;

@Service
public interface LanguageService extends JPAService<Language> {

    Language findLanguageById(Integer id);

    List<LanguageResponseDTO> getUISupportedLanguages();
}
