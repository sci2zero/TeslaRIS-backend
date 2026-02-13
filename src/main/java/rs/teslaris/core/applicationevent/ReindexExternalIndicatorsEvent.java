package rs.teslaris.core.applicationevent;

import rs.teslaris.core.indexmodel.DocumentPublicationIndex;
import rs.teslaris.core.indexmodel.OrganisationUnitIndex;
import rs.teslaris.core.indexmodel.PersonIndex;

public record ReindexExternalIndicatorsEvent(
    PersonIndex personIndex,

    OrganisationUnitIndex organisationUnitIndex,

    DocumentPublicationIndex documentPublicationIndex
) {
}
