package rs.teslaris.exporter.util.rocrate;

import com.fasterxml.jackson.databind.JsonNode;

public class Json2HtmlTable {

    public static String toHtmlTable(JsonNode node) {
        if (node.isObject()) {
            StringBuilder sb = new StringBuilder("<table border='1' cellpadding='4'>");
            node.fields().forEachRemaining(field -> {
                sb.append("<tr><th>")
                    .append(field.getKey())
                    .append("</th><td>")
                    .append(toHtmlTable(field.getValue()))
                    .append("</td></tr>");
            });
            sb.append("</table>");
            return sb.toString();
        }

        if (node.isArray()) {
            StringBuilder sb = new StringBuilder();

            // For each element in array, create SEPARATE table
            for (JsonNode elem : node) {
                if (elem.isObject()) {
                    sb.append("<div style='margin-bottom: 20px;'>");
                    sb.append("<table border='1' cellpadding='4'>");

                    elem.fields().forEachRemaining(field -> {
                        sb.append("<tr><th>")
                            .append(field.getKey())
                            .append("</th><td>")
                            .append(toHtmlTable(field.getValue()))
                            .append("</td></tr>");
                    });

                    sb.append("</table>");
                    sb.append("</div>");
                } else {
                    // Simple array of primitives - handle as before
                    sb.append("<table border='1' cellpadding='4'><tr><th>Value</th></tr>");
                    sb.append("<tr><td>").append(toHtmlTable(elem)).append("</td></tr>");
                    sb.append("</table>");
                }
            }
            return sb.toString();
        } else if (node.isTextual() && node.asText().startsWith("http")) {
            return "<a href=\"" + node.asText() + "\" target=\"_blank\">" + node.asText() + "</a>";
        }

        // Primitive values
        return node.asText();
    }
}
