package rs.teslaris.core.service.interfaces.commontypes;

import org.springframework.stereotype.Service;
import rs.teslaris.core.model.commontypes.LanguageTag;

@Service
public interface LanguageTagService {

    LanguageTag findLanguageTagById(Integer languageTagId);
}
