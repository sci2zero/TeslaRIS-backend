package rs.teslaris.importer.utility;

import ai.djl.translate.TranslateException;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import jakarta.annotation.Nullable;
import java.util.ArrayList;
import java.util.Objects;
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
            var mapper = new ObjectMapper();
            mapper.registerModule(new JavaTimeModule());
            mapper.disable(
                SerializationFeature.WRITE_DATES_AS_TIMESTAMPS); // Optional, for ISO-8601

            var json = mapper.writeValueAsString(entry);
            var flattened = DeduplicationUtil.flattenJson(json);
            return DeduplicationUtil.getEmbedding(flattened);
        } catch (JsonProcessingException | TranslateException e) {
            log.error("Error generating embedding: {}", e.getMessage());
            return null;
        }
    }

    @Nullable
    public static DocumentImport findExistingImport(String identifier) {
        var query = new Query(Criteria.where("identifier").is(identifier));
        return mongoTemplate.findOne(query, DocumentImport.class, "documentImports");
    }

    @Nullable
    public static DocumentImport findImportByDOIOrMetadata(DocumentImport documentImport) {
        var doi = documentImport.getDoi();
        if (Objects.isNull(doi) || doi.isBlank()) {
            doi = "NOT_MATCHING";
        }

        var titleCriteria = new ArrayList<Criteria>();
        documentImport.getTitle().forEach(content -> {
            titleCriteria.add(new Criteria().andOperator(
                Criteria.where("title.content").is(content.getContent()),
                Criteria.where("publication_type").is(documentImport.getPublicationType()),
                Criteria.where("document_date").is(documentImport.getDocumentDate())
            ));
        });
        var titleCriteriaArray = titleCriteria.toArray(new Criteria[0]);

        var criteria = new Criteria().orOperator(
            Criteria.where("doi").is(doi),
            new Criteria().orOperator(titleCriteriaArray)
        );

        var query = new Query(criteria);
        return mongoTemplate.findOne(query, DocumentImport.class, "documentImports");
    }

    public static Set<Integer> getAdminUserIds() {
        return userService.findAllSystemAdminUsers().stream()
            .map(BaseEntity::getId)
            .collect(Collectors.toSet());
    }
}
