package rs.teslaris.importer.model.common;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.springframework.data.mongodb.core.mapping.Field;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class Event {

    @Field("name")
    private List<MultilingualContent> name = new ArrayList<>();

    @Field("name_abbreviation")
    private List<MultilingualContent> nameAbbreviation = new ArrayList<>();

    @Field("description")
    private List<MultilingualContent> description = new ArrayList<>();

    @Field("keywords")
    private List<MultilingualContent> keywords = new ArrayList<>();

    @Field("serial_event")
    private Boolean serialEvent;

    @Field("date_from")
    private LocalDate dateFrom;

    @Field("date_to")
    private LocalDate dateTo;

    @Field("cond_id")
    private String confId;

    @Field("state")
    private List<MultilingualContent> state = new ArrayList<>();

    @Field("place")
    private List<MultilingualContent> place = new ArrayList<>();

    @Field("open_alex_id")
    private String openAlexId;
}
