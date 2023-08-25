package rs.teslaris.core.dto.document;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import rs.teslaris.core.model.document.EventContributionType;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class PersonEventContributionDTO extends PersonContributionDTO {

    private EventContributionType eventContributionType;
}
