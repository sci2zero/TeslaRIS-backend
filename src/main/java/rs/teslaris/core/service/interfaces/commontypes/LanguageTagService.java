package rs.teslaris.core.service.interfaces.commontypes;

import org.springframework.stereotype.Service;
import rs.teslaris.core.model.commontypes.LanguageTag;
import rs.teslaris.core.service.interfaces.JPAService;

@Service
public interface LanguageTagService extends JPAService<LanguageTag> {

    LanguageTag findLanguageTagById(Integer languageTagId);
}
