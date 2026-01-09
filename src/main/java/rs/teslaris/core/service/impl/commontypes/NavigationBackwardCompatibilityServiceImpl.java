package rs.teslaris.core.service.impl.commontypes;

import jakarta.annotation.Nullable;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import rs.teslaris.core.annotation.Traceable;
import rs.teslaris.core.model.document.Dataset;
import rs.teslaris.core.model.document.GeneticMaterial;
import rs.teslaris.core.model.document.JournalPublication;
import rs.teslaris.core.model.document.MaterialProduct;
import rs.teslaris.core.model.document.Monograph;
import rs.teslaris.core.model.document.MonographPublication;
import rs.teslaris.core.model.document.Patent;
import rs.teslaris.core.model.document.Proceedings;
import rs.teslaris.core.model.document.ProceedingsPublication;
import rs.teslaris.core.model.document.Software;
import rs.teslaris.core.model.document.Thesis;
import rs.teslaris.core.repository.document.BookSeriesRepository;
import rs.teslaris.core.repository.document.DocumentFileRepository;
import rs.teslaris.core.repository.document.DocumentRepository;
import rs.teslaris.core.repository.document.JournalRepository;
import rs.teslaris.core.repository.institution.OrganisationUnitRepository;
import rs.teslaris.core.repository.person.PersonRepository;
import rs.teslaris.core.service.interfaces.commontypes.NavigationBackwardCompatibilityService;
import rs.teslaris.core.util.functional.Pair;

