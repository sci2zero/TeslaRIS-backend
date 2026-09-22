package rs.teslaris.revisioner.hydrator.document;

import java.util.ArrayList;
import java.util.Objects;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;
import rs.teslaris.core.dto.commontypes.LanguageTagResponseDTO;
import rs.teslaris.core.dto.document.PerformanceRelatedOutputDTO;
import rs.teslaris.core.indexmodel.DocumentPublicationType;
import rs.teslaris.core.service.interfaces.commontypes.CountryService;
import rs.teslaris.core.service.interfaces.commontypes.LanguageTagService;
import rs.teslaris.core.util.search.CollectionOperations;
import rs.teslaris.revisioner.hydrator.RevisionHydrator;

@Component
public class PerformanceRelatedOutputRevisionHydrator
    extends RevisionHydrator<PerformanceRelatedOutputDTO> {

    private final LanguageTagService languageTagService;


    @Autowired
    public PerformanceRelatedOutputRevisionHydrator(
        CountryService countryService, LanguageTagService languageTagService) {
        super(countryService);
        this.languageTagService = languageTagService;
    }

    @Override
    public String entityType() {
        return DocumentPublicationType.PERFORMANCE_RELATED_OUTPUT.name();
    }

    @Override
    @Transactional(readOnly = true)
    public void hydrate(PerformanceRelatedOutputDTO dto) {
        hydrateCommonFields(dto);

        if (CollectionOperations.containsValues(dto.getLanguageTagIds())) {
            if (Objects.isNull(dto.getLanguageTags())) {
                dto.setLanguageTags(new ArrayList<>());
            }

            dto.getLanguageTagIds().forEach(languageTagId -> {
                    var languageTag = languageTagService.findOne(languageTagId);
                    dto.getLanguageTags()
                        .add(new LanguageTagResponseDTO(languageTag.getId(),
                            languageTag.getLanguageTag(), languageTag.getDisplay()));
                }
            );
        }
    }
}
