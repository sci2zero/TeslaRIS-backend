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
public class LoadProgressReport {

    private String lastLoadedId;

    private Integer userId;

    private OAIPMHDataSet dataset;
}
