package rs.teslaris.revisioner.hydrator.event;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import rs.teslaris.core.dto.document.ConferenceDTO;
import rs.teslaris.core.indexmodel.EntityType;
import rs.teslaris.core.service.interfaces.commontypes.CountryService;
import rs.teslaris.revisioner.hydrator.RevisionHydrator;

@Component
public class ConferenceRevisionHydrator extends RevisionHydrator<ConferenceDTO> {

    @Autowired
    public ConferenceRevisionHydrator(CountryService countryService) {
        super(countryService);
    }

    @Override
    public String entityType() {
        return EntityType.CONFERENCE.name();
    }

    @Override
    public void hydrate(ConferenceDTO dto) {
        // Nothing to hydrate
    }
}
