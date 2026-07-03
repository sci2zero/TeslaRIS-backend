package rs.teslaris.revisioner.hydrator.event;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import rs.teslaris.core.dto.document.OtherEventDTO;
import rs.teslaris.core.indexmodel.EntityType;
import rs.teslaris.core.service.interfaces.commontypes.CountryService;
import rs.teslaris.revisioner.hydrator.RevisionHydrator;

@Component
public class OtherEventRevisionHydrator extends RevisionHydrator<OtherEventDTO> {

    @Autowired
    public OtherEventRevisionHydrator(CountryService countryService) {
        super(countryService);
    }

    @Override
    public String entityType() {
        return EntityType.OTHER_EVENT.name();
    }

    @Override
    public void hydrate(OtherEventDTO dto) {
        // Nothing to hydrate
    }
}
