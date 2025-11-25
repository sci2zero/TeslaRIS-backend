package rs.teslaris.exporter.model.common;

import java.util.Date;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.springframework.data.mongodb.core.mapping.Field;
import rs.teslaris.core.model.document.AccessRights;
import rs.teslaris.core.model.document.License;
import rs.teslaris.core.model.document.ResourceType;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class ExportDocumentFile {

    @Field("type")
    private ResourceType type;

    @Field("last_updated")
    private Date lastUpdated;

    @Field("creation_date")
    private Date creationDate;

    @Field("license")
    private License license;

    @Field("access_rights")
    private AccessRights accessRights;
}
