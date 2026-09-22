package rs.teslaris.migrator.util;

import java.util.Objects;

/**
 * Entity types the migrator can produce.
 * <p>
 * Document subtypes report {@link #DOCUMENT} as their parent, which is what allows a pipeline
 * registered for a specific subtype (its own endpoint and record model) to take precedence over the
 * generic document pipeline, while unregistered subtypes fall back to it.
 */
public enum MigrationEntityType {

    ORGANISATION_UNIT,
    PERSON,
    PERSON_EMPLOYMENT,

    DOCUMENT,
    JOURNAL_PUBLICATION,
    PROCEEDINGS_PUBLICATION,
    THESIS,
    MONOGRAPH,
    MONOGRAPH_PUBLICATION;


    public MigrationEntityType parent() {
        return switch (this) {
            case JOURNAL_PUBLICATION, PROCEEDINGS_PUBLICATION, THESIS, MONOGRAPH,
                 MONOGRAPH_PUBLICATION -> DOCUMENT;
            default -> null;
        };
    }

    public boolean isDocumentType() {
        return Objects.equals(DOCUMENT, parent());
    }
}
