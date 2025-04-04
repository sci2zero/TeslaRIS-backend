package rs.teslaris.thesislibrary.service.impl;

import java.util.ArrayList;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.InputStreamResource;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import rs.teslaris.core.indexmodel.DocumentPublicationIndex;
import rs.teslaris.core.indexrepository.DocumentPublicationIndexRepository;
import rs.teslaris.core.model.commontypes.ExportableEndpointType;
import rs.teslaris.core.service.impl.CSVExportHelper;
import rs.teslaris.core.service.interfaces.document.CitationService;
import rs.teslaris.thesislibrary.dto.ThesisCSVExportRequestDTO;
import rs.teslaris.thesislibrary.service.interfaces.ThesisLibraryCSVExportService;
import rs.teslaris.thesislibrary.service.interfaces.ThesisSearchService;

@Service
@RequiredArgsConstructor
@Transactional
public class ThesisLibraryCSVExportServiceImpl implements ThesisLibraryCSVExportService {

    private final ThesisSearchService thesisSearchService;

    private final CitationService citationService;

    private final DocumentPublicationIndexRepository documentPublicationIndexRepository;

    @Value("${csv-export.maximum-export-amount}")
    private Integer maximumExportAmount;


    @Override
    public InputStreamResource exportThesesToCSV(ThesisCSVExportRequestDTO request) {
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
}
