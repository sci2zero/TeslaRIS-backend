package rs.teslaris.thesislibrary.service.impl;

import jakarta.annotation.Nullable;
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
import rs.teslaris.core.indexmodel.DocumentPublicationIndex;
import rs.teslaris.core.indexrepository.DocumentPublicationIndexRepository;
import rs.teslaris.core.model.commontypes.ExportableEndpointType;
import rs.teslaris.core.service.impl.TableExportHelper;
import rs.teslaris.core.service.interfaces.document.CitationService;
import rs.teslaris.core.service.interfaces.document.ThesisService;
import rs.teslaris.thesislibrary.dto.ThesisTableExportRequestDTO;
import rs.teslaris.thesislibrary.service.interfaces.ThesisLibraryTableExportService;
import rs.teslaris.thesislibrary.service.interfaces.ThesisSearchService;

@Service
@RequiredArgsConstructor
@Transactional
@Traceable
public class ThesisLibraryTableExportServiceImpl implements ThesisLibraryTableExportService {

    private final ThesisSearchService thesisSearchService;

    private final CitationService citationService;

    private final DocumentPublicationIndexRepository documentPublicationIndexRepository;

    private final ThesisService thesisService;

    @Value("${table-export.maximum-export-amount}")
    private Integer maximumExportAmount;


    @Override
    @Nullable
    public InputStreamResource exportThesesToCSV(ThesisTableExportRequestDTO request) {
        if (!List.of(ExportFileType.CSV, ExportFileType.XLSX)
            .contains(request.getExportFileType())) {
            return null;
        }

        String configurationFile = "thesisSearchFieldConfiguration.json";

        var rowsData = new ArrayList<List<String>>();
        var tableHeaders = TableExportHelper.getTableHeaders(request, configurationFile);
        TableExportHelper.addCitationColumns(tableHeaders, request);
        rowsData.add(tableHeaders);

        if (request.getExportMaxPossibleAmount()) {
            returnBulkDataFromDefinedEndpoint(request)
                .forEach(entity -> {
                    var rowData =
                        TableExportHelper.constructRowData(entity, request.getColumns(),
                            configurationFile,
                            request.getExportLanguage());
                    TableExportHelper.addCitationData(rowData, entity, request, citationService);
                    rowsData.add(rowData);
                });
        } else {
            request.getExportEntityIds().forEach(
                entityId -> documentPublicationIndexRepository.findDocumentPublicationIndexByDatabaseId(
                        entityId)
                    .ifPresent(entity -> {
                        var rowData =
                            TableExportHelper.constructRowData(entity, request.getColumns(),
                                configurationFile,
                                request.getExportLanguage());
                        TableExportHelper.addCitationData(rowData, entity, request,
                            citationService);
                        rowsData.add(rowData);
                    }));
        }

        return TableExportHelper.createExportFile(rowsData, request.getExportFileType());
    }

    @Override
    public InputStreamResource exportThesesToBibliographicFile(
        ThesisTableExportRequestDTO request) {
        if (!List.of(ExportFileType.BIB, ExportFileType.RIS, ExportFileType.ENW)
            .contains(request.getExportFileType())) {
            return null;
        }

        var exportedEntities = new ArrayList<String>();

        if (request.getExportMaxPossibleAmount()) {
            returnBulkDataFromDefinedEndpoint(request)
                .forEach(entity -> {
                    exportedEntities.add(
                        DocumentPublicationConverter.getBibliographicExportEntity(request,
                            thesisService.getThesisById(entity.getDatabaseId())));
                });
        } else {
            request.getExportEntityIds().forEach(entityId -> exportedEntities.add(
                DocumentPublicationConverter.getBibliographicExportEntity(request,
                    thesisService.getThesisById(entityId))));
        }

        return TableExportHelper.createExportFile(exportedEntities, request.getExportFileType());
    }

    private Page<DocumentPublicationIndex> returnBulkDataFromDefinedEndpoint(
        ThesisTableExportRequestDTO exportRequest) {
        if (exportRequest.getEndpointType().equals(ExportableEndpointType.THESIS_ADVANCED_SEARCH)) {
            return thesisSearchService.performAdvancedThesisSearch(
                exportRequest.getThesisSearchRequest(),
                PageRequest.of(exportRequest.getBulkExportOffset(), maximumExportAmount));
        }

        return thesisSearchService.performSimpleThesisSearch(exportRequest.getThesisSearchRequest(),
            PageRequest.of(exportRequest.getBulkExportOffset(), maximumExportAmount));
    }
}
