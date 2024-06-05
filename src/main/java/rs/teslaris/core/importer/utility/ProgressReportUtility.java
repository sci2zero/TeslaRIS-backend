package rs.teslaris.core.importer.utility;

import jakarta.annotation.Nullable;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.query.Criteria;
import org.springframework.data.mongodb.core.query.Query;


public class ProgressReportUtility {

    public static void resetProgressReport(DataSet requestDataSet, Integer userId,
                                           MongoTemplate mongoTemplate) {
        Query deleteQuery = new Query();
        deleteQuery.addCriteria(Criteria.where("dataset").is(requestDataSet))
            .addCriteria(Criteria.where("userId").is(userId));
        mongoTemplate.remove(deleteQuery, LoadProgressReport.class);
        mongoTemplate.save(new LoadProgressReport("", userId, requestDataSet));
    }

    public static void deleteProgressReport(DataSet requestDataSet, Integer userId,
                                            MongoTemplate mongoTemplate) {
        Query deleteQuery = new Query();
        deleteQuery.addCriteria(Criteria.where("dataset").is(requestDataSet))
            .addCriteria(Criteria.where("userId").is(userId));
        mongoTemplate.remove(deleteQuery, LoadProgressReport.class);
    }

    @Nullable
    public static LoadProgressReport getProgressReport(DataSet requestDataSet, Integer userId,
                                                       MongoTemplate mongoTemplate) {
        Query query = new Query();
        query.addCriteria(Criteria.where("dataset").is(requestDataSet.name()))
            .addCriteria(Criteria.where("userId").is(userId));
        return mongoTemplate.findOne(query, LoadProgressReport.class);
    }

    public static void updateProgressReport(DataSet requestDataSet, String lastLoadedId,
                                            Integer userId, MongoTemplate mongoTemplate) {
        Query deleteQuery = new Query();
        deleteQuery.addCriteria(Criteria.where("dataset").is(requestDataSet))
            .addCriteria(Criteria.where("userId").is(userId));
        mongoTemplate.remove(deleteQuery, LoadProgressReport.class);

        mongoTemplate.save(new LoadProgressReport(lastLoadedId, userId, requestDataSet));
    }
}
