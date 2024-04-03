package rs.teslaris.core.importer.utility;

import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.query.Criteria;
import org.springframework.data.mongodb.core.query.Query;


public class ProgressReportUtility {

    public static void resetProgressReport(OAIPMHDataSet requestDataSet, Integer userId,
                                           MongoTemplate mongoTemplate) {
        Query deleteQuery = new Query();
        deleteQuery.addCriteria(Criteria.where("dataset").is(requestDataSet))
            .addCriteria(Criteria.where("userId").is(userId));
        mongoTemplate.remove(deleteQuery, ProgressReport.class);
        mongoTemplate.save(new ProgressReport("", userId, requestDataSet));
    }

    public static void deleteProgressReport(OAIPMHDataSet requestDataSet, Integer userId,
                                            MongoTemplate mongoTemplate) {
        Query deleteQuery = new Query();
        deleteQuery.addCriteria(Criteria.where("dataset").is(requestDataSet))
            .addCriteria(Criteria.where("userId").is(userId));
        mongoTemplate.remove(deleteQuery, ProgressReport.class);
    }
}
