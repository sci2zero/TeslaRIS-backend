package rs.teslaris.core.assessment.service.impl;

import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Service;
import rs.teslaris.core.assessment.converter.IndicatorConverter;
import rs.teslaris.core.assessment.dto.IndicatorDTO;
import rs.teslaris.core.assessment.dto.IndicatorResponseDTO;
import rs.teslaris.core.assessment.model.Indicator;
import rs.teslaris.core.assessment.repository.IndicatorRepository;
import rs.teslaris.core.assessment.service.interfaces.IndicatorService;
import rs.teslaris.core.model.commontypes.AccessLevel;
import rs.teslaris.core.service.impl.JPAServiceImpl;
import rs.teslaris.core.service.interfaces.commontypes.MultilingualContentService;
import rs.teslaris.core.util.exceptionhandling.exception.IndicatorCodeInUseException;
import rs.teslaris.core.util.exceptionhandling.exception.IndicatorReferenceConstraintViolationException;

@Service
@RequiredArgsConstructor
public class IndicatorServiceImpl extends JPAServiceImpl<Indicator>
    implements IndicatorService {

    private final IndicatorRepository indicatorRepository;

    private final MultilingualContentService multilingualContentService;


    @Override
    protected JpaRepository<Indicator, Integer> getEntityRepository() {
        return indicatorRepository;
    }

    @Override
    public Page<IndicatorResponseDTO> readAllIndicators(Pageable pageable) {
        return indicatorRepository.findAll(pageable).map(IndicatorConverter::toDTO);
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
        indicator.setTitle(
            multilingualContentService.getMultilingualContent(indicatorDTO.title()));
        indicator.setDescription(
            multilingualContentService.getMultilingualContent(indicatorDTO.description()));
        indicator.setAccessLevel(indicatorDTO.indicatorAccessLevel());
    }

    @Override
    public void deleteIndicator(Integer indicatorId) {
        if (indicatorRepository.isInUse(indicatorId)) {
            throw new IndicatorReferenceConstraintViolationException("indicatorInUse.");
        }

        delete(indicatorId);
    }

}