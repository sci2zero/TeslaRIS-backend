package rs.teslaris.core.applicationevent;

import rs.teslaris.core.indexmodel.ExternallyEnrichable;

public record ReindexExternalIndicatorsEvent(
    ExternallyEnrichable index
) {
}
