package rs.teslaris.importer.service.impl;

import java.time.LocalDate;
import java.util.HashMap;
import java.util.Objects;
import lombok.RequiredArgsConstructor;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.stereotype.Service;
import rs.teslaris.core.util.deduplication.DeduplicationUtil;
import rs.teslaris.importer.model.converter.harvest.OpenAlexConverter;
import rs.teslaris.importer.service.interfaces.OpenAlexHarvester;
import rs.teslaris.importer.utility.CommonImportUtility;
import rs.teslaris.importer.utility.openalex.OpenAlexImportUtility;

@Service
@RequiredArgsConstructor
public class OpenAlexHarvesterImpl implements OpenAlexHarvester {

    private final OpenAlexImportUtility openAlexImportUtility;

    private final MongoTemplate mongoTemplate;


    @Override
    public HashMap<Integer, Integer> harvestDocumentsForAuthor(Integer userId, LocalDate startDate,
                                                               LocalDate endDate,
                                                               HashMap<Integer, Integer> newEntriesCount) {


        openAlexImportUtility.getPublicationsForAuthor("A5070362523", startDate.toString(),
            endDate.toString(), false).forEach(
            publication -> OpenAlexConverter.toCommonImportModel(publication)
                .ifPresent(documentImport -> {
                    var existingImport =
                        CommonImportUtility.findExistingImport(documentImport.getIdentifier());
                    if (Objects.isNull(existingImport) &&
                        Objects.nonNull(documentImport.getDoi())) {
                        if (Objects.nonNull(
                            CommonImportUtility.findImportByDOI(documentImport.getDoi()))) {
                            // Probably imported before from scopus, which has higher priority (for now)
                            return;
                        }
                    }

                    var embedding = CommonImportUtility.generateEmbedding(documentImport);
                    if (DeduplicationUtil.isDuplicate(existingImport, embedding)) {
                        return;
                    }

                    if (Objects.nonNull(embedding)) {
                        documentImport.setEmbedding(embedding.toFloatVector());
                    }

                    documentImport.getImportUsersId().add(userId);
                    mongoTemplate.save(documentImport, "documentImports");
                    newEntriesCount.merge(userId, 1, Integer::sum);
                }));
        return newEntriesCount;
    }

    @Override
    public HashMap<Integer, Integer> harvestDocumentsForInstitutionalEmployee(Integer userId,
                                                                              Integer institutionId,
                                                                              LocalDate startDate,
                                                                              LocalDate endDate,
                                                                              HashMap<Integer, Integer> newEntriesCount) {
        return null;
    }
}
