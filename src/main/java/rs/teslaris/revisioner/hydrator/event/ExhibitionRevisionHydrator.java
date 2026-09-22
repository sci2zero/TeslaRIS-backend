package rs.teslaris.revisioner.hydrator.event;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import rs.teslaris.core.dto.document.ExhibitionDTO;
import rs.teslaris.core.indexmodel.EntityType;
import rs.teslaris.core.service.interfaces.commontypes.CountryService;
import rs.teslaris.revisioner.hydrator.RevisionHydrator;

@Component
public class ExhibitionRevisionHydrator extends RevisionHydrator<ExhibitionDTO> {

    @Autowired
    public ExhibitionRevisionHydrator(CountryService countryService) {
        super(countryService);
    }

    @Override
    public String entityType() {
        return EntityType.EXHIBITION.name();
    }

    @Override
    public void hydrate(ExhibitionDTO dto) {
        // Nothing to hydrate
    }
}
