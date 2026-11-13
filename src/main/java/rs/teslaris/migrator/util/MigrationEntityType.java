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
    PERSON_PRIZE,
    PERSON_EDUCATION,
    THESIS_SUPERVISION,
    THESIS_EXAMINATION,
    PROJECT,
    PROJECT_FUNDING,
    PROJECT_ORGANISATION,
    CV_FUNDING,

    DOCUMENT,
    JOURNAL_PUBLICATION,
    PROCEEDINGS_PUBLICATION,
    THESIS,
    MONOGRAPH,
    MONOGRAPH_PUBLICATION,
    PERFORMANCE_RELATED_OUTPUT,

    CONFERENCE,
    PROCEEDINGS;


    public MigrationEntityType parent() {
        return switch (this) {
            case JOURNAL_PUBLICATION, PROCEEDINGS_PUBLICATION, THESIS, MONOGRAPH,
                 MONOGRAPH_PUBLICATION, PERFORMANCE_RELATED_OUTPUT -> DOCUMENT;
            case PERSON_EMPLOYMENT, PERSON_PRIZE -> PERSON;
            case PROJECT_FUNDING, PROJECT_ORGANISATION -> PROJECT;
            default -> null;
        };
    }

    /**
     * Whether a request for this type takes items of {@code itemType}. A thesis request also
     * takes the educations, supervisions and examinations, which make theses too.
     */
    public boolean includes(MigrationEntityType itemType) {
        if (Objects.equals(this, itemType)) {
            return true;
        }

        return Objects.equals(THESIS, this) && switch (itemType) {
            case PERSON_EDUCATION, THESIS_SUPERVISION, THESIS_EXAMINATION -> true;
            default -> false;
        };
    }

    public boolean isDocumentType() {
        return Objects.equals(DOCUMENT, parent());
    }
}
