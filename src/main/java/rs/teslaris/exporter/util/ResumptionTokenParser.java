package rs.teslaris.exporter.util;

import java.util.Date;
import java.util.Objects;
import lombok.RequiredArgsConstructor;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.query.Criteria;
import org.springframework.data.mongodb.core.query.Query;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class ResumptionTokenParser {

    private final MongoTemplate mongoTemplate;


    public boolean validateResumptionToken(String token) {
        var query = new Query();
        query.addCriteria(Criteria.where("tokenValue").is(token));

        var tokenStash = mongoTemplate.findOne(query, ResumptionTokenStash.class);

        if (Objects.isNull(tokenStash)) {
            return false;
        }

        return !(new Date()).after(tokenStash.getExpirationTimestamp());
    }

    public ResumptionTokenData parseResumptionToken(String resumptionToken)
        throws IllegalArgumentException {
        var tokens = resumptionToken.split("!");

        if (tokens.length != 6) {
            throw new IllegalArgumentException("Resumption token is invalid");
        }

        return new ResumptionTokenData(tokens[0], tokens[1], tokens[2], Integer.parseInt(tokens[3]),
            tokens[4]);
    }

    public record ResumptionTokenData(String from, String until, String set, int page,
                                      String format) {
    }
}
