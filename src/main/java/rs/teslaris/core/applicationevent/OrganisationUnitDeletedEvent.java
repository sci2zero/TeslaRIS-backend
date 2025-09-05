package rs.teslaris.core.applicationevent;

import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
public class OrganisationUnitDeletedEvent {

    private Integer organisationUnitId;
}
