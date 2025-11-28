package rs.teslaris.exporter.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.annotation.Nullable;
import jakarta.servlet.http.HttpServletRequest;
import java.time.LocalDate;
import java.util.Collections;
import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.PageRequest;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import rs.teslaris.core.util.exceptionhandling.exception.UnsupportedEntityTypeException;
import rs.teslaris.core.util.exceptionhandling.exception.UnsupportedFilterException;
import rs.teslaris.core.util.persistence.IdentifierUtil;
import rs.teslaris.exporter.model.common.BaseExportEntity;
import rs.teslaris.exporter.model.common.ExportDocument;
import rs.teslaris.exporter.model.common.ExportOrganisationUnit;
import rs.teslaris.exporter.model.common.ExportPerson;
import rs.teslaris.exporter.model.skgif.JsonLdToTurtleConverter;
import rs.teslaris.exporter.model.skgif.SKGIFListResponse;
import rs.teslaris.exporter.model.skgif.SKGIFSingleResponse;
import rs.teslaris.exporter.service.interfaces.SKGIFExportService;
import rs.teslaris.exporter.util.skgif.OrganisationFilteringUtil;
import rs.teslaris.exporter.util.skgif.PersonFilteringUtil;
import rs.teslaris.exporter.util.skgif.ResearchProductFilteringUtil;
import rs.teslaris.exporter.util.skgif.SKGIFFilterCriteria;
import rs.teslaris.exporter.util.skgif.VenueFilteringUtil;

@RestController
@RequestMapping("/api/skg-if")
@RequiredArgsConstructor
public class SKGIFExportController {

    private final SKGIFExportService skgifExportService;


    @GetMapping(value = "/{entityType}/{localIdentifier}", produces = {
        MediaType.APPLICATION_JSON_VALUE, "text/turtle"})
    public ResponseEntity<?> getEntityById(
        HttpServletRequest request,
        @PathVariable String entityType,
        @PathVariable String localIdentifier,
        @RequestParam(defaultValue = "json") String format) {

        var entityClass = getEntityClass(request, entityType);
        localIdentifier = IdentifierUtil.removeCommonPrefix(localIdentifier);

        int databaseId;
        try {
            databaseId = Integer.parseInt(localIdentifier);
        } catch (NumberFormatException e) {
            throw new UnsupportedFilterException(
                "Invalid ID format. Supported format example: '" + IdentifierUtil.identifierPrefix +
                    "123456'.",
                request.getRequestURL().toString());
        }

        SKGIFSingleResponse response = skgifExportService.getEntityById(entityClass, databaseId,
            entityType.equals("venue"));

        if (format.equals("rdf") || format.equals("ttl")) {
            try {
                var mapper = new ObjectMapper();
                String jsonString = mapper.writeValueAsString(response);
                String turtleContent = JsonLdToTurtleConverter.convertJsonLdToTurtle(jsonString);

                return ResponseEntity.ok()
                    .contentType(MediaType.parseMediaType("text/turtle; charset=UTF-8"))
                    .header("Content-Disposition", "inline; filename=\"entity.ttl\"")
                    .body(turtleContent);

            } catch (Exception e) {
                throw new RuntimeException("Failed to convert to RDF format", e);
            }
        } else {
            return ResponseEntity.ok()
                .contentType(MediaType.APPLICATION_JSON)
                .body(response);
        }
    }

    @GetMapping("/{entityType}")
    public SKGIFListResponse<?> getEntitiesFiltered(HttpServletRequest request,
                                                    @PathVariable String entityType,
                                                    @RequestParam(defaultValue = "") String filter,
                                                    @RequestParam Integer page,
                                                    @RequestParam("page_size") Integer pageSize,
                                                    @RequestParam(required = false)
                                                    LocalDate dateFrom,
                                                    @RequestParam(required = false)
                                                    LocalDate dateTo) {
        var entityClass = getEntityClass(request, entityType);

        var filterCriteria = new SKGIFFilterCriteria(filter);
        var unsupportedFilters = validateFilters(filterCriteria, entityType);
        if (!unsupportedFilters.isEmpty()) {
            throw new UnsupportedFilterException("Unsupported filters: " + unsupportedFilters,
                request.getRequestURL().toString());
        }

        if (!List.of(10, 20, 50, 100).contains(pageSize)) {
            throw new UnsupportedFilterException(
                "Supported page sizes are [10, 20, 50, 100].",
                request.getRequestURL().toString());
        }

        var fullRequestUrl = request.getRequestURL().toString() +
            (Objects.nonNull(request.getQueryString()) ? "?" + request.getQueryString() : "");
        return skgifExportService.getEntitiesFiltered(entityClass, filter,
            entityType.equals("venue"), filterCriteria, dateFrom, dateTo,
            PageRequest.of(page, pageSize), fullRequestUrl);
    }

    private Class<? extends BaseExportEntity> getEntityClass(HttpServletRequest request,
                                                             String entityType) {
        var entityClass = getInternalEntityClassByType(entityType);
        if (Objects.isNull(entityClass)) {
            throw new UnsupportedEntityTypeException(
                "Entity type '" + entityType + "' is not supported.",
                request.getRequestURL().toString());
        }

        return entityClass;
    }

    @Nullable
    private Class<? extends BaseExportEntity> getInternalEntityClassByType(String entityType) {
        return switch (entityType) {
            case "person" -> ExportPerson.class;
            case "organisation" -> ExportOrganisationUnit.class;
            case "venue", "product" -> ExportDocument.class;
            default -> null;
        };
    }

    private List<String> validateFilters(SKGIFFilterCriteria criteria, String entityType) {
        List<String> supportedFilters = getSupportedFilters(entityType);
        return criteria.getFilterKeys().stream()
            .filter(filter -> !supportedFilters.contains(filter))
            .collect(Collectors.toList());
    }

    private List<String> getSupportedFilters(String entityType) {
        return switch (entityType) {
            case "person" -> PersonFilteringUtil.SUPPORTED_FILTERS;
            case "organisation" -> OrganisationFilteringUtil.SUPPORTED_FILTERS;
            case "venue" -> VenueFilteringUtil.SUPPORTED_FILTERS;
            case "product" -> ResearchProductFilteringUtil.SUPPORTED_FILTERS;
            default -> Collections.emptyList();
        };
    }
}
