package rs.teslaris.assessment.service.impl.classification;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import rs.teslaris.assessment.model.classification.DocumentAssessmentClassification;
import rs.teslaris.assessment.repository.classification.DocumentAssessmentClassificationRepository;
import rs.teslaris.assessment.service.interfaces.classification.PublicationClassificationService;
import rs.teslaris.core.indexmodel.DocumentPublicationIndex;
import rs.teslaris.core.indexmodel.DocumentPublicationType;
import rs.teslaris.core.model.institution.Commission;
import rs.teslaris.core.util.functional.QuadConsumer;

@Service
@RequiredArgsConstructor
@Slf4j
public class PublicationClassificationServiceImpl implements PublicationClassificationService {

    private final DocumentAssessmentClassificationRepository
        documentAssessmentClassificationRepository;


    @Override
    @Transactional
    public void classifyPublicationsChunk(List<DocumentPublicationIndex> chunk,
                                          Commission presetCommission,
                                          QuadConsumer<DocumentPublicationIndex, Integer, Commission, ArrayList<DocumentAssessmentClassification>> assessFunction,
                                          ArrayList<DocumentAssessmentClassification> batchClassifications) {
        log.info("Assessing {} documents in chunk.", chunk.size());

        chunk.forEach(publicationIndex -> {
            log.info("Assessing publication with ID {}", publicationIndex.getDatabaseId());

            if (publicationIndex.getType().equals(DocumentPublicationType.THESIS.name()) &&
                Objects.isNull(publicationIndex.getThesisDefenceDate())) {
                return;
            }

            if (Objects.nonNull(presetCommission)) {
                assessFunction.accept(publicationIndex, null, presetCommission,
                    batchClassifications);
            } else {
                publicationIndex.getOrganisationUnitIds().forEach(organisationUnitId ->
                    assessFunction.accept(publicationIndex, organisationUnitId, null,
                        batchClassifications));
            }
        });

        if (!batchClassifications.isEmpty()) {
            log.info("Saving {} new classifications.", batchClassifications.size());
        }

        documentAssessmentClassificationRepository.saveAll(batchClassifications);
        batchClassifications.clear();
    }
}
