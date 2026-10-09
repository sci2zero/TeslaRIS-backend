package rs.teslaris.core.revision;

/**
 * The identifier fields a {@link QualityUniquenessChecker} can be asked about.
 * <p>
 * An enum rather than a field name, so a rule that asks for an identifier its entity does not
 * have is a compile error in the checker rather than a rule that silently never fires.
 */
public enum QualityIdentifierField {
    DOI,
    RAID,
    NATIONAL_ID
}
