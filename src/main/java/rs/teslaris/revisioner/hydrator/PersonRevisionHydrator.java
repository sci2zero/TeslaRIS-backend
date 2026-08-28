package rs.teslaris.revisioner.hydrator;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import rs.teslaris.core.dto.person.PersonSnapshotDTO;
import rs.teslaris.core.indexmodel.EntityType;
import rs.teslaris.core.service.interfaces.commontypes.CountryService;

@Component
public class PersonRevisionHydrator extends RevisionHydrator<PersonSnapshotDTO> {

    @Autowired
    public PersonRevisionHydrator(CountryService countryService) {
        super(countryService);
    }

    @Override
    public String entityType() {
        return EntityType.PERSON.name();
    }

    @Override
    public void hydrate(PersonSnapshotDTO dto) {
        // Nothing to hydrate
    }
}
