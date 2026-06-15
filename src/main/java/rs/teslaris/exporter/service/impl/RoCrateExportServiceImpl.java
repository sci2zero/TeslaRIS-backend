package rs.teslaris.exporter.service.impl;

import com.fasterxml.jackson.databind.ObjectMapper;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Locale;
import java.util.Objects;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.MessageSource;
import org.springframework.core.io.ClassPathResource;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StreamUtils;
import rs.teslaris.core.indexrepository.DocumentPublicationIndexRepository;
import rs.teslaris.core.model.commontypes.ApproveStatus;
import rs.teslaris.core.model.document.AccessRights;
import rs.teslaris.core.model.document.Dataset;
import rs.teslaris.core.model.document.Document;
import rs.teslaris.core.model.document.GeneticMaterial;
import rs.teslaris.core.model.document.IntangibleProduct;
import rs.teslaris.core.model.document.JournalPublication;
import rs.teslaris.core.model.document.MaterialProduct;
import rs.teslaris.core.model.document.Monograph;
import rs.teslaris.core.model.document.MonographPublication;
import rs.teslaris.core.model.document.Patent;
import rs.teslaris.core.model.document.PerformanceRelatedOutput;
import rs.teslaris.core.model.document.Proceedings;
import rs.teslaris.core.model.document.ProceedingsPublication;
import rs.teslaris.core.model.document.Thesis;
import rs.teslaris.core.model.person.Person;
import rs.teslaris.core.model.rocrate.ContextualEntity;
import rs.teslaris.core.model.rocrate.RoCrate;
import rs.teslaris.core.model.rocrate.RoCratePublicationBase;
import rs.teslaris.core.service.interfaces.commontypes.ProgressService;
import rs.teslaris.core.service.interfaces.document.DocumentLookupService;
import rs.teslaris.core.service.interfaces.document.FileService;
import rs.teslaris.core.service.interfaces.person.PersonService;
import rs.teslaris.core.util.exceptionhandling.exception.LoadingException;
import rs.teslaris.core.util.language.LanguageAbbreviations;
import rs.teslaris.core.util.search.StringUtil;
import rs.teslaris.core.util.session.SessionUtil;
import rs.teslaris.exporter.model.converter.RoCrateConverter;
import rs.teslaris.exporter.service.interfaces.RoCrateExportService;
import rs.teslaris.exporter.util.rocrate.Json2HtmlTable;

@Service
@RequiredArgsConstructor
@Slf4j
public class RoCrateExportServiceImpl implements RoCrateExportService {

    private final DocumentLookupService documentLookupService;

    private final FileService fileService;

    private final ProgressService progressService;

    private final DocumentPublicationIndexRepository documentPublicationIndexRepository;

    private final PersonService personService;

    private final MessageSource messageSource;

    private final ObjectMapper objectMapper;

    String previewTableStyles;

    {
        var styles = "";
        try {
            var resource = new ClassPathResource("export/roCratePreviewStyles.html");
            if (resource.exists()) {
                styles =
                    StreamUtils.copyToString(resource.getInputStream(), StandardCharsets.UTF_8);
            }
        } catch (IOException e) {
            log.error("Error reading Ro-Crate style file: {}", e.getMessage());
        }
        previewTableStyles = styles;
    }

