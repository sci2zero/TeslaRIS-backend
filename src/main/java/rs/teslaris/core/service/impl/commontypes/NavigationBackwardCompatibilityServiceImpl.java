package rs.teslaris.core.service.impl.commontypes;

import jakarta.annotation.Nullable;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import rs.teslaris.core.model.document.Dataset;
import rs.teslaris.core.model.document.JournalPublication;
import rs.teslaris.core.model.document.Monograph;
import rs.teslaris.core.model.document.MonographPublication;
import rs.teslaris.core.model.document.Patent;
import rs.teslaris.core.model.document.Proceedings;
import rs.teslaris.core.model.document.ProceedingsPublication;
import rs.teslaris.core.model.document.Software;
import rs.teslaris.core.model.document.Thesis;
import rs.teslaris.core.repository.document.DocumentRepository;
import rs.teslaris.core.repository.person.OrganisationUnitRepository;
import rs.teslaris.core.repository.person.PersonRepository;
import rs.teslaris.core.util.Pair;

@Service
@RequiredArgsConstructor
@Transactional
public class NavigationBackwardCompatibilityServiceImpl {

    private final PersonRepository personRepository;

    private final OrganisationUnitRepository organisationUnitRepository;

    private final DocumentRepository documentRepository;

    @Nullable
    public Pair<String, Integer> readResourceByOldId(Integer oldId) {
        var documentOpt = documentRepository.findDocumentByOldId(oldId);
        if (documentOpt.isPresent()) {
            var document = documentOpt.get();
            switch (document) {
                case JournalPublication ignored -> {
                    return new Pair<>("JOURNAL_PUBLICATION", document.getId());
                }
                case ProceedingsPublication ignored -> {
                    return new Pair<>("PROCEEDINGS_PUBLICATION", document.getId());
                }
                case MonographPublication ignored -> {
                    return new Pair<>("MONOGRAPH_PUBLICATION", document.getId());
                }
                case Monograph ignored -> {
                    return new Pair<>("MONOGRAPH", document.getId());
                }
                case Proceedings ignored -> {
                    return new Pair<>("PROCEEDINGS", document.getId());
                }
                case Thesis ignored -> {
                    return new Pair<>("THESIS", document.getId());
                }
                case Patent ignored -> {
                    return new Pair<>("PATENT", document.getId());
                }
                case Dataset ignored -> {
                    return new Pair<>("DATASET", document.getId());
                }
                case Software ignored -> {
                    return new Pair<>("SOFTWARE", document.getId());
                }
                default -> throw new IllegalStateException("Unexpected value: " + document);
            }
        }

        var personOpt = personRepository.findPersonByOldId(oldId);
        if (personOpt.isPresent()) {
            return new Pair<>("PERSON", personOpt.get().getId());
        }

        var orgUnitOpt = organisationUnitRepository.findOrganisationUnitByOldId(oldId);
        return orgUnitOpt.map(
                organisationUnit -> new Pair<>("ORGANISATION_UNIT", organisationUnit.getId()))
            .orElse(null);

    }
}
