package rs.teslaris.thesislibrary.service.impl;

import jakarta.annotation.Nullable;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.OutputStreamWriter;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.InputStreamResource;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import rs.teslaris.core.annotation.Traceable;
import rs.teslaris.core.converter.document.DocumentPublicationConverter;
import rs.teslaris.core.dto.commontypes.ExportFileType;
import rs.teslaris.core.dto.commontypes.TableExportRequest;
import rs.teslaris.core.indexmodel.DocumentPublicationIndex;
import rs.teslaris.core.indexrepository.DocumentPublicationIndexRepository;
import rs.teslaris.core.model.commontypes.ExportableEndpointType;
import rs.teslaris.core.model.document.Document;
import rs.teslaris.core.service.impl.CSVExportHelper;
import rs.teslaris.core.service.interfaces.document.CitationService;
import rs.teslaris.core.service.interfaces.document.ThesisService;
import rs.teslaris.core.util.search.StringUtil;
import rs.teslaris.thesislibrary.dto.ThesisCSVExportRequestDTO;
import rs.teslaris.thesislibrary.service.interfaces.ThesisLibraryCSVExportService;
import rs.teslaris.thesislibrary.service.interfaces.ThesisSearchService;

@Service
@RequiredArgsConstructor
@Transactional
@Traceable
public class ThesisLibraryCSVExportServiceImpl implements ThesisLibraryCSVExportService {

    private final ThesisSearchService thesisSearchService;

    private final CitationService citationService;

    private final DocumentPublicationIndexRepository documentPublicationIndexRepository;

    private final ThesisService thesisService;

    @Value("${csv-export.maximum-export-amount}")
    private Integer maximumExportAmount;


    @Override
    @Nullable
    public InputStreamResource exportThesesToCSV(ThesisCSVExportRequestDTO request) {
        if (!List.of(ExportFileType.CSV, ExportFileType.XLSX)
            .contains(request.getExportFileType())) {
            return null;
        }

        String configurationFile = "thesisSearchFieldConfiguration.json";

        var rowsData = new ArrayList<List<String>>();
        var tableHeaders = CSVExportHelper.getTableHeaders(request, configurationFile);
        CSVExportHelper.addCitationColumns(tableHeaders, request);
        rowsData.add(tableHeaders);

        if (request.getExportMaxPossibleAmount()) {
            returnBulkDataFromDefinedEndpoint(request)
                .forEach(entity -> {
                    var rowData =
                        CSVExportHelper.constructRowData(entity, request.getColumns(),
                            configurationFile,
                            request.getExportLanguage());
                    CSVExportHelper.addCitationData(rowData, entity, request, citationService);
                    rowsData.add(rowData);
                });
        } else {
            request.getExportEntityIds().forEach(
                entityId -> documentPublicationIndexRepository.findDocumentPublicationIndexByDatabaseId(
                        entityId)
                    .ifPresent(entity -> {
                        var rowData = CSVExportHelper.constructRowData(entity, request.getColumns(),
                            configurationFile,
                            request.getExportLanguage());
                        CSVExportHelper.addCitationData(rowData, entity, request, citationService);
                        rowsData.add(rowData);
                    }));
        }

        return CSVExportHelper.createExportFile(rowsData, request.getExportFileType());
    }

    @Override
    public InputStreamResource exportThesesToBibliographicFile(ThesisCSVExportRequestDTO request) {
        if (!List.of(ExportFileType.BIB, ExportFileType.RIS, ExportFileType.ENW)
            .contains(request.getExportFileType())) {
            return null;
        }

        var exportedEntities = new ArrayList<String>();

        if (request.getExportMaxPossibleAmount()) {
            returnBulkDataFromDefinedEndpoint(request)
                .forEach(entity -> {
                    exportedEntities.add(getBibliographicExportEntity(request,
                        thesisService.getThesisById(entity.getDatabaseId())));
                });
        } else {
            request.getExportEntityIds().forEach(entityId -> exportedEntities.add(
                getBibliographicExportEntity(request, thesisService.getThesisById(entityId))));
        }

        var outputStream = new ByteArrayOutputStream();
        try (var writer = new OutputStreamWriter(outputStream, StandardCharsets.UTF_8)) {
            for (String record : exportedEntities) {
                writer.write(record);
                writer.write("\n");
            }
        } catch (IOException e) {
            throw new RuntimeException(e); // should never happen
        }

        return new InputStreamResource(new ByteArrayInputStream(outputStream.toByteArray()));
    }

    private Page<DocumentPublicationIndex> returnBulkDataFromDefinedEndpoint(
        ThesisCSVExportRequestDTO exportRequest) {
        if (exportRequest.getEndpointType().equals(ExportableEndpointType.THESIS_ADVANCED_SEARCH)) {
            return thesisSearchService.performAdvancedThesisSearch(
                exportRequest.getThesisSearchRequest(),
                PageRequest.of(exportRequest.getBulkExportOffset(), maximumExportAmount));
        }

        return thesisSearchService.performSimpleThesisSearch(exportRequest.getThesisSearchRequest(),
            PageRequest.of(exportRequest.getBulkExportOffset(), maximumExportAmount));
    }

    private String getBibliographicExportEntity(TableExportRequest request, Document document) {
        return switch (request.getExportFileType()) {
            case BIB -> StringUtil.bibTexEntryToString(
                DocumentPublicationConverter.toBibTeXEntry(document));
            case RIS -> DocumentPublicationConverter.toTaggedFormat(document, true);
            case ENW -> DocumentPublicationConverter.toTaggedFormat(document, false);
            default -> throw new IllegalStateException("Unexpected value: " +
                request.getExportFileType()); // should never happen
        };
    }
}
