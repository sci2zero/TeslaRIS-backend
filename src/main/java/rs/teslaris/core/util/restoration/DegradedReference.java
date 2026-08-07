package rs.teslaris.core.util.restoration;

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
public class DegradedReference {

    private String messageKey;

    private String fieldPath;

    @Builder.Default
    private List<String> parameters = new ArrayList<>();

    private DegradationOutcome outcome;
}
