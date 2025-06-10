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
import rs.teslaris.importer.model.common.DocumentImport;
import rs.teslaris.importer.model.common.MultilingualContent;
import rs.teslaris.importer.service.interfaces.EndNoteHarvester;
import rs.teslaris.importer.utility.taggedformats.TaggedBibliographicFormatUtility;

@Service
@Slf4j
public class EndNoteHarvesterImpl implements EndNoteHarvester {

    @Override
    public HashMap<Integer, Integer> harvestDocumentsForAuthor(Integer userId,
                                                               MultipartFile enwFile,
                                                               HashMap<Integer, Integer> newEntriesCount) {
        try (var reader = new BufferedReader(
            new InputStreamReader(enwFile.getInputStream(), StandardCharsets.UTF_8))) {
            DocumentImport document = null;
            List<String> affiliations = new ArrayList<>();
            List<String> keywords = new ArrayList<>();
            String line;

            while (Objects.nonNull((line = reader.readLine()))) {
                if (line.trim().isEmpty()) {
                    continue;
                }

                String tag = line.length() >= 2 ? line.substring(0, 2) : "";
                String content = line.length() > 3 ? line.substring(3).trim() : "";

                if ("%0".equals(tag)) {
                    // New reference starts
                    if (Objects.nonNull(document)) {
                        TaggedBibliographicFormatUtility.finalizeAndSaveDocument(document, userId,
                            newEntriesCount, affiliations, keywords);
                    }
                    document = TaggedBibliographicFormatUtility.parseDocumentType(content);
                    continue;
                }

                if (Objects.isNull(document)) {
                    continue;
                }

                switch (tag) {
                    case "%T" -> document.getTitle().add(new MultilingualContent("EN", content, 1));
                    case "%A" -> TaggedBibliographicFormatUtility.addAuthor(document, content);
                    case "%J" ->
                        document.getPublishedIn().add(new MultilingualContent("EN", content, 1));
                    case "%I" -> {
                        if (document.getPublishedIn().isEmpty()) {
                            TaggedBibliographicFormatUtility.handleSettingProceedingsAndEvent(
                                content, document);
                        }
                    }
                    case "%V" -> document.setVolume(content);
                    case "%N" -> document.setIssue(content);
                    case "%@" -> TaggedBibliographicFormatUtility.parseIssnIsbn(document, content);
                    case "%P" -> handlePageRange(content, document);
                    case "%D" -> TaggedBibliographicFormatUtility.handleDate(content, document);
                    case "%K" -> keywords.add(content);
                    case "%X" ->
                        document.getDescription().add(new MultilingualContent("EN", content, 1));
                    default -> log.info("Unknown EndNote tag {} with value: {}", tag, content);
                }
            }

            if (Objects.nonNull(document)) {
                TaggedBibliographicFormatUtility.finalizeAndSaveDocument(document, userId,
                    newEntriesCount, affiliations, keywords);
            }

            return newEntriesCount;
        } catch (IOException e) {
            throw new UncheckedIOException("Failed to read EndNote (.enw) file", e);
        }
    }

    private void handlePageRange(String pageField, DocumentImport document) {
        String[] parts = pageField.split("-");

        if (parts.length == 2) {
            document.setStartPage(parts[0].trim());
            document.setEndPage(parts[1].trim());
            try {
                int pages =
                    Integer.parseInt(parts[1].trim()) - Integer.parseInt(parts[0].trim()) + 1;
                document.setNumberOfPages(pages);
            } catch (Exception ignored) {
            }
        } else {
            document.setStartPage(pageField.trim());
        }
    }
}
