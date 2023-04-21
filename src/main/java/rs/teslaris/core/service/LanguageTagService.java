package rs.teslaris.core.service;

import org.springframework.stereotype.Service;
import rs.teslaris.core.model.commontypes.LanguageTag;

@Service
public interface LanguageTagService {

    LanguageTag findLanguageTagById(Integer languageTagId);
}
