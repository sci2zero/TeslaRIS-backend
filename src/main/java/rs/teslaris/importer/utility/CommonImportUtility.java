package rs.teslaris.importer.utility;

import ai.djl.translate.TranslateException;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.util.Set;
import java.util.stream.Collectors;
import lombok.extern.slf4j.Slf4j;
import org.nd4j.linalg.api.ndarray.INDArray;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.query.Criteria;
import org.springframework.data.mongodb.core.query.Query;
import org.springframework.stereotype.Component;
import rs.teslaris.core.model.commontypes.BaseEntity;
import rs.teslaris.core.service.interfaces.user.UserService;
import rs.teslaris.core.util.deduplication.DeduplicationUtil;
import rs.teslaris.importer.model.common.DocumentImport;

@Component
@Slf4j
public class CommonImportUtility {

    private static MongoTemplate mongoTemplate;

    private static UserService userService;


    @Autowired
    public CommonImportUtility(MongoTemplate mongoTemplate, UserService userService) {
        CommonImportUtility.mongoTemplate = mongoTemplate;
        CommonImportUtility.userService = userService;
    }

    public static INDArray generateEmbedding(DocumentImport entry) {
        try {
            var json = new ObjectMapper().writeValueAsString(entry);
            var flattened = DeduplicationUtil.flattenJson(json);
            return DeduplicationUtil.getEmbedding(flattened);
        } catch (JsonProcessingException | TranslateException e) {
            log.error("Error generating embedding: {}", e.getMessage());
            return null;
        }
    }

    public static DocumentImport findExistingImport(String identifier) {
        var query = new Query(Criteria.where("identifier").is(identifier));
        return mongoTemplate.findOne(query, DocumentImport.class, "documentImports");
    }

    public static DocumentImport findImportByDOI(String doi) {
        var query = new Query(Criteria.where("doi").is(doi));
        return mongoTemplate.findOne(query, DocumentImport.class, "documentImports");
    }

    public static Set<Integer> getAdminUserIds() {
        return userService.findAllSystemAdminUsers().stream()
            .map(BaseEntity::getId)
            .collect(Collectors.toSet());
    }
}
