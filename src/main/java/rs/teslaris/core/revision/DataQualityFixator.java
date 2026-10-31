package rs.teslaris.core.revision;

import rs.teslaris.core.dto.document.DocumentDTO;
import rs.teslaris.core.dto.document.EventDTO;
import rs.teslaris.core.dto.document.PublicationSeriesDTO;
import rs.teslaris.core.dto.document.PublisherDTO;
import rs.teslaris.core.dto.institution.OrganisationUnitDTO;
import rs.teslaris.core.dto.person.PersonSnapshotDTO;

/**
 * A fixator mutates the record's snapshot DTO in place, which the engine then writes back through
 * {@link RevisionRestorer#restore} so that the fix is validated, reindexed and revisioned.
 */
public interface DataQualityFixator {

    String key();

    String name();

    default void applyFix(DocumentDTO document) {
    }

    default void applyFix(PersonSnapshotDTO person) {
    }

    default void applyFix(OrganisationUnitDTO organisationUnit) {
    }

    default void applyFix(EventDTO event) {
    }

    default void applyFix(PublicationSeriesDTO publicationSeries) {
    }

    default void applyFix(PublisherDTO publisher) {
    }

    // Project and funding overloads follow once their DTOs move to core.
}
