package rs.teslaris.exporter.model.common;

import jakarta.persistence.Column;
import java.util.ArrayList;
import java.util.List;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.springframework.data.mongodb.core.mapping.Document;
import org.springframework.data.mongodb.core.mapping.Field;
import rs.teslaris.core.model.person.Sex;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Document(collection = "personExports")
public class ExportPerson extends BaseExportEntity {

    @Field("name")
    private ExportPersonName name;

    @Field("apvnt")
    private String apvnt;

    @Column(name = "e_cris_id")
    private String eCrisId;

    @Column(name = "e_nauka_id")
    private String eNaukaId;

    @Field("orcid")
    private String orcid;

    @Field("scopus_id")
    private String scopusAuthorId;

    @Field("open_alex")
    private String openAlexId;

    @Field("sex")
    private Sex sex;

    @Field("electronic_addresses")
    private List<String> electronicAddresses = new ArrayList<>();

    @Field("employments")
    private List<ExportEmployment> employments = new ArrayList<>();
}
