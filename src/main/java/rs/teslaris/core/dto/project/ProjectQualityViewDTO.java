package rs.teslaris.core.dto.project;

import java.time.LocalDate;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import rs.teslaris.core.dto.commontypes.MonetaryAmountDTO;
import rs.teslaris.core.dto.commontypes.MultilingualContentDTO;

/**
 * The part of a project revision the data quality profile has rules for.
 * <p>
 * A project's own DTO lives in the {@code project} module, which the quality calculator may not
 * depend on, so a project revision is assessed as this core-owned projection instead. Every field
 * the {@code Project.*} rules read is a core type, so nothing of the project domain has to move.
 * It deserializes straight from the stored {@code ProjectDTO} snapshot - the revision object
 * mapper does not fail on unknown properties - and the fields the snapshot excludes
 * ({@code persons}, {@code organisations}, {@code relations}, {@code funding}) are the ones no
 * rule can reach anyway.
 */
@Getter
@Setter
@NoArgsConstructor
public class ProjectQualityViewDTO {

    private Integer id;

    private String doi;

    private String raid;

    private String nationalId;

    private Set<String> internalIdentifiers = new HashSet<>();

    private Set<String> uris = new HashSet<>();

    private List<MultilingualContentDTO> name;

    private List<MultilingualContentDTO> description;

    private Set<Integer> researchAreasId = new HashSet<>();

    private LocalDate dateFrom;

    private LocalDate dateTo;

    private MonetaryAmountDTO costs;
}
