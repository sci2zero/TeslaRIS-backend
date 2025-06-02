package rs.teslaris.importer.utility;

import jakarta.annotation.Nullable;
import java.util.Objects;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.query.Criteria;
import org.springframework.data.mongodb.core.query.Query;


public class ProgressReportUtility {

    public static void resetProgressReport(DataSet requestDataSet, Integer userId,
                                           Integer institutionId,
                                           MongoTemplate mongoTemplate) {
        Query deleteQuery = new Query();
        deleteQuery.addCriteria(Criteria.where("dataset").is(requestDataSet))
            .addCriteria(Criteria.where("userId").is(userId));

        if (Objects.nonNull(institutionId)) {
            deleteQuery.addCriteria(Criteria.where("institutionId").is(institutionId));
        }

        mongoTemplate.remove(deleteQuery, LoadProgressReport.class);
        mongoTemplate.save(new LoadProgressReport("", userId, institutionId, requestDataSet));
    }

    public static void deleteProgressReport(DataSet requestDataSet, Integer userId,
                                            Integer institutionId,
                                            MongoTemplate mongoTemplate) {
        Query deleteQuery = new Query();
        deleteQuery.addCriteria(Criteria.where("dataset").is(requestDataSet))
            .addCriteria(Criteria.where("userId").is(userId));

        if (Objects.nonNull(institutionId)) {
            deleteQuery.addCriteria(Criteria.where("institutionId").is(institutionId));
        }

        mongoTemplate.remove(deleteQuery, LoadProgressReport.class);
    }

    @Nullable
    public static LoadProgressReport getProgressReport(
        DataSet requestDataSet, Integer userId,
        Integer institutionId,
        MongoTemplate mongoTemplate) {
        Query query = new Query();
        query.addCriteria(Criteria.where("dataset").is(requestDataSet.name()))
            .addCriteria(Criteria.where("userId").is(userId));

        if (Objects.nonNull(institutionId)) {
            query.addCriteria(Criteria.where("institutionId").is(institutionId));
        }

        return mongoTemplate.findOne(query, LoadProgressReport.class);
    }

    public static void updateProgressReport(DataSet requestDataSet, String lastLoadedId,
                                            Integer userId, Integer institutionId,
                                            MongoTemplate mongoTemplate) {
        Query deleteQuery = new Query();
        deleteQuery.addCriteria(Criteria.where("dataset").is(requestDataSet))
            .addCriteria(Criteria.where("userId").is(userId));

        if (Objects.nonNull(institutionId)) {
            deleteQuery.addCriteria(Criteria.where("institutionId").is(institutionId));
        }

        mongoTemplate.remove(deleteQuery, LoadProgressReport.class);

        mongoTemplate.save(
            new LoadProgressReport(lastLoadedId, userId, institutionId, requestDataSet));
    }
}
