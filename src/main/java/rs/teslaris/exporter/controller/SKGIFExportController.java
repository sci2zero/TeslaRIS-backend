package rs.teslaris.exporter.controller;

import jakarta.annotation.Nullable;
import jakarta.servlet.http.HttpServletRequest;
import java.util.List;
import java.util.Objects;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.PageRequest;
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
import rs.teslaris.exporter.model.skgif.SKGIFListResponse;
import rs.teslaris.exporter.model.skgif.SKGIFSingleResponse;
import rs.teslaris.exporter.service.interfaces.SKGIFExportService;

@RestController
@RequestMapping("/api/skg-if")
@RequiredArgsConstructor
public class SKGIFExportController {

    private final SKGIFExportService skgifExportService;


    @GetMapping("/{entityType}/{localIdentifier}")
    public SKGIFSingleResponse getEntityById(
        HttpServletRequest request,
        @PathVariable String entityType,
        @PathVariable String localIdentifier,
        @RequestParam(defaultValue = "json") String format) {
        var entityClass = getEntityClass(request, entityType);

        localIdentifier = localIdentifier.trim().replace(IdentifierUtil.identifierPrefix, "");

        int databaseId;
        try {
            databaseId = Integer.parseInt(localIdentifier);
        } catch (NumberFormatException e) {
            throw new UnsupportedFilterException(
                "Invalid ID format. Supported format example: '" + IdentifierUtil.identifierPrefix +
                    "123456'.",
                request.getRequestURL().toString());
        }

        return skgifExportService.getEntityById(entityClass, databaseId,
            entityType.equals("venue"));
    }

    @GetMapping("/{entityType}")
    public SKGIFListResponse getEntitiesFiltered(HttpServletRequest request,
                                                 @PathVariable String entityType,
                                                 @RequestParam(defaultValue = "") String filter,
                                                 @RequestParam Integer page,
                                                 @RequestParam("page_size") Integer pageSize) {
        var entityClass = getEntityClass(request, entityType);

        if (!List.of(10, 20, 50, 100).contains(pageSize)) {
            throw new UnsupportedFilterException(
                "Supported page sizes are [10, 20, 50, 100].",
                request.getRequestURL().toString());
        }

        return skgifExportService.getEntitiesFiltered(entityClass, filter,
            entityType.equals("venue"), PageRequest.of(page, pageSize));
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
}
