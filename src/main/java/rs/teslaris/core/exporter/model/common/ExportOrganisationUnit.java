package rs.teslaris.core.exporter.model.common;

import java.util.ArrayList;
import java.util.List;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.springframework.data.mongodb.core.mapping.Document;
import org.springframework.data.mongodb.core.mapping.Field;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Document(collection = "organisationUnitExports")
public class ExportOrganisationUnit extends BaseExportEntity {

    @Field("name")
    private List<ExportMultilingualContent> name = new ArrayList<>();

    @Field("name_abbreviation")
    private String nameAbbreviation;

    @Field("scopus_afid")
    private String scopusAfid;

    @Field("super_organisation_unit")
    private ExportOrganisationUnit superOU;
}
