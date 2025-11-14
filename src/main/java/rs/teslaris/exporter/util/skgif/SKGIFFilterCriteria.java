package rs.teslaris.exporter.util.skgif;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.regex.Pattern;
import lombok.Getter;
import rs.teslaris.core.util.search.StringUtil;

@Getter
public class SKGIFFilterCriteria {

    private static final Pattern SAFE_VALUE_PATTERN =
        Pattern.compile("^[a-zA-Z0-9_\\-\\.@]+$");
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

            var sanitizedValue = sanitizeValue(keyValue[1]);
            if (Objects.nonNull(sanitizedValue)) {
                filters.put(keyValue[0], sanitizedValue);
                filterKeys.add(keyValue[0]);
                filterValues.add(sanitizedValue);
            }
        });
    }

    private String sanitizeValue(String value) {
        if (!StringUtil.valueExists(value)) {
            return null;
        }

        if (!SAFE_VALUE_PATTERN.matcher(value).matches()) {
            throw new IllegalArgumentException("Invalid filter value: " + value);
        }

        return value.trim();
    }

    public boolean containsTypeFilter() {
        return filterKeys.contains("type");
    }

    public boolean containsLastUpdatedFilter() {
        return filterKeys.contains("manifestations.dates.modified");
    }
}
