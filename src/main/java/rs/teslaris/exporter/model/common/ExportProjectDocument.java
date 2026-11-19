package rs.teslaris.exporter.model.common;

import java.util.ArrayList;
import java.util.List;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.springframework.data.mongodb.core.mapping.Field;

/**
 * A document linked to the project. The relation type is the name of the project module's
 * ProjectDocumentType (REPORT, RESULT, USED, INTERNAL).
 */
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class ExportProjectDocument {

    @Field("document_id")
    private Integer documentId;

    @Field("relation_type")
    private String relationType;

    // Fundings that paid for this document (funding parts), empty when there are none.
    @Field("funding_ids")
    private List<Integer> fundingIds = new ArrayList<>();
}
