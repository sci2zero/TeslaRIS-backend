package rs.teslaris.assessment.service.impl.classification;

import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Service;
import rs.teslaris.assessment.converter.EntityAssessmentClassificationConverter;
import rs.teslaris.assessment.dto.classification.EntityAssessmentClassificationResponseDTO;
import rs.teslaris.assessment.dto.classification.PrizeAssessmentClassificationDTO;
import rs.teslaris.assessment.model.classification.PrizeAssessmentClassification;
import rs.teslaris.assessment.repository.classification.EntityAssessmentClassificationRepository;
import rs.teslaris.assessment.repository.classification.PrizeAssessmentClassificationRepository;
import rs.teslaris.assessment.service.interfaces.CommissionService;
import rs.teslaris.assessment.service.interfaces.classification.AssessmentClassificationService;
import rs.teslaris.assessment.service.interfaces.classification.PrizeAssessmentClassificationService;
import rs.teslaris.core.applicationevent.ResearcherPointsReindexingEvent;
import rs.teslaris.core.repository.person.PrizeRepository;
import rs.teslaris.core.service.interfaces.document.ConferenceService;
import rs.teslaris.core.service.interfaces.document.DocumentPublicationService;
import rs.teslaris.core.service.interfaces.person.PrizeService;
import rs.teslaris.core.util.exceptionhandling.exception.CantEditException;
import rs.teslaris.core.util.exceptionhandling.exception.NotFoundException;

@Service
public class PrizeAssessmentClassificationServiceImpl
    extends EntityAssessmentClassificationServiceImpl implements
    PrizeAssessmentClassificationService {

    private final PrizeAssessmentClassificationRepository prizeAssessmentClassificationRepository;

    private final PrizeRepository prizeRepository;

    private final PrizeService prizeService;


    @Autowired
    public PrizeAssessmentClassificationServiceImpl(
        AssessmentClassificationService assessmentClassificationService,
        CommissionService commissionService,
        DocumentPublicationService documentPublicationService,
        ConferenceService conferenceService,
        ApplicationEventPublisher applicationEventPublisher,
        EntityAssessmentClassificationRepository entityAssessmentClassificationRepository,
        PrizeAssessmentClassificationRepository prizeAssessmentClassificationRepository,
        PrizeRepository prizeRepository, PrizeService prizeService) {
        super(assessmentClassificationService, commissionService, documentPublicationService,
            conferenceService, applicationEventPublisher, entityAssessmentClassificationRepository);
        this.prizeAssessmentClassificationRepository = prizeAssessmentClassificationRepository;
        this.prizeRepository = prizeRepository;
        this.prizeService = prizeService;
    }

    @Override
    public List<EntityAssessmentClassificationResponseDTO> getAssessmentClassificationsForPrize(
        Integer prizeId) {
        return prizeAssessmentClassificationRepository.findAssessmentClassificationsForPrize(
                prizeId).stream().map(EntityAssessmentClassificationConverter::toDTO)
            .sorted((a, b) -> b.year().compareTo(a.year()))
            .collect(Collectors.toList());
    }

    @Override
    public EntityAssessmentClassificationResponseDTO createPrizeAssessmentClassification(
        PrizeAssessmentClassificationDTO prizeAssessmentClassificationDTO) {
        var newPrizeClassification = new PrizeAssessmentClassification();

        setCommonFields(newPrizeClassification, prizeAssessmentClassificationDTO);

        newPrizeClassification.setCommission(
            commissionService.findOne(prizeAssessmentClassificationDTO.getCommissionId()));
        var prize =
            prizeRepository.findById(prizeAssessmentClassificationDTO.getPrizeId())
                .orElseThrow(() -> new NotFoundException(
                    "Prize with ID " + prizeAssessmentClassificationDTO.getPrizeId() +
                        " does not exist."));

        if (Objects.isNull(prize.getDate())) {
            throw new CantEditException("Prize does not have an acquisition date.");
        }

        prizeAssessmentClassificationRepository.deleteByPrizeIdAndCommissionId(
            prizeAssessmentClassificationDTO.getPrizeId(),
            prizeAssessmentClassificationDTO.getCommissionId(), true);

        newPrizeClassification.setClassificationYear(prize.getDate().getYear());
        newPrizeClassification.setPrize(prize);

        var savedPrizeClassification =
            prizeAssessmentClassificationRepository.save(newPrizeClassification);
        prizeService.reindexPrizeVolatileInformation(prize, null, false, true);

        applicationEventPublisher.publishEvent(
            new ResearcherPointsReindexingEvent(List.of(prize.getPerson().getId())));

        return EntityAssessmentClassificationConverter.toDTO(savedPrizeClassification);
    }

    @Override
    public void editPrizeAssessmentClassification(Integer classificationId,
                                                  PrizeAssessmentClassificationDTO prizeAssessmentClassificationDTO) {
        var prizeClassification = prizeAssessmentClassificationRepository.findById(classificationId)
            .orElseThrow(
                () -> new NotFoundException("Prize classification with given ID does not exist"));

        setCommonFields(prizeClassification, prizeAssessmentClassificationDTO);

        save(prizeClassification);

        prizeService.reindexPrizeVolatileInformation(prizeClassification.getPrize(), null, false,
            true);
        applicationEventPublisher.publishEvent(new ResearcherPointsReindexingEvent(
            List.of(prizeClassification.getPrize().getPerson().getId())));
    }
}
