package rs.teslaris.importer.utility;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.ToString;
import org.bson.types.ObjectId;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@ToString
public class LoadProgressReport {

    private String lastLoadedIdentifier;

    private ObjectId lastLoadedId;

    private Integer userId;

    private Integer institutionId;

    private DataSet dataset;
}
