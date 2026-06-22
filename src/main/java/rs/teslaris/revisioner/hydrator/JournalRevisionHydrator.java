package rs.teslaris.revisioner.hydrator;

import java.util.ArrayList;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;
import rs.teslaris.core.dto.document.JournalResponseDTO;
import rs.teslaris.core.indexmodel.EntityType;
import rs.teslaris.core.service.interfaces.commontypes.CountryService;
import rs.teslaris.core.service.interfaces.commontypes.LanguageService;

@Component
public class JournalRevisionHydrator extends RevisionHydrator<JournalResponseDTO> {

    private final LanguageService languageService;


    @Autowired
    public JournalRevisionHydrator(
        CountryService countryService, LanguageService languageService) {
        super(countryService);
        this.languageService = languageService;
    }

    @Override
    public String entityType() {
        return EntityType.JOURNAL.name();
    }

    @Override
    @Transactional(readOnly = true)
    public void hydrate(JournalResponseDTO dto) {
        dto.setLanguageTagNames(new ArrayList<>());
        dto.getLanguageIds()
            .forEach(languageId -> dto.getLanguageTagNames().add(
                languageService.findOne(languageId).getLanguageCode()));
    }
}
