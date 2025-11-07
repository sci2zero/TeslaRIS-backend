package rs.teslaris.exporter.util.skgif;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import lombok.Getter;

@Getter
public class SKGIFFilterCriteria {

    private final List<String> filterKeys = new ArrayList<>();

    private final List<String> filterValues = new ArrayList<>();

    private final Map<String, String> filters = new HashMap<>();


    public SKGIFFilterCriteria(String filterQuery) {
        var queryTokens = filterQuery.trim().split(",");
        Arrays.asList(queryTokens).forEach(filterExpression -> {
            var keyValue = filterExpression.trim().split(":", 2);

            if (keyValue.length != 2) {
                return;
            }

            filters.put(keyValue[0], keyValue[1]);
            filterKeys.add(keyValue[0]);
            filterValues.add(keyValue[1]);
        });
    }

    public boolean containsTypeFilter() {
        return filterKeys.contains("type");
    }
}
