package rs.teslaris.exporter.service.impl;

import com.fasterxml.jackson.databind.ObjectMapper;
import java.io.IOException;
import java.io.OutputStream;
import java.util.Objects;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import rs.teslaris.core.model.commontypes.ApproveStatus;
import rs.teslaris.core.model.document.AccessRights;
import rs.teslaris.core.model.document.Dataset;
import rs.teslaris.core.model.document.Document;
import rs.teslaris.core.model.document.JournalPublication;
import rs.teslaris.core.model.document.Monograph;
import rs.teslaris.core.model.document.MonographPublication;
import rs.teslaris.core.model.document.Patent;
import rs.teslaris.core.model.document.Proceedings;
import rs.teslaris.core.model.document.ProceedingsPublication;
import rs.teslaris.core.model.document.Software;
import rs.teslaris.core.model.document.Thesis;
import rs.teslaris.core.model.rocrate.ContextualEntity;
import rs.teslaris.core.model.rocrate.RoCrate;
import rs.teslaris.core.service.interfaces.document.DocumentPublicationService;
import rs.teslaris.core.service.interfaces.document.FileService;
import rs.teslaris.core.util.exceptionhandling.exception.LoadingException;
import rs.teslaris.core.util.search.StringUtil;
import rs.teslaris.exporter.model.converter.RoCrateConverter;
import rs.teslaris.exporter.service.interfaces.RoCrateExportService;
import rs.teslaris.exporter.util.rocrate.Json2HtmlTable;

@Service
@RequiredArgsConstructor
@Slf4j
public class RoCrateExportServiceImpl implements RoCrateExportService {

    private final DocumentPublicationService documentPublicationService;

    private final FileService fileService;

    private final ObjectMapper objectMapper;

    @Value("${export.internal-identifier.prefix}")
    private String identifierPrefix;


    @Override
    @Transactional(readOnly = true)
    public void createRoCrateZip(Integer documentId, OutputStream outputStream) {
        try {
            var document = fetchDocumentForPacking(documentId);
            if (Objects.isNull(document)) {
                return;
            }

            var roCrate = createMetadataInfo(document);

            var metadataJsonBytes = objectMapper
                .writerWithDefaultPrettyPrinter()
                .writeValueAsBytes(roCrate);

            var htmlPreview = Json2HtmlTable.toHtmlTable(objectMapper.readTree(
                objectMapper.writerWithDefaultPrettyPrinter()
                    .writeValueAsString(roCrate.getGraph()))).getBytes();

            try (var zipOut = new ZipOutputStream(outputStream)) {
                var metadataEntry = new ZipEntry("ro-crate-metadata.json");
                zipOut.putNextEntry(metadataEntry);
                zipOut.write(metadataJsonBytes);
                zipOut.closeEntry();

                var previewEntry = new ZipEntry("ro-crate-preview.html");
                zipOut.putNextEntry(previewEntry);
                zipOut.write(htmlPreview);
                zipOut.closeEntry();

                for (var file : document.getFileItems()) {
                    if (!file.getApproveStatus().equals(ApproveStatus.APPROVED) ||
                        !file.getAccessRights().equals(AccessRights.OPEN_ACCESS)) {
                        continue;
                    }

                    var entry = new ZipEntry("data/" + file.getFilename());
                    zipOut.putNextEntry(entry);

                    try (var resource = fileService.loadAsResource(file.getServerFilename())) {
                        resource.transferTo(zipOut);
                    }

                    zipOut.closeEntry();
                }

                zipOut.finish();
            } catch (IOException e) {
                throw new RuntimeException(e); // will be caught in outer scope
            }
        } catch (IOException e) {
            throw new LoadingException("Failed to create RO-Crate ZIP. Reason: " + e.getMessage());
        }
    }

    private RoCrate createMetadataInfo(Document document) {
        var metadataInfo = new RoCrate();
        addRequiredMetadataDescriptor(metadataInfo);

        var documentIdentifier = constructDocumentIdentifier(document);

        switch (document) {
            case Dataset dataset -> metadataInfo.getGraph().add(
                RoCrateConverter.toRoCrateModel(dataset, documentIdentifier,
                    metadataInfo));
            case JournalPublication journalPublication -> metadataInfo.getGraph().add(
                RoCrateConverter.toRoCrateModel(journalPublication, documentIdentifier,
                    metadataInfo));
            case ProceedingsPublication proceedingsPublication -> metadataInfo.getGraph().add(
                RoCrateConverter.toRoCrateModel(proceedingsPublication,
                    documentIdentifier,
                    metadataInfo));
            case Proceedings proceedings -> metadataInfo.getGraph().add(
                RoCrateConverter.toRoCrateModel(proceedings,
                    documentIdentifier,
                    metadataInfo));
            case Monograph monograph -> metadataInfo.getGraph().add(
                RoCrateConverter.toRoCrateModel(monograph,
                    documentIdentifier,
                    metadataInfo));
            case MonographPublication monographPublication -> metadataInfo.getGraph().add(
                RoCrateConverter.toRoCrateModel(monographPublication,
                    documentIdentifier,
                    metadataInfo));
            case Software software -> metadataInfo.getGraph().add(
                RoCrateConverter.toRoCrateModel(software,
                    documentIdentifier,
                    metadataInfo));
            case Patent patent -> metadataInfo.getGraph().add(
                RoCrateConverter.toRoCrateModel(patent,
                    documentIdentifier,
                    metadataInfo));
            case Thesis thesis -> metadataInfo.getGraph().add(
                RoCrateConverter.toRoCrateModel(thesis,
                    documentIdentifier,
                    metadataInfo));
            default -> log.error("Unexpected document type: {}. ID: {}.", document.getClass(),
                document.getId()); // Should never happen
        }

        metadataInfo.setGraph(metadataInfo.getGraph().reversed());
        return metadataInfo;
    }

    public Document fetchDocumentForPacking(Integer documentId) {
        return documentPublicationService.findDocumentById(documentId);
    }

    public String constructDocumentIdentifier(Document document) {
        if (StringUtil.valueExists(document.getDoi())) {
            return "https://doi.org/" + document.getDoi();
        } else if (StringUtil.valueExists(document.getScopusId())) {
            return "https://www.scopus.com/pages/publications/" + document.getScopusId();
        } else if (StringUtil.valueExists(document.getOpenAlexId())) {
            return "https://openalex.org/" + document.getOpenAlexId();
        }
        // TODO: WOS???

        return identifierPrefix + document.getId();
    }

    private void addRequiredMetadataDescriptor(RoCrate metadataInfo) {
        metadataInfo.getGraph().add(
            new ContextualEntity("ro-crate-metadata.json", "CreativeWork",
                new ContextualEntity("https://w3id.org/ro/crate/1.2"),
                new ContextualEntity("./")
            )
        );
    }
}
