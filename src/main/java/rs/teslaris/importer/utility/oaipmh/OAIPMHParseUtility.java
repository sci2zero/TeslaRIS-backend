package rs.teslaris.importer.utility.oaipmh;

import java.util.Date;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.stream.Collectors;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.query.Criteria;
import org.springframework.data.mongodb.core.query.Query;
import org.springframework.stereotype.Component;
import rs.teslaris.core.dto.person.BasicPersonDTO;
import rs.teslaris.core.model.oaipmh.common.MultilingualContent;
import rs.teslaris.exporter.util.ResumptionTokenStash;

@Component
public class OAIPMHParseUtility {

    private static MongoTemplate mongoTemplate;


    @Autowired
    public OAIPMHParseUtility(MongoTemplate mongoTemplate) {
        OAIPMHParseUtility.mongoTemplate = mongoTemplate;
    }

    public static Integer parseBISISID(String id) {
        var tokens = id.split("\\)");
        return Integer.parseInt(tokens[1]);
    }

    public static boolean validateResumptionToken(String token) {
        var query = new Query();
        query.addCriteria(Criteria.where("tokenValue").is(token));

        var tokenStash = mongoTemplate.findOne(query, ResumptionTokenStash.class);

        if (Objects.isNull(tokenStash)) {
            return false;
        }

        return !(new Date()).after(tokenStash.getExpirationTimestamp());
    }

    public static ResumptionTokenData parseResumptionToken(String resumptionToken)
        throws IllegalArgumentException {
        var tokens = resumptionToken.split("!");

        if (tokens.length != 6) {
            throw new IllegalArgumentException("Resumption token is invalid");
        }

        return new ResumptionTokenData(tokens[0], tokens[1], tokens[2], Integer.parseInt(tokens[3]),
            tokens[4]);
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

    public record ResumptionTokenData(String from, String until, String set, int page,
                                      String format) {
    }
}
