package rs.teslaris.exporter.model.common;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.springframework.data.mongodb.core.mapping.Field;
import rs.teslaris.core.model.person.DegreeType;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class ExportEducation {

    @Field("involvement_id")
    private Integer involvementId;

    @Field("institution")
    private ExportOrganisationUnit institution;

    @Field("display_institution")
    private List<ExportMultilingualContent> displayInstitution = new ArrayList<>();

    @Field("title")
    private List<ExportMultilingualContent> title = new ArrayList<>();

    @Field("degree_classification")
    private List<ExportMultilingualContent> degreeClassification = new ArrayList<>();

    @Field("degree_type")
    private DegreeType degreeType;

    @Field("date_from")
    private LocalDate dateFrom;

    @Field("date_to")
    private LocalDate dateTo;
}
