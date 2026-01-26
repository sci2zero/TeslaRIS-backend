package rs.teslaris.assessment.service.interfaces.classification;

import java.util.ArrayList;
import java.util.List;
import org.springframework.stereotype.Service;
import rs.teslaris.assessment.model.classification.DocumentAssessmentClassification;
import rs.teslaris.core.indexmodel.DocumentPublicationIndex;
import rs.teslaris.core.model.institution.Commission;
import rs.teslaris.core.util.functional.QuadConsumer;

@Service
public interface PublicationClassificationService {

    void classifyPublicationsChunk(List<DocumentPublicationIndex> chunk,
                                   Commission presetCommission,
                                   QuadConsumer<DocumentPublicationIndex, Integer, Commission, ArrayList<DocumentAssessmentClassification>> assessFunction,
                                   ArrayList<DocumentAssessmentClassification> batchClassifications);
}
