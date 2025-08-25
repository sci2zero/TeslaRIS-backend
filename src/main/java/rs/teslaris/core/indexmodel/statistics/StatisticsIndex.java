package rs.teslaris.core.indexmodel.statistics;

import jakarta.persistence.Id;
import java.time.LocalDateTime;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.springframework.data.elasticsearch.annotations.Document;
import org.springframework.data.elasticsearch.annotations.Field;
import org.springframework.data.elasticsearch.annotations.FieldType;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Document(indexName = "statistics")
public class StatisticsIndex {

    @Id
    private String id;

    @Field(type = FieldType.Date, store = true, name = "timestamp")
    private LocalDateTime timestamp;

    @Field(type = FieldType.Integer, store = true, name = "document_id")
    private Integer documentId;

    @Field(type = FieldType.Integer, store = true, name = "person_id")
    private Integer personId;

    @Field(type = FieldType.Integer, store = true, name = "organisation_unit_id")
    private Integer organisationUnitId;

    @Field(type = FieldType.Integer, store = true, name = "event_id")
    private Integer eventId;

    @Field(type = FieldType.Integer, store = true, name = "publication_series_id")
    private Integer publicationSeriesId;

    @Field(type = FieldType.Keyword, store = true, name = "type")
    private String type;

    @Field(type = FieldType.Keyword, store = true, name = "session_id")
    private String sessionId;

    @Field(type = FieldType.Keyword, store = true, name = "ip_address")
    private String ipAddress;

    @Field(type = FieldType.Keyword, store = true, name = "country_name")
    private String countryName;

    @Field(type = FieldType.Keyword, store = true, name = "country_code")
    private String countryCode;
}
