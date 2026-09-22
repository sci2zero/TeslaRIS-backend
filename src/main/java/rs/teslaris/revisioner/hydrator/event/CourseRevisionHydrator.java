package rs.teslaris.revisioner.hydrator.event;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import rs.teslaris.core.dto.document.CourseDTO;
import rs.teslaris.core.indexmodel.EntityType;
import rs.teslaris.core.service.interfaces.commontypes.CountryService;
import rs.teslaris.revisioner.hydrator.RevisionHydrator;

@Component
public class CourseRevisionHydrator extends RevisionHydrator<CourseDTO> {

    @Autowired
    public CourseRevisionHydrator(CountryService countryService) {
        super(countryService);
    }

    @Override
    public String entityType() {
        return EntityType.COURSE.name();
    }

    @Override
    public void hydrate(CourseDTO dto) {
        // Nothing to hydrate
    }
}
