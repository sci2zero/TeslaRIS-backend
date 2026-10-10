package rs.teslaris.core.revision;

import java.util.List;
import java.util.Objects;

/**
 * What a data quality assessment needs to know about the record it describes but cannot read from
 * the record's own snapshot: who the record belongs to, and what to call it.
 * <p>
 * {@code organisationUnitIds} is what every scoped query and the per-record authorization check
 * match on, so a scope that comes back empty makes the assessment invisible to everyone but an
 * administrator - see {@link QualityScopeResolver}.
 *
 * @param relatedPersonIds    persons the record is attributed to
 * @param organisationUnitIds institutions the record belongs to, ancestors included
 * @param entityNameSr        display name in Serbian, may be {@code null}
 * @param entityNameOther     display name in the other language, may be {@code null}
 */
public record QualityScope(
    List<Integer> relatedPersonIds,
    List<Integer> organisationUnitIds,
    String entityNameSr,
    String entityNameOther
) {

    public QualityScope {
        relatedPersonIds = Objects.isNull(relatedPersonIds) ? List.of() : List.copyOf(
            relatedPersonIds);
        organisationUnitIds = Objects.isNull(organisationUnitIds) ? List.of() : List.copyOf(
            organisationUnitIds);
    }
}
