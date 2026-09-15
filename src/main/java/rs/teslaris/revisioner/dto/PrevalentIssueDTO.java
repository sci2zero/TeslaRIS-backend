package rs.teslaris.revisioner.dto;

import jakarta.annotation.Nullable;
import java.util.List;
import rs.teslaris.core.dto.commontypes.MultilingualContentDTO;
import rs.teslaris.revisioner.util.dataquality.RepositoryEntityType;

/**
 * The rule that fails most often for an entity type. Ties are decided by rule key ascending.
 */
public record PrevalentIssueDTO(

    RepositoryEntityType entityType,

    @Nullable
    String ruleKey,

    List<MultilingualContentDTO> title,

    long occurrences
) {

    public static PrevalentIssueDTO none(RepositoryEntityType entityType) {
        return new PrevalentIssueDTO(entityType, null, List.of(), 0);
    }
}
