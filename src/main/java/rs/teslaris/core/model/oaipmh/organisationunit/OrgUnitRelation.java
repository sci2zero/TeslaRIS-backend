package rs.teslaris.core.model.oaipmh.organisationunit;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.ToString;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@ToString
public class OrgUnitRelation {

    private Integer sourceId;

    private Integer targetId;
}
