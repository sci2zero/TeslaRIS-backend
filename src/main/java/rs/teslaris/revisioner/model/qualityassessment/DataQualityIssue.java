package rs.teslaris.revisioner.model.qualityassessment;

import java.util.ArrayList;
import java.util.List;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class DataQualityIssue {

    private String key;

    @Builder.Default
    private List<String> parameters = new ArrayList<>();

    private IssueSeverity severity;

    private String target;

    private QualityDimension dimension;

    private boolean blocking;
}
