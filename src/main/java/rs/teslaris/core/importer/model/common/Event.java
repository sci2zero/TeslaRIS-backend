package rs.teslaris.core.importer.model.common;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class Event {

    private List<MultilingualContent> name = new ArrayList<>();
    private List<MultilingualContent> nameAbbreviation = new ArrayList<>();
    private List<MultilingualContent> description = new ArrayList<>();
    private List<MultilingualContent> keywords = new ArrayList<>();
    private Boolean serialEvent;
    private LocalDate dateFrom;
    private LocalDate dateTo;
    private List<MultilingualContent> state = new ArrayList<>();
    private List<MultilingualContent> place = new ArrayList<>();
}