    @Override
    @Transactional(readOnly = true)
    public Path createRoCrateZip(Integer documentId, String exportId) {
        Path tempFile;

        try {
            tempFile = Files.createTempFile("ro-crate-" + documentId + "-", ".zip");

            progressService.send(exportId, 5,
                getStatusMessage("statusMessage.fetchingDocument"));

            var documentIndex =
                documentPublicationIndexRepository.findDocumentPublicationIndexByDatabaseId(
                    documentId);

            if (documentIndex.isEmpty()) {
                progressService.send(exportId, 100,
                    getStatusMessage("statusMessage.notFound"));
                progressService.complete(exportId);
                return tempFile;
            }

            var document = documentLookupService.fastDocumentLookup(documentIndex.get());

            progressService.send(exportId, 15,
                getStatusMessage("statusMessage.creatingMetadata"));

            var roCrate = new RoCrate();
            populateMetadataInfo(document, roCrate);
            addRequiredMetadataDescriptor(roCrate, null);

            var metadataJsonBytes = objectMapper
                .writerWithDefaultPrettyPrinter()
                .writeValueAsBytes(roCrate);

            progressService.send(exportId, 25,
                getStatusMessage("statusMessage.generatingPreview"));

            var htmlPreview = ("<body><div class=\"json-view\">" +
                Json2HtmlTable.toHtmlTable(
                    objectMapper.readTree(
                        objectMapper.writeValueAsString(roCrate.getGraph())))
                + "</div>" + previewTableStyles + "</body>").getBytes();

            try (var zipOut = new ZipOutputStream(
                Files.newOutputStream(tempFile))) {

                zipOut.putNextEntry(new ZipEntry("ro-crate-metadata.json"));
                zipOut.write(metadataJsonBytes);
                zipOut.closeEntry();

                zipOut.putNextEntry(new ZipEntry("ro-crate-preview.html"));
                zipOut.write(htmlPreview);
                zipOut.closeEntry();

                var files = document.getFileItems().stream()
                    .filter(f -> f.getApproveStatus() == ApproveStatus.APPROVED)
                    .filter(f -> f.getAccessRights() == AccessRights.OPEN_ACCESS)
                    .toList();

                int total = files.size();
                int index = 0;

                for (var file : files) {
                    index++;

                    progressService.send(
                        exportId,
                        30 + (index * 60 / total),
                        getStatusMessage("statusMessage.zippingFile")
                            + " " + index + "/" + total
                    );

                    zipOut.putNextEntry(new ZipEntry("data/" + file.getFilename()));

                    try (var resource = fileService.loadAsResource(file.getServerFilename())) {
                        resource.transferTo(zipOut);
                    }

                    zipOut.closeEntry();
                }

                zipOut.finish();
            }

            progressService.send(exportId, 100,
                getStatusMessage("statusMessage.completed"));
            progressService.complete(exportId);

            return tempFile;

        } catch (Exception e) {
            progressService.send(exportId, 100,
                getStatusMessage("statusMessage.failed"));
            progressService.complete(exportId);

            throw new LoadingException(
                "Failed to create RO-Crate ZIP. Reason: " + e.getMessage());
        }
    }

    @Override
    @Transactional(readOnly = true)
    public Path createRoCrateBibliographyZip(Integer personId, String exportId) {
        Path tempFile;

        try {
            tempFile = Files.createTempFile(
                "ro-crate-bibliography-" + personId + "-", ".zip");

            var pageNumber = 0;
            var chunkSize = 500;
            var hasNextPage = true;

            var roCrate = new RoCrate();

            while (hasNextPage) {
                var chunk = documentPublicationIndexRepository.findByAuthorIds(personId,
                    PageRequest.of(pageNumber, chunkSize,
                        Sort.by(Sort.Direction.ASC, "databaseId")));

                chunk.forEach(documentIndex -> {
                    progressService.send(
                        exportId,
                        5,
                        getStatusMessage("statusMessage.creatingMetadata")
                            + " (" + documentIndex.getTitleOther() + ")"
                    );

                    var document =
                        documentLookupService.fastDocumentLookup(documentIndex);

                    if (Objects.nonNull(document)) {
                        populateMetadataInfo(document, roCrate);
                    }
                });

                pageNumber++;
                hasNextPage = chunk.hasNext();
            }

            addRequiredMetadataDescriptor(
                roCrate,
                personService.findOne(personId));

            var metadataJsonBytes = objectMapper
                .writerWithDefaultPrettyPrinter()
                .writeValueAsBytes(roCrate);

            var htmlPreview =
                ("<body><div class=\"json-view\">" +
                    Json2HtmlTable.toHtmlTable(
                        objectMapper.readTree(
                            objectMapper.writeValueAsString(
                                roCrate.getGraph())))
                    + "</div>" + previewTableStyles +
                    "</body>").getBytes();

            try (var zipOut = new ZipOutputStream(Files.newOutputStream(tempFile))) {
                zipOut.putNextEntry(
                    new ZipEntry("ro-crate-metadata.json"));
                zipOut.write(metadataJsonBytes);
                zipOut.closeEntry();

                zipOut.putNextEntry(
                    new ZipEntry("ro-crate-preview.html"));
                zipOut.write(htmlPreview);
                zipOut.closeEntry();

                zipOut.finish();
            }

            progressService.send(exportId, 100,
                getStatusMessage("statusMessage.completed"));
            progressService.complete(exportId);

            return tempFile;

        } catch (Exception e) {
            progressService.send(exportId, 100,
                getStatusMessage("statusMessage.failed"));
            progressService.complete(exportId);

            throw new LoadingException(
                "Failed to create RO-Crate ZIP. Reason: "
                    + e.getMessage());
        }
    }

