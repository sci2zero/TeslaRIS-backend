package rs.teslaris.revisioner.dto;

import java.util.List;
import rs.teslaris.core.dto.commontypes.MultilingualContentDTO;

/**
 * Every stored version of one policy. The latest version is the only one an upload can overwrite.
 */
public record PolicyFileSummaryDTO(

    String profileName,

    List<MultilingualContentDTO> name,

    List<String> versions,

    String latestVersion
) {
}
