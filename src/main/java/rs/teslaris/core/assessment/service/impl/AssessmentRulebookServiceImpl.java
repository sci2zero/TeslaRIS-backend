package rs.teslaris.core.assessment.service.impl;

import java.util.Objects;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import rs.teslaris.core.assessment.converter.AssessmentMeasureConverter;
import rs.teslaris.core.assessment.converter.AssessmentRulebookConverter;
import rs.teslaris.core.assessment.dto.AssessmentMeasureDTO;
import rs.teslaris.core.assessment.dto.AssessmentRulebookDTO;
import rs.teslaris.core.assessment.dto.AssessmentRulebookResponseDTO;
import rs.teslaris.core.assessment.model.AssessmentRulebook;
import rs.teslaris.core.assessment.repository.AssessmentRulebookRepository;
import rs.teslaris.core.assessment.service.interfaces.AssessmentMeasureService;
import rs.teslaris.core.assessment.service.interfaces.AssessmentRulebookService;
import rs.teslaris.core.converter.document.DocumentFileConverter;
import rs.teslaris.core.dto.document.DocumentFileDTO;
import rs.teslaris.core.dto.document.DocumentFileResponseDTO;
import rs.teslaris.core.service.impl.JPAServiceImpl;
import rs.teslaris.core.service.interfaces.commontypes.MultilingualContentService;
import rs.teslaris.core.service.interfaces.document.DocumentFileService;
import rs.teslaris.core.service.interfaces.document.PublisherService;

@Service
@RequiredArgsConstructor
@Transactional
public class AssessmentRulebookServiceImpl extends JPAServiceImpl<AssessmentRulebook> implements
    AssessmentRulebookService {

    private final AssessmentRulebookRepository assessmentRulebookRepository;

    private final MultilingualContentService multilingualContentService;

    private final DocumentFileService documentFileService;

    private final PublisherService publisherService;

    private final AssessmentMeasureService assessmentMeasureService;


    @Override
    protected JpaRepository<AssessmentRulebook, Integer> getEntityRepository() {
        return assessmentRulebookRepository;
    }

    @Override
    public Page<AssessmentRulebookResponseDTO> readAllAssessmentRulebooks(Pageable pageable) {
        return assessmentRulebookRepository.findAll(pageable)
            .map(AssessmentRulebookConverter::toDTO);
    }

    @Override
    public Page<AssessmentMeasureDTO> readAssessmentMeasuresForRulebook(Pageable pageable,
                                                                        Integer rulebookId) {
        return assessmentRulebookRepository.readAssessmentMeasuresForRulebook(pageable, rulebookId)
            .map(AssessmentMeasureConverter::toDTO);
    }

    @Override
    public AssessmentRulebookResponseDTO readAssessmentRulebookById(Integer assessmentRulebookId) {
        return AssessmentRulebookConverter.toDTO(findOne(assessmentRulebookId));
    }

    @Override
    public AssessmentRulebook createAssessmentRulebook(
        AssessmentRulebookDTO assessmentRulebookDTO) {
        var newAssessmentRulebook = new AssessmentRulebook();

        setCommonFields(newAssessmentRulebook, assessmentRulebookDTO);

        return save(newAssessmentRulebook);
    }

    @Override
    public DocumentFileResponseDTO addPDFFile(Integer assessmentRulebookId,
                                              DocumentFileDTO file) {
        var assessmentRulebook = findOne(assessmentRulebookId);
        var documentFile = documentFileService.saveNewDocument(file, false);

        if (Objects.nonNull(assessmentRulebook.getPdfFile())) {
            deletePDFFile(assessmentRulebookId, assessmentRulebook.getPdfFile().getId());
        }

        assessmentRulebook.setPdfFile(documentFile);
        assessmentRulebookRepository.save(assessmentRulebook);

        return DocumentFileConverter.toDTO(documentFile);
    }

    @Override
    public void deletePDFFile(Integer assessmentRulebookId, Integer documentFileId) {
        var assessmentRulebook = findOne(assessmentRulebookId);
        var documentFile = documentFileService.findOne(documentFileId);

        assessmentRulebook.setPdfFile(null);
        documentFileService.deleteDocumentFile(documentFile.getServerFilename());

        assessmentRulebookRepository.save(assessmentRulebook);
    }

    @Override
    public void updateAssessmentRulebook(Integer assessmentRulebookId,
                                         AssessmentRulebookDTO assessmentRulebookDTO) {
        var assessmentRulebookToUpdate = findOne(assessmentRulebookId);

        setCommonFields(assessmentRulebookToUpdate, assessmentRulebookDTO);

        save(assessmentRulebookToUpdate);
    }

    @Override
    public void deleteAssessmentRulebook(Integer assessmentRulebookId) {
        // TODO: Do we need any checks here, if not - delete this method and use JpaService method
        delete(assessmentRulebookId);
    }

    private void setCommonFields(AssessmentRulebook assessmentRulebook, AssessmentRulebookDTO dto) {
        assessmentRulebook.setName(multilingualContentService.getMultilingualContent(dto.name()));
        assessmentRulebook.setDescription(
            multilingualContentService.getMultilingualContent(dto.description()));
        assessmentRulebook.setIssueDate(dto.issueDate());

        if (Objects.nonNull(assessmentRulebook.getPdfFile())) {
            documentFileService.deleteDocumentFile(
                assessmentRulebook.getPdfFile().getServerFilename());
        }

        if (Objects.nonNull(dto.publisherId())) {
            assessmentRulebook.setPublisher(publisherService.findOne(dto.publisherId()));
        }

        assessmentRulebook.getAssessmentMeasures().clear();
        dto.assessmentMeasureIds().forEach(assessmentMeasureId -> {
            var measure = assessmentMeasureService.findOne(assessmentMeasureId);
            assessmentRulebook.getAssessmentMeasures()
                .add(measure);
            measure.setRulebook(assessmentRulebook);
        });
    }
}
