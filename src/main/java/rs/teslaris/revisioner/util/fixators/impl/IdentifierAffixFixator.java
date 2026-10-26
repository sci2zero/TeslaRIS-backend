package rs.teslaris.revisioner.util.fixators.impl;

import org.springframework.stereotype.Component;
import rs.teslaris.core.dto.document.DocumentDTO;
import rs.teslaris.core.dto.person.PersonSnapshotDTO;
import rs.teslaris.core.revision.DataQualityFixator;

@Component
public class IdentifierAffixFixator implements DataQualityFixator {

    @Override
    public String key() {
        return "identifierAffix";
    }

    @Override
    public String name() {
        return "Identifier prefix and suffix removal";
    }

    @Override
    public void applyFix(DocumentDTO document) {
        // TODO: normalise doi, scopusId, uris and openAlexId.
    }

    @Override
    public void applyFix(PersonSnapshotDTO person) {
        // TODO: normalise orcid, scopusAuthorId and the remaining person identifiers.
    }
}
