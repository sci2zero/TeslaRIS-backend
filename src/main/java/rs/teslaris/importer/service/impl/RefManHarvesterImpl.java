package rs.teslaris.importer.service.impl;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.UncheckedIOException;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Objects;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;
import rs.teslaris.core.indexmodel.DocumentPublicationType;
import rs.teslaris.importer.model.common.DocumentImport;
import rs.teslaris.importer.model.common.MultilingualContent;
import rs.teslaris.importer.service.interfaces.RefManHarvester;
import rs.teslaris.importer.utility.taggedformats.TaggedBibliographicFormatUtility;

@Service
@Slf4j
public class RefManHarvesterImpl implements RefManHarvester {

    @Override
    public HashMap<Integer, Integer> harvestDocumentsForAuthor(Integer userId,
                                                               MultipartFile risFile,
                                                               HashMap<Integer, Integer> newEntriesCount) {
        try (var reader = new BufferedReader(
            new InputStreamReader(risFile.getInputStream(), StandardCharsets.UTF_8))) {
            DocumentImport document = null;
            List<String> affiliations = new ArrayList<>();
            List<String> keywords = new ArrayList<>();
            String line;

            while (Objects.nonNull((line = reader.readLine()))) {
                if (line.trim().isEmpty()) {
                    continue;
                }

                String tag = line.length() >= 6 ? line.substring(0, 2) : "";
                String content = line.length() >= 6 ? line.substring(6).trim() : "";

                if (Objects.nonNull(document) && ("ER" .equals(tag) || tag.isBlank())) {
                    TaggedBibliographicFormatUtility.finalizeAndSaveDocument(document, userId,
                        newEntriesCount,
                        affiliations, keywords);
                    document = null;
                    continue;
                }

                if ("TY" .equals(tag)) {
                    document = TaggedBibliographicFormatUtility.parseDocumentType(content);
                    continue;
                }

                if (Objects.isNull(document)) {
                    continue;
                }

                switch (tag) {
                    case "T1", "TI" ->
                        document.getTitle().add(new MultilingualContent("EN", content, 1));
                    case "A1", "AU", "A2" ->
                        TaggedBibliographicFormatUtility.addAuthor(document, content);
                    case "JO" -> handleJoTag(document, content);
                    case "PB", "N1" -> {
                        if (document.getPublishedIn().isEmpty()) {
                            TaggedBibliographicFormatUtility.handleSettingProceedingsAndEvent(
                                content, document);
                        }
                    }
                    case "VL" -> document.setVolume(content);
                    case "IS" -> document.setIssue(content);
                    case "DO" -> document.setDoi(content);
                    case "UR" -> document.getUris().add(content);
                    case "SP" -> document.setStartPage(content);
                    case "EP" -> TaggedBibliographicFormatUtility.handleEndPage(document, content);
                    case "SN" -> TaggedBibliographicFormatUtility.parseIssnIsbn(document, content);
                    case "Y1", "PY" ->
                        TaggedBibliographicFormatUtility.handleDate(content, document);
                    case "AD" -> affiliations.add(content);
                    case "KW" -> keywords.add(content);
                    case "AB" ->
                        document.getDescription().add(new MultilingualContent("EN", content, 1));
                    default -> log.info("Encountered unknown tag {} with value: {}", tag, content);
                }
            }

            return newEntriesCount;
        } catch (IOException e) {
            throw new UncheckedIOException("Failed to read RIS file", e); // should never happen
        }
    }

    private void handleJoTag(DocumentImport document, String content) {
        if (DocumentPublicationType.PROCEEDINGS_PUBLICATION.equals(document.getPublicationType())) {
            TaggedBibliographicFormatUtility.handleSettingProceedingsAndEvent(content, document);
        } else {
            document.getPublishedIn().add(new MultilingualContent("EN", content, 1));
        }
    }
}
