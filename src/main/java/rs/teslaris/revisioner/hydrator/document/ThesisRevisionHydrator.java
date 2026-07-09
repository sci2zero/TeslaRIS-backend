package rs.teslaris.revisioner.hydrator.document;

import java.util.Objects;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;
import rs.teslaris.core.dto.document.ThesisResponseDTO;
import rs.teslaris.core.indexmodel.DocumentPublicationType;
import rs.teslaris.core.service.interfaces.commontypes.CountryService;
import rs.teslaris.core.service.interfaces.commontypes.LanguageService;
import rs.teslaris.revisioner.hydrator.RevisionHydrator;

@Component
public class ThesisRevisionHydrator extends RevisionHydrator<ThesisResponseDTO> {

    private final LanguageService languageService;


    @Autowired
    public ThesisRevisionHydrator(
        CountryService countryService, LanguageService languageService) {
        super(countryService);
        this.languageService = languageService;
    }

    @Override
    public String entityType() {
        return DocumentPublicationType.THESIS.name();
    }

    @Override
    @Transactional(readOnly = true)
    public void hydrate(ThesisResponseDTO dto) {
        hydrateCommonFields(dto);

        if (Objects.nonNull(dto.getLanguageId())) {
            dto.setLanguageCode(languageService.findOne(dto.getLanguageId()).getLanguageCode());
        }
    }
}
