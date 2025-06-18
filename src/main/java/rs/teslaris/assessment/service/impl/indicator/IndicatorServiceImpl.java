package rs.teslaris.assessment.service.impl.indicator;

import java.util.HashSet;
import java.util.List;
import java.util.stream.Collectors;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Service;
import rs.teslaris.assessment.converter.IndicatorConverter;
import rs.teslaris.assessment.dto.indicator.IndicatorDTO;
import rs.teslaris.assessment.dto.indicator.IndicatorResponseDTO;
import rs.teslaris.assessment.model.indicator.ApplicableEntityType;
import rs.teslaris.assessment.model.indicator.Indicator;
import rs.teslaris.assessment.repository.indicator.IndicatorRepository;
import rs.teslaris.assessment.service.interfaces.indicator.IndicatorService;
import rs.teslaris.assessment.util.IndicatorMappingConfigurationLoader;
import rs.teslaris.core.annotation.Traceable;
import rs.teslaris.core.model.commontypes.AccessLevel;
import rs.teslaris.core.service.impl.JPAServiceImpl;
import rs.teslaris.core.service.interfaces.commontypes.MultilingualContentService;
import rs.teslaris.core.util.exceptionhandling.exception.IndicatorCodeInUseException;
import rs.teslaris.core.util.exceptionhandling.exception.IndicatorReferenceConstraintViolationException;

@Service
@RequiredArgsConstructor
@Traceable
public class IndicatorServiceImpl extends JPAServiceImpl<Indicator>
    implements IndicatorService {

    private final IndicatorRepository indicatorRepository;

    private final MultilingualContentService multilingualContentService;


    @Override
    protected JpaRepository<Indicator, Integer> getEntityRepository() {
        return indicatorRepository;
    }

    @Override
    public Page<IndicatorResponseDTO> readAllIndicators(Pageable pageable, String language) {
        return indicatorRepository.readAll(language, pageable).map(IndicatorConverter::toDTO);
    }

    @Override
    public List<IndicatorResponseDTO> getIndicatorsApplicableToEntity(
        List<ApplicableEntityType> applicableEntityTypes) {
        if (!applicableEntityTypes.isEmpty() &&
            !applicableEntityTypes.contains(ApplicableEntityType.ALL)) {
            applicableEntityTypes.add(ApplicableEntityType.ALL);
        }

        var statisticIndicators =
            IndicatorMappingConfigurationLoader.fetchAllStatisticsIndicatorCodes();

        return indicatorRepository.getIndicatorsApplicableToEntity(applicableEntityTypes).stream()
            .filter(indicator -> !statisticIndicators.contains(indicator.getCode()))
            .map(IndicatorConverter::toDTO).collect(Collectors.toList());
    }

    @Override
    public IndicatorResponseDTO readIndicatorById(
        Integer indicatorId) {
        return IndicatorConverter.toDTO(findOne(indicatorId));
    }

    @Override
    public AccessLevel readIndicatorAccessLEvel(Integer indicatorId) {
        return findOne(indicatorId).getAccessLevel();
    }

    @Override
    public Indicator getIndicatorByCode(String code) {
        return indicatorRepository.findByCode(code);
    }

    @Override
    public Indicator createIndicator(IndicatorDTO indicator) {
        var newIndicator = new Indicator();

        setCommonFields(newIndicator, indicator);

        return save(newIndicator);
    }

    @Override
    public void updateIndicator(Integer indicatorId, IndicatorDTO indicator) {
        var indicatorToUpdate = findOne(indicatorId);

        setCommonFields(indicatorToUpdate, indicator);

        save(indicatorToUpdate);
    }

    private void setCommonFields(Indicator indicator, IndicatorDTO indicatorDTO) {
        if (indicatorRepository.indicatorCodeInUse(indicatorDTO.code(), indicator.getId())) {
            throw new IndicatorCodeInUseException(
                "Indicator code " + indicatorDTO.code() + " is allready in use.");
        }

        indicator.setCode(indicatorDTO.code());
        indicator.setApplicableTypes(new HashSet<>(indicatorDTO.applicableTypes()));
        indicator.setTitle(
            multilingualContentService.getMultilingualContentAndSetDefaultsIfNonExistent(
                indicatorDTO.title()));
        indicator.setDescription(
            multilingualContentService.getMultilingualContentAndSetDefaultsIfNonExistent(
                indicatorDTO.description()));
        indicator.setAccessLevel(indicatorDTO.indicatorAccessLevel());
        indicator.setContentType(indicatorDTO.contentType());
    }

    @Override
    public void deleteIndicator(Integer indicatorId) {
        if (indicatorRepository.isInUse(indicatorId)) {
            throw new IndicatorReferenceConstraintViolationException("indicatorInUse.");
        }

        delete(indicatorId);
    }

}