    private void populateMetadataInfo(Document document, RoCrate metadataInfo) {
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
            case IntangibleProduct intangibleProduct -> metadataInfo.getGraph().add(
                RoCrateConverter.toRoCrateModel(intangibleProduct,
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
            case MaterialProduct materialProduct -> metadataInfo.getGraph().add(
                RoCrateConverter.toRoCrateModel(materialProduct,
                    documentIdentifier,
                    metadataInfo));
            case GeneticMaterial geneticMaterial -> metadataInfo.getGraph().add(
                RoCrateConverter.toRoCrateModel(geneticMaterial,
                    documentIdentifier,
                    metadataInfo));
            case PerformanceRelatedOutput performanceRelatedOutput -> metadataInfo.getGraph().add(
                RoCrateConverter.toRoCrateModel(performanceRelatedOutput,
                    documentIdentifier,
                    metadataInfo));
            default -> log.error("Unexpected document type: {}. ID: {}.", document.getClass(),
                document.getId()); // Should never happen
        }
    }

    public String constructDocumentIdentifier(Document document) {
        if (StringUtil.valueExists(document.getDoi())) {
            return "https://doi.org/" + document.getDoi();
        } else if (StringUtil.valueExists(document.getScopusId())) {
            return "https://www.scopus.com/pages/publications/" + document.getScopusId();
        } else if (StringUtil.valueExists(document.getOpenAlexId())) {
            return "https://openalex.org/" + document.getOpenAlexId();
        }

        if (document instanceof Proceedings) {
            return RoCrateConverter.baseUrl + "en/proceedings/" + document.getId();
        }

        return RoCrateConverter.baseUrl + "en/scientific-results/DOC_TYPE/" + document.getId();
    }

    private void addRequiredMetadataDescriptor(RoCrate metadataInfo, Person person) {
        if (Objects.nonNull(person)) {
            var metadataDescriptor = new ContextualEntity("ro-crate-metadata.json", "Dataset",
                new ContextualEntity("https://w3id.org/ro/crate/1.2"),
                new ContextualEntity("./")
            );
            metadataDescriptor.setName("Bibliography of " + person.getName().toText());
            metadataDescriptor.setCreator(
                new ContextualEntity(RoCrateConverter.getPersonIdentifier(person)));
            metadataDescriptor.setHasPart(metadataInfo.getGraph().stream()
                .filter(node -> node instanceof RoCratePublicationBase)
                .map(node -> new ContextualEntity(((RoCratePublicationBase) node).getId()))
                .toList());

            metadataInfo.getGraph().add(metadataDescriptor);
        } else {
            metadataInfo.getGraph().add(
                new ContextualEntity("ro-crate-metadata.json", "CreativeWork",
                    new ContextualEntity("https://w3id.org/ro/crate/1.2"),
                    new ContextualEntity("./")
                )
            );
        }

        metadataInfo.reverseGraph();
    }

    private String getStatusMessage(String messageCode) {
        var loggedInUser = SessionUtil.getLoggedInUser();

        return messageSource.getMessage(
            messageCode,
            new Object[] {},
            Locale.forLanguageTag(
                Objects.nonNull(loggedInUser) ?
                    loggedInUser.getPreferredUILanguage().getLanguageTag().toLowerCase() :
                    LanguageAbbreviations.ENGLISH.toLowerCase())
        );
    }
}
