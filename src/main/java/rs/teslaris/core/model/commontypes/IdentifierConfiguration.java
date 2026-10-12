package rs.teslaris.core.model.commontypes;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/**
 * The defaults describe how the system behaved before this configuration existed.
 */
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class IdentifierConfiguration {

    private Boolean hideWhenNotSet = true;

    private Boolean featured = false;

    private Boolean canBeUpdated = true;
}
