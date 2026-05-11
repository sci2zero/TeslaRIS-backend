package rs.teslaris.project.indexmodel.funding;

import jakarta.persistence.Id;
import java.time.LocalDate;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.springframework.data.elasticsearch.annotations.Document;
import org.springframework.data.elasticsearch.annotations.Field;
import org.springframework.data.elasticsearch.annotations.FieldType;
import org.springframework.data.elasticsearch.annotations.Setting;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Document(indexName = "funding_application")
@Setting(settingPath = "/configuration/index-config.json")
public class FundingApplicationIndex {

    @Id
    private String id;

    @Field(type = FieldType.Integer, name = "database_id", store = true)
    private Integer databaseId;

    @Field(type = FieldType.Integer, name = "funding_call_id", store = true)
    private Integer fundingCallId;

    @Field(type = FieldType.Integer, name = "project_id", store = true)
    private Integer projectId;

    @Field(type = FieldType.Integer, name = "funder_id", store = true)
    private Integer funderId;

    @Field(type = FieldType.Date, name = "submission_date")
    private LocalDate submissionDate;

    @Field(type = FieldType.Date, name = "decision_date")
    private LocalDate decisionDate;

    @Field(type = FieldType.Keyword, name = "result")
    private String result;
}
