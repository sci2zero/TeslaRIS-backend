package rs.teslaris.core.util.oaipmh;

import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.stream.Collectors;
import rs.teslaris.core.dto.person.BasicPersonDTO;
import rs.teslaris.core.model.oaipmh.common.MultilingualContent;

public final class OAIPMHParseUtility {

    private OAIPMHParseUtility() {
    }

    public static Integer parseBISISID(String id) {
        var tokens = id.split("\\)");
        return tokens.length > 1 ? Integer.parseInt(tokens[1]) : Integer.parseInt(tokens[0]);
    }

    public static void parseElectronicAddresses(List<String> electronicAddresses,
                                                BasicPersonDTO dto) {
        electronicAddresses.forEach((electronicAddress) -> {
            var tokens = electronicAddress.split(":");
            switch (tokens[0]) {
                case "mailto":
                    dto.setContactEmail(tokens[1]);
                    break;
                case "tel":
                    // TODO: SUPPORT MULTIPLE PHONE NUMBERS
                    dto.setPhoneNumber(tokens[1]);
                    break;
            }
        });
    }

    public static List<MultilingualContent> groupParsedMultilingualKeywords(
        List<MultilingualContent> keywords) {
        Map<String, List<String>> keywordsByLang = keywords.stream()
            .filter(k -> Objects.nonNull(k.getLang()) && Objects.nonNull(k.getValue()))
            .collect(Collectors.groupingBy(
                MultilingualContent::getLang,
                Collectors.mapping(MultilingualContent::getValue, Collectors.toList())
            ));

        return keywordsByLang.entrySet().stream()
            .map(entry -> new MultilingualContent(
                entry.getKey(),
                String.join("\n", entry.getValue())
            ))
            .toList();
    }
}
