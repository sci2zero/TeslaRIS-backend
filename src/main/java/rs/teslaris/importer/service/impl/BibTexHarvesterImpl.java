package rs.teslaris.importer.service.impl;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.HashMap;
import java.util.Map;
import java.util.Objects;
import lombok.RequiredArgsConstructor;
import org.jbibtex.BibTeXDatabase;
import org.jbibtex.BibTeXEntry;
import org.jbibtex.BibTeXParser;
import org.jbibtex.Key;
import org.jbibtex.ParseException;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.query.Criteria;
import org.springframework.data.mongodb.core.query.Query;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;
import rs.teslaris.importer.model.common.DocumentImport;
import rs.teslaris.importer.model.converter.harvest.BibTexConverter;
import rs.teslaris.importer.service.interfaces.BibTexHarvester;

@Service
@RequiredArgsConstructor
public class BibTexHarvesterImpl implements BibTexHarvester {

    private final MongoTemplate mongoTemplate;


    @Override
    public HashMap<Integer, Integer> harvestDocumentsForAuthor(Integer userId,
                                                               MultipartFile bibTexFile,
                                                               HashMap<Integer, Integer> newEntriesCount) {
        var database = parseBibtexFile(bibTexFile);

        for (Map.Entry<Key, BibTeXEntry> entry : database.getEntries().entrySet()) {
            BibTeXEntry bibEntry = entry.getValue();

            var publication = BibTexConverter.toCommonImportModel(bibEntry);
            publication.ifPresent(documentImport -> {
                var existingImport = findExistingImport(documentImport.getIdentifier());
                if (Objects.nonNull(existingImport)) {
                    return;
                }

                documentImport.getImportUsersId().add(userId);
                mongoTemplate.save(documentImport, "documentImports");
                newEntriesCount.merge(userId, 1, Integer::sum);
            });
        }

        return newEntriesCount;
    }

    private BibTeXDatabase parseBibtexFile(MultipartFile file) {
        if (file.isEmpty()) {
            throw new IllegalArgumentException("File is empty.");
        }

        if (!Objects.requireNonNull(file.getOriginalFilename()).toLowerCase().endsWith(".bib")) {
            throw new IllegalArgumentException("Only .bib files are allowed.");
        }

        try (var reader = new BufferedReader(new InputStreamReader(file.getInputStream()))) {
            BibTeXParser bibtexParser = new BibTeXParser();
            BibTeXDatabase database = bibtexParser.parse(reader);

            if (Objects.isNull(database) || database.getEntries().isEmpty()) {
                throw new IllegalArgumentException("No valid entries found in the .bib file.");
            }

            return database;
        } catch (ParseException | IOException e) {
            throw new IllegalArgumentException("Failed to parse BibTeX file: " + e.getMessage(), e);
        }
    }

    private DocumentImport findExistingImport(String citationKey) {
        var query = new Query(Criteria.where("identifier").is(citationKey));
        return mongoTemplate.findOne(query, DocumentImport.class, "documentImports");
    }
}
