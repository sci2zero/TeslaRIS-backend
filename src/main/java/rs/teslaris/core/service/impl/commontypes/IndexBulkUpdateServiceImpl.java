package rs.teslaris.core.service.impl.commontypes;

import co.elastic.clients.elasticsearch.ElasticsearchClient;
import co.elastic.clients.elasticsearch._types.query_dsl.Query;
import co.elastic.clients.elasticsearch.core.UpdateByQueryRequest;
import co.elastic.clients.json.JsonData;
import java.util.Map;
import java.util.Objects;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import rs.teslaris.core.annotation.Traceable;
import rs.teslaris.core.service.interfaces.commontypes.IndexBulkUpdateService;

@Service
@RequiredArgsConstructor
@Slf4j
@Traceable
public class IndexBulkUpdateServiceImpl implements IndexBulkUpdateService {

    private final ElasticsearchClient elasticsearchClient;

    @Override
    public void removeIdFromRecord(String indexName, String fieldMappingName, Integer queryValue) {
        var request = new UpdateByQueryRequest.Builder()
            .index(indexName)
            .query(q -> q.term(t -> t
                .field(fieldMappingName)
                .value(queryValue)))
            .script(s -> s
                .inline(i -> i
                    .source("ctx._source." + fieldMappingName + " = null")
                    .lang("painless")
                    .params(Map.of())))
            .build();

        try {
            var response = elasticsearchClient.updateByQuery(request);

            log.info("Unbinded {} documents in index {} by field {}", response.updated(), indexName,
                fieldMappingName);
        } catch (Exception e) {
            log.error("An error occurred while unbinding index records: {}", e.getMessage());
        }
    }

    @Override
    public void setIdFieldForRecord(String indexName, String fieldMappingName, Integer queryValue,
                                    String idField, Integer idToSet) {
        var request = Objects.nonNull(idToSet) ?
            new UpdateByQueryRequest.Builder()
                .index(indexName)
                .waitForCompletion(true)
                .query(q -> q.term(t -> t
                    .field(fieldMappingName)
                    .value(queryValue)))
                .script(s -> s
                    .inline(i -> i
                        .source("ctx._source." + idField + " = params.idToSet")
                        .lang("painless")
                        .params(Map.of("idToSet", JsonData.of(idToSet)))))
                .build() :
            new UpdateByQueryRequest.Builder()
                .index(indexName)
                .waitForCompletion(true)
                .query(q -> q.term(t -> t
                    .field(fieldMappingName)
                    .value(queryValue)))
                .script(s -> s
                    .inline(i -> i
                        .source("ctx._source.remove('" + idField + "')")
                        .lang("painless")))
                .build();

        try {
            elasticsearchClient.updateByQuery(request);
            elasticsearchClient.indices().refresh(r -> r.index(indexName));

            log.info("Set ID {} for document in index {} on field {}", idToSet, indexName,
                idField);
        } catch (Exception e) {
            log.error("An error occurred while setting id to index records: {}", e.getMessage());
        }
    }

    @Override
    public void removeIdFromListField(String indexName, String fieldMappingName,
                                      Integer idToRemove) {
        var request = new UpdateByQueryRequest.Builder()
            .index(indexName)
            .query(q -> q.term(t -> t
                .field(fieldMappingName)
                .value(idToRemove)))
            .script(s -> s
                .inline(i -> i
                    .source("if (ctx._source." + fieldMappingName + " != null) { ctx._source." +
                        fieldMappingName + ".removeIf(id -> id == params.idToRemove); }")
                    .lang("painless")
                    .params(Map.of("idToRemove", JsonData.of(idToRemove)))))
            .build();

        try {
            var response = elasticsearchClient.updateByQuery(request);
            log.info("Removed ID {} from {} documents in index {} by field {}", idToRemove,
                response.updated(), indexName, fieldMappingName);
        } catch (Exception e) {
            log.error("An error occurred while updating list field records: {}", e.getMessage());
        }
    }

    @Override
    public void removeIdFromListAndRelatedArrayField(String indexName, String fieldMappingName,
                                                     String relatedArrayFieldName,
                                                     String relatedSortFieldName,
                                                     Integer idToRemove) {
        var request = new UpdateByQueryRequest.Builder()
            .index(indexName)
            .query(q -> q.term(t -> t
                .field(fieldMappingName)
                .value(idToRemove)))
            .script(s -> s
                .inline(i -> i
                    .source(
                        "if (ctx._source." + fieldMappingName + " != null && ctx._source." +
                            relatedArrayFieldName + " != null) {" +
                            "  int indexToRemove = ctx._source." + fieldMappingName +
                            ".indexOf(params.idToRemove);" +
                            "  if (indexToRemove != -1) {" +
                            "    ctx._source." + fieldMappingName + ".remove(indexToRemove);" +
                            "    def relatedArray = ctx._source." + relatedArrayFieldName +
                            ".splitOnToken(';');" +
                            "    StringBuilder updatedArray = new StringBuilder();" +
                            "    for (int i = 0; i < relatedArray.length; i++) {" +
                            "      if (i != indexToRemove) {" +
                            "        if (updatedArray.length() > 0) {" +
                            "          updatedArray.append(';');" +
                            "        }" +
                            "        updatedArray.append(relatedArray[i]);" +
                            "      }" +
                            "    }" +
                            "    ctx._source." + relatedArrayFieldName +
                            " = updatedArray.toString();" +
                            "    ctx._source." + relatedSortFieldName +
                            " = updatedArray.toString();" +
                            "  }" +
                            "}"
                    )
                    .lang("painless")
                    .params(Map.of("idToRemove", JsonData.of(idToRemove)))))
            .build();

        try {
            var response = elasticsearchClient.updateByQuery(request);
            log.info("Removed ID {} and corresponding element from {} documents in index {}",
                idToRemove, response.updated(), indexName);
        } catch (Exception e) {
            log.error("An error occurred while updating list and related array fields: {}",
                e.getMessage());
        }
    }

    @Override
    public void setYearForAggregatedRecord(String fieldMappingName, Integer queryValue,
                                           Integer year) {
        var requestBuilder = new UpdateByQueryRequest.Builder()
            .index("document_publication")
            .waitForCompletion(true);

        var queryBuilder = Query.of(q -> q.bool(b -> {
            b.must(m -> m.term(t -> t.field(fieldMappingName).value(queryValue)));

            if (fieldMappingName.startsWith("monograph")) {
                b.must(m ->
                    m.term(t -> t.field("publication_type").value("CHAPTER")));
            }

            return b;
        }));

        requestBuilder.query(queryBuilder)
            .script(s -> s
                .inline(i -> i
                    .source("ctx._source.year = params.year")
                    .lang("painless")
                    .params(Map.of("year", JsonData.of(year)))));

        var request = requestBuilder.build();

        try {
            elasticsearchClient.updateByQuery(request);
            elasticsearchClient.indices().refresh(r -> r.index("document_publication"));

            log.info("Set year {} for document in index document_publication on field {}", year,
                fieldMappingName);
        } catch (Exception e) {
            log.error(
                "An error occurred while setting year to document_publication index records: {}",
                e.getMessage());
        }
    }
}
