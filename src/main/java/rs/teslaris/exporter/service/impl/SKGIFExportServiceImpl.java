package rs.teslaris.exporter.service.impl;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.time.LocalDate;
import java.time.ZoneId;
import java.util.Date;
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
import rs.teslaris.exporter.model.common.ExportDocument;
import rs.teslaris.exporter.model.common.ExportPublicationType;
import rs.teslaris.exporter.model.converter.skgif.AgentConverter;
import rs.teslaris.exporter.model.converter.skgif.ResearchProductConverter;
import rs.teslaris.exporter.model.converter.skgif.VenueConverter;
import rs.teslaris.exporter.model.skgif.SKGIFListResponse;
import rs.teslaris.exporter.model.skgif.SKGIFMeta;
import rs.teslaris.exporter.model.skgif.SKGIFSingleResponse;
import rs.teslaris.exporter.service.interfaces.SKGIFExportService;
import rs.teslaris.exporter.util.skgif.OrganisationFilteringUtil;
import rs.teslaris.exporter.util.skgif.PersonFilteringUtil;
import rs.teslaris.exporter.util.skgif.ResearchProductFilteringUtil;
import rs.teslaris.exporter.util.skgif.SKGIFFilterCriteria;
import rs.teslaris.exporter.util.skgif.VenueFilteringUtil;

@Service
@RequiredArgsConstructor
@Slf4j
public class SKGIFExportServiceImpl implements SKGIFExportService {

    private final MongoTemplate mongoTemplate;

    @Value("${export.base.url}")
    private String baseUrl;


    @Override
    public <T extends BaseExportEntity> SKGIFSingleResponse<?> getEntityById(Class<T> entityClass,
                                                                             Integer localIdentifier,
                                                                             boolean isVenue) {
        var conversionMethod = getConversionMethod(entityClass, isVenue);

        var query = new Query();
        query.addCriteria(Criteria.where("database_id").is(localIdentifier));
        query.addCriteria(Criteria.where("deleted").is(false));
        addDocumentTypeConstraints(query, entityClass, isVenue);

        var records = mongoTemplate.find(query, entityClass);

        if (records.isEmpty()) {
            throw new NotFoundException("Record with ID " + localIdentifier + " does not exist.");
        }

        SKGIFSingleResponse<?> response;
        try {
            var convertedEntity =
                conversionMethod.invoke(null, records.getFirst());
            response = new SKGIFSingleResponse<>((List<Object>) convertedEntity, baseUrl);
        } catch (IllegalAccessException | InvocationTargetException e) {
            throw new RuntimeException(e);
        }

        return response;
    }

    @Override
    public <T extends BaseExportEntity> SKGIFListResponse<?> getEntitiesFiltered(
        Class<T> entityClass,
        String filter,
        boolean isVenue,
        SKGIFFilterCriteria criteria,
        LocalDate dateFrom,
        LocalDate dateTo,
        Pageable pageable) {
        var conversionMethod = getConversionMethod(entityClass, isVenue);

        var query = new Query();
        query.addCriteria(Criteria.where("deleted").is(false));

        if (!criteria.containsTypeFilter()) {
            addDocumentTypeConstraints(query, entityClass, isVenue);
        }

        addQueryFilters(entityClass, query, criteria, isVenue, dateFrom, dateTo);

        var totalCount = mongoTemplate.count(query, entityClass);

        query.with(pageable);
        var records = mongoTemplate.find(query, entityClass);

        var convertedRecords = records.stream().map(record -> {
            try {
                return ((List<Object>) conversionMethod.invoke(null, record)).getFirst();
            } catch (IllegalAccessException | InvocationTargetException e) {
                log.error("Error while converting SKG-IF entity: {}", e.getMessage());
                return null;
            }
        }).filter(Objects::nonNull).toList();

        var response = new SKGIFListResponse<>();
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

    private void addDocumentTypeConstraints(Query query, Class<?> entityClass, boolean isVenue) {
        if (entityClass.equals(ExportDocument.class)) {
            var venueTypes =
                List.of(ExportPublicationType.JOURNAL, ExportPublicationType.PROCEEDINGS,
                    ExportPublicationType.MONOGRAPH);
            if (isVenue) {
                query.addCriteria(Criteria.where("type").in(venueTypes));
            } else {
                query.addCriteria(Criteria.where("type").nin(venueTypes));
            }
        }
    }

    private void addQueryFilters(Class<? extends BaseExportEntity> entityClass, Query query,
                                 SKGIFFilterCriteria criteria, boolean isVenue, LocalDate dateFrom,
                                 LocalDate dateTo) {
        if (!criteria.containsLastUpdatedFilter() &&
            (Objects.nonNull(dateFrom) || Objects.nonNull(dateTo))) {

            Criteria dateCriteria = Criteria.where("last_updated");

            if (Objects.nonNull(dateFrom)) {
                dateCriteria = dateCriteria.gte(
                    Date.from(dateFrom.atStartOfDay(ZoneId.systemDefault()).toInstant()));
            }

            if (Objects.nonNull(dateTo)) {
                dateCriteria = dateCriteria.lte(
                    Date.from(dateTo.atStartOfDay(ZoneId.systemDefault()).toInstant()));
            }

            query.addCriteria(dateCriteria);
        }

        switch (entityClass.getSimpleName()) {
            case "ExportPerson":
                PersonFilteringUtil.addQueryFilters(criteria, query);
                break;
            case "ExportOrganisationUnit":
                OrganisationFilteringUtil.addQueryFilters(criteria, query);
                break;
            case "ExportDocument":
                if (isVenue) {
                    VenueFilteringUtil.addQueryFilters(criteria, query);
                } else {
                    ResearchProductFilteringUtil.addQueryFilters(criteria, query);
                }
                break;
            default:
                throw new IllegalArgumentException("No filter handler for: " + entityClass);
        }
    }
}