@Service
@RequiredArgsConstructor
@Transactional
@Slf4j
@Traceable
public class NavigationBackwardCompatibilityServiceImpl implements
    NavigationBackwardCompatibilityService {

    private final PersonRepository personRepository;

    private final OrganisationUnitRepository organisationUnitRepository;

    private final DocumentRepository documentRepository;

    private final JournalRepository journalRepository;

    private final BookSeriesRepository bookSeriesRepository;

    private final DocumentFileRepository documentFileRepository;


    @Nullable
    public Pair<String, Integer> readResourceByOldId(Integer oldId, String source,
                                                     String language) {
        var documentIdOpt = documentRepository.findDocumentByOldIdsContains(oldId);
        if (documentIdOpt.isPresent()) {
            var documentId = documentIdOpt.get();
            var documentOpt = documentRepository.findById(documentId);

            if (documentOpt.isEmpty()) {
                documentIdOpt = documentRepository.findDocumentByMergedIdsContains(documentId);
                if (documentIdOpt.isEmpty()) {
                    return new Pair<>("DELETED", documentId);
                }

                documentOpt = documentRepository.findById(documentId);
            }

            var document = documentOpt.get();

            switch (document) {
                case JournalPublication ignored -> {
                    log.info("NAVIGATION SUCCESS - JOURNAL_PUBLICATION {} from {} in LANGUAGE {}",
                        document.getId(), source, language);
                    return new Pair<>("JOURNAL_PUBLICATION", document.getId());
                }
                case ProceedingsPublication ignored -> {
                    log.info(
                        "NAVIGATION SUCCESS - PROCEEDINGS_PUBLICATION {} from {} in LANGUAGE {}",
                        document.getId(), source, language);
                    return new Pair<>("PROCEEDINGS_PUBLICATION", document.getId());
                }
                case MonographPublication ignored -> {
                    log.info("NAVIGATION SUCCESS - MONOGRAPH_PUBLICATION {} from {} in LANGUAGE {}",
                        document.getId(), source, language);
                    return new Pair<>("MONOGRAPH_PUBLICATION", document.getId());
                }
                case Monograph ignored -> {
                    log.info("NAVIGATION SUCCESS - MONOGRAPH {} from {} in LANGUAGE {}",
                        document.getId(), source, language);
                    return new Pair<>("MONOGRAPH", document.getId());
                }
                case Proceedings ignored -> {
                    log.info("NAVIGATION SUCCESS - PROCEEDINGS {} from {} in LANGUAGE {}", document,
                        source, language);
                    return new Pair<>("PROCEEDINGS", document.getId());
                }
                case Thesis ignored -> {
                    log.info("NAVIGATION SUCCESS - THESIS {} from {} in LANGUAGE {}", document,
                        source, language);
                    return new Pair<>("THESIS", document.getId());
                }
                case Patent ignored -> {
                    log.info("NAVIGATION SUCCESS - PATENT {} from {} in LANGUAGE {}",
                        document.getId(), source, language);
                    return new Pair<>("PATENT", document.getId());
                }
                case Dataset ignored -> {
                    log.info("NAVIGATION SUCCESS - DATASET {} from {} in LANGUAGE {}",
                        document.getId(), source, language);
                    return new Pair<>("DATASET", document.getId());
                }
                case Software ignored -> {
                    log.info("NAVIGATION SUCCESS - SOFTWARE {} from {} in LANGUAGE {}",
                        document.getId(), source, language);
                    return new Pair<>("SOFTWARE", document.getId());
                }
                case MaterialProduct ignored -> {
                    log.info("NAVIGATION SUCCESS - MATERIAL_PRODUCT {} from {} in LANGUAGE {}",
                        document.getId(), source, language);
                    return new Pair<>("MATERIAL_PRODUCT", document.getId());
                }
                case GeneticMaterial ignored -> {
                    log.info("NAVIGATION SUCCESS - GENETIC_MATERIAL {} from {} in LANGUAGE {}",
                        document.getId(), source, language);
                    return new Pair<>("GENETIC_MATERIAL", document.getId());
                }
                default -> throw new IllegalStateException("Unexpected value: " + document);
            }
        }

        var journalOpt = journalRepository.findByOldIdsContains(oldId);
        if (journalOpt.isPresent()) {
            log.info("NAVIGATION SUCCESS - JOURNAL {} from {} in LANGUAGE {}",
                journalOpt.get().getId(), source, language);
            return new Pair<>("JOURNAL", journalOpt.get().getId());
        }

        var bookSeriesOpt = bookSeriesRepository.findBookSeriesByOldIdsContains(oldId);
        if (bookSeriesOpt.isPresent()) {
            log.info("NAVIGATION SUCCESS - BOOK_SERIES {} from {} in LANGUAGE {}",
                bookSeriesOpt.get().getId(), source, language);
            return new Pair<>("BOOK_SERIES", bookSeriesOpt.get().getId());
        }

        var personOpt = personRepository.findPersonByOldIdsContains(oldId);
        if (personOpt.isPresent()) {
            log.info("NAVIGATION SUCCESS - PERSON {} from {} in LANGUAGE {}",
                personOpt.get().getId(), source, language);
            return new Pair<>("PERSON", personOpt.get().getId());
        }

        var orgUnitOpt = organisationUnitRepository.findOrganisationUnitByOldIdsContains(oldId);
        if (orgUnitOpt.isPresent()) {
            log.info("NAVIGATION SUCCESS - ORGANISATION_UNIT {} from {} in LANGUAGE {}",
                orgUnitOpt.get().getId(), source, language);
            return new Pair<>("ORGANISATION_UNIT", orgUnitOpt.get().getId());
        }

        log.info("NAVIGATION FAILED - RESOURCE with legacy ID {} from {} in LANGUAGE {}", oldId,
            source, language);

        return new Pair<>("NOT_FOUND", -1);
    }

    @Override
    public Pair<String, String> readDocumentFileByOldId(String oldServerFilename, String source,
                                                        String language) {
        var documentFileOpt =
            documentFileRepository.findDocumentFileByLegacyFilename(oldServerFilename);
        if (documentFileOpt.isPresent()) {
            log.info("NAVIGATION SUCCESS - DOCUMENT_FILE {} from {} in LANGUAGE {}",
                documentFileOpt.get().getId(), source, language);
            return new Pair<>(documentFileOpt.get().getServerFilename(),
                documentFileOpt.get().getFilename());
        }

        log.info("NAVIGATION FAILED - DOCUMENT FILE with legacy FILENAME {} from {} in LANGUAGE {}",
            oldServerFilename, source, language);

        return new Pair<>("NOT_FOUND", "");
    }
}
