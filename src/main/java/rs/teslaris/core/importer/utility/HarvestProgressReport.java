package rs.teslaris.core.importer.utility;

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
public class HarvestProgressReport {

    private String resumptionToken;

    private Integer userId;

    private DataSet dataset;
}
