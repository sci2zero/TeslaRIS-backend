package rs.teslaris.core.util.signposting;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.stream.Collectors;

public class FairSignpostingLinksetUtility {

    private static final ObjectMapper mapper = new ObjectMapper();

    /**
     * JSON-based linkset
     */
    public static String toJson(List<LinkEntry> links) {
        Map<String, Map<String, List<Map<String, String>>>> grouped =
            links.stream().collect(Collectors.groupingBy(
                LinkEntry::anchor,
                Collectors.groupingBy(
                    LinkEntry::rel,
                    Collectors.mapping(l -> {
                        Map<String, String> entry = new HashMap<>();
                        entry.put("href", l.href);
                        if (Objects.nonNull(l.type)) {
                            entry.put("type", l.type);
                        }
                        return entry;
                    }, Collectors.toList())
                )
            ));

        var linkset = new ArrayList<Map<String, Object>>();
        grouped.forEach((anchor, relations) -> {
            Map<String, Object> obj = new LinkedHashMap<>();
            obj.put("anchor", anchor);
            obj.putAll(relations);
            linkset.add(obj);
        });

        try {
            return mapper.writerWithDefaultPrettyPrinter().writeValueAsString(
                Collections.singletonMap("linkset", linkset));
        } catch (JsonProcessingException e) {
            return ""; // should never happen
        }
    }

    /**
     * RFC 9264 Linkset (linkset document in Link header serialization style)
     */
    public static String toLinksetFormat(List<LinkEntry> links) {
        return links.stream()
            .map(l -> {
                var sb = new StringBuilder();
                sb.append("<").append(l.href).append(">");
                sb.append(" ; rel=\"").append(l.rel).append("\"");
                if (Objects.nonNull(l.type)) {
                    sb.append(" ; type=\"").append(l.type).append("\"");
                }
                if (Objects.nonNull(l.anchor)) {
                    sb.append(" ; anchor=\"").append(l.anchor).append("\"");
                }
                return sb.toString();
            })
            .collect(Collectors.joining(" ,\n"));
    }


    @JsonInclude(JsonInclude.Include.NON_NULL)
    public record LinksetJSON(
        List<Map<String, Object>> linkset
    ) {
    }

    public record LinkEntry(
        String href,
        String rel,
        String type,   // optional
        String anchor // optional
    ) {
    }
}
