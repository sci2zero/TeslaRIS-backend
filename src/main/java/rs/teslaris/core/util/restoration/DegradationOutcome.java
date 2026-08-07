package rs.teslaris.core.util.restoration;

public enum DegradationOutcome {

    /**
     * The reference was left unset. Used for values the entity can exist without.
     */
    DROPPED,

    /**
     * The reference was replaced by a weaker representation that keeps the information visible,
     * e.g. a contributor kept as a display name instead of a link to a person.
     */
    DEGRADED
}
