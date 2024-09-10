package rs.teslaris.core.exporter.model.common;

import jakarta.persistence.Id;
import java.util.Date;
import java.util.HashSet;
import java.util.Set;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.springframework.data.mongodb.core.mapping.Field;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public abstract class BaseExportEntity {

    @Id
    private String id;

    @Field("database_id")
    private Integer databaseId;

    @Field("old_id")
    private Integer oldId;

    @Field("deleted")
    private Boolean deleted = false;

    @Field("last_updated")
    private Date lastUpdated;

    @Field("related_institution_ids")
    private Set<Integer> relatedInstitutionIds = new HashSet<>();

    @Field("actively_related_institution_ids")
    private Set<Integer> activelyRelatedInstitutionIds = new HashSet<>();
}
