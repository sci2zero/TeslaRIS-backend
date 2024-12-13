package rs.teslaris.core.service.interfaces.commontypes;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import rs.teslaris.core.dto.commontypes.LanguageTagDTO;
import rs.teslaris.core.dto.commontypes.LanguageTagResponseDTO;
import rs.teslaris.core.model.commontypes.LanguageTag;
import rs.teslaris.core.service.interfaces.JPAService;

@Service
public interface LanguageTagService extends JPAService<LanguageTag> {

    Page<LanguageTagResponseDTO> searchLanguageTags(Pageable pageable, String searchExpression);

    LanguageTag findLanguageTagById(Integer languageTagId);

    LanguageTag findLanguageTagByValue(String languageTag);

    LanguageTag createLanguageTag(LanguageTagDTO languageTagDTO);

    void updateLanguageTag(Integer languageTagId, LanguageTagDTO languageTagDTO);

    void deleteLanguageTag(Integer languageTagId);
}
