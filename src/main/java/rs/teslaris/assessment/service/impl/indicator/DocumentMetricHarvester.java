package rs.teslaris.assessment.service.impl.indicator;

import java.util.Optional;

public interface DocumentMetricHarvester {

    Optional<DocumentMetricResult> harvest(String doi);

}
