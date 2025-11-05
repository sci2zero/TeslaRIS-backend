package rs.teslaris.exporter.service.impl;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.List;
import java.util.Objects;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.domain.Pageable;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.query.Criteria;
import org.springframework.data.mongodb.core.query.Query;
import org.springframework.stereotype.Service;
import rs.teslaris.core.util.exceptionhandling.exception.NotFoundException;
import rs.teslaris.exporter.model.common.BaseExportEntity;
import rs.teslaris.exporter.model.common.ExportPublicationType;
import rs.teslaris.exporter.model.converter.skgif.AgentConverter;
import rs.teslaris.exporter.model.converter.skgif.ResearchProductConverter;
import rs.teslaris.exporter.model.converter.skgif.VenueConverter;
import rs.teslaris.exporter.model.skgif.SKGIFListResponse;
import rs.teslaris.exporter.model.skgif.SKGIFMeta;
import rs.teslaris.exporter.model.skgif.SKGIFSingleResponse;
import rs.teslaris.exporter.service.interfaces.SKGIFExportService;

@Service
@RequiredArgsConstructor
@Slf4j
public class SKGIFExportServiceImpl implements SKGIFExportService {

    private final MongoTemplate mongoTemplate;

    @Value("${export.base.url}")
    private String baseUrl;


    @Override
    public <T extends BaseExportEntity> SKGIFSingleResponse getEntityById(Class<T> entityClass,
                                                                          Integer localIdentifier,
                                                                          boolean isVenue) {
        var conversionMethod = getConversionMethod(entityClass, isVenue);

        var query = new Query();
        query.addCriteria(Criteria.where("database_id").is(localIdentifier));
        var records = mongoTemplate.find(query, entityClass);

        if (records.isEmpty()) {
            throw new NotFoundException("Record with ID " + localIdentifier + " does not exist.");
        }

        SKGIFSingleResponse response;
        try {
            var convertedEntity =
                conversionMethod.invoke(null, records.getFirst());
            response = new SKGIFSingleResponse((List<Object>) convertedEntity, baseUrl);
        } catch (IllegalAccessException | InvocationTargetException e) {
            throw new RuntimeException(e);
        }

        return response;
    }

    @Override
    public <T extends BaseExportEntity> SKGIFListResponse getEntitiesFiltered(Class<T> entityClass,
                                                                              String filter,
                                                                              boolean isVenue,
                                                                              Pageable pageable) {
        var conversionMethod = getConversionMethod(entityClass, isVenue);

        var query = new Query();

        if (isVenue) {
            query.addCriteria(Criteria.where("type")
                .in(List.of(ExportPublicationType.JOURNAL, ExportPublicationType.PROCEEDINGS,
                    ExportPublicationType.MONOGRAPH)));
        }

        var totalCount = mongoTemplate.count(query, entityClass);

        query.with(pageable);
        var records = mongoTemplate.find(query, entityClass);

        var convertedRecords = records.stream().map(record -> {
            try {
                return ((List<Object>) conversionMethod.invoke(null, record)).getFirst();
            } catch (IllegalAccessException | InvocationTargetException e) {
                log.error("Error while converting SKG-IF entity: {}", e.getMessage());
                e.printStackTrace();
                return null;
            }
        }).filter(Objects::nonNull).toList();

        var response = new SKGIFListResponse();
        response.setMeta(
            new SKGIFMeta(totalCount, pageable.getPageNumber(), pageable.getPageSize()));
        response.setResults(convertedRecords);
        return response;
    }

    private Class<?> getConverterClass(Class<? extends BaseExportEntity> entityClass,
                                       boolean isVenue) {
        return switch (entityClass.getSimpleName()) {
            case "ExportPerson", "ExportOrganisationUnit" -> AgentConverter.class;
            case "ExportDocument" ->
                isVenue ? VenueConverter.class : ResearchProductConverter.class;
            default -> throw new IllegalArgumentException("No converter for: " + entityClass);
        };
    }

    private Method getConversionMethod(Class<? extends BaseExportEntity> entityClass,
                                       boolean isVenue) {
        var converterClass = getConverterClass(entityClass, isVenue);

        try {
            return converterClass.getMethod("toSKGIF", entityClass);
        } catch (NoSuchMethodException e) {
            throw new RuntimeException(e);
        }
    }
}
