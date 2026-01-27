package rs.teslaris.assessment.service.impl.classification;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import lombok.RequiredArgsConstructor;
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
public class PublicationClassificationServiceImpl implements PublicationClassificationService {

    private final DocumentAssessmentClassificationRepository
        documentAssessmentClassificationRepository;


    @Override
    @Transactional
    public void classifyPublicationsChunk(List<DocumentPublicationIndex> chunk,
                                          Commission presetCommission,
                                          QuadConsumer<DocumentPublicationIndex, Integer, Commission, ArrayList<DocumentAssessmentClassification>> assessFunction,
                                          ArrayList<DocumentAssessmentClassification> batchClassifications) {
        chunk.forEach(publicationIndex -> {
            if (publicationIndex.getType().equals(DocumentPublicationType.THESIS.name())) {
                if (Objects.isNull(publicationIndex.getThesisDefenceDate())) {
                    return;
                }

                assessFunction.accept(publicationIndex,
                    publicationIndex.getThesisInstitutionId(), null, batchClassifications);
            } else if (Objects.nonNull(presetCommission)) {
                assessFunction.accept(publicationIndex, null, presetCommission,
                    batchClassifications);
            } else {
                publicationIndex.getOrganisationUnitIds().forEach(organisationUnitId ->
                    assessFunction.accept(publicationIndex, organisationUnitId, null,
                        batchClassifications));
            }
        });

        documentAssessmentClassificationRepository.saveAll(batchClassifications);
        batchClassifications.clear();
    }
}
