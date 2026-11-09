package rs.teslaris.exporter.model.common;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.springframework.data.mongodb.core.mapping.Document;
import org.springframework.data.mongodb.core.mapping.Field;
import rs.teslaris.core.model.project.ProjectCollaborationType;
import rs.teslaris.core.model.project.ProjectResearchType;
import rs.teslaris.core.model.project.ProjectStatus;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Document(collection = "projectExports")
public class ExportProject extends BaseExportEntity {

    @Field("name")
    private List<ExportMultilingualContent> name = new ArrayList<>();

    @Field("name_abbreviation")
    private List<ExportMultilingualContent> nameAbbreviation = new ArrayList<>();

    @Field("description")
    private List<ExportMultilingualContent> description = new ArrayList<>();

    @Field("keywords")
    private List<ExportMultilingualContent> keywords = new ArrayList<>();

    @Field("doi")
    private String doi;

    @Field("raid")
    private String raid;

    @Field("national_id")
    private String nationalId;

    @Field("identifiers")
    private List<ExportEntityIdentifier> identifiers = new ArrayList<>();

    @Field("uris")
    private List<String> uris = new ArrayList<>();

    @Field("date_from")
    private LocalDate dateFrom;

    @Field("date_to")
    private LocalDate dateTo;

    @Field("status")
    private ProjectStatus status;

    @Field("collaboration_type")
    private ProjectCollaborationType collaborationType;

    @Field("research_type")
    private ProjectResearchType researchType;

    @Field("not_funded")
    private Boolean notFunded;

    @Field("costs")
    private ExportMonetaryAmount costs;

    @Field("persons")
    private List<ExportProjectPersonContribution> persons = new ArrayList<>();

    @Field("organisations")
    private List<ExportProjectOrganisationContribution> organisations = new ArrayList<>();
}
