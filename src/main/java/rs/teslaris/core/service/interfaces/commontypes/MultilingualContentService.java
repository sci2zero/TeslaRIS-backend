package rs.teslaris.core.service.interfaces.commontypes;

import java.util.List;
import java.util.Set;
import org.springframework.stereotype.Service;
import rs.teslaris.core.dto.commontypes.MultilingualContentDTO;
import rs.teslaris.core.model.commontypes.MultiLingualContent;

@Service
public interface MultilingualContentService {

    Set<MultiLingualContent> getMultilingualContent(
        List<MultilingualContentDTO> multilingualContent);

    Set<MultiLingualContent> getMultilingualContentAndSetDefaultsIfNonExistent(
        List<MultilingualContentDTO> multilingualContentDTOs);

    Set<MultiLingualContent> deepCopy(Set<MultiLingualContent> content);

    void buildLanguageStrings(StringBuilder serbianBuilder,
                              StringBuilder otherLanguagesBuilder,
                              Set<MultiLingualContent> contentList,
                              boolean popEnglishFirst);

    void buildLanguageStringsFromHTMLMC(StringBuilder serbianBuilder,
                                        StringBuilder otherLanguagesBuilder,
                                        Set<MultiLingualContent> contentList,
                                        boolean popEnglishFirst);
}
