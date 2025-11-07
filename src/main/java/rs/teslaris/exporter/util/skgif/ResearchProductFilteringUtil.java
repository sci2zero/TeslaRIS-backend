package rs.teslaris.exporter.util.skgif;

import java.util.Arrays;
import java.util.List;
import org.springframework.data.mongodb.core.query.Query;

public class ResearchProductFilteringUtil {

    public static final List<String> SUPPORTED_FILTERS = Arrays.asList(
        "identifiers.scheme", "identifiers.value", "name", "short_name", "other_names"
    );


    public static void addQueryFilters(SKGIFFilterCriteria criteria, Query query) {

    }
}
