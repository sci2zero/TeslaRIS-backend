package rs.teslaris.core.revision;

import java.util.Set;

/**
 * Do not save through the entity service, because that makes a new revision.
 */
public interface PublicationCandidacyUpdater {

    Set<String> entityTypes();

    void apply(String entityType, Integer entityId, boolean publicationCandidate);
}
