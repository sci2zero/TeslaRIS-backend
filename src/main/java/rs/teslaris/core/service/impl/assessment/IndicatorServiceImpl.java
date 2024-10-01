package rs.teslaris.core.service.impl.assessment;

import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Service;
import rs.teslaris.core.converter.assessment.IndicatorConverter;
import rs.teslaris.core.dto.assessment.IndicatorDTO;
import rs.teslaris.core.model.assessment.Indicator;
import rs.teslaris.core.repository.assessment.IndicatorRepository;
import rs.teslaris.core.service.impl.JPAServiceImpl;
import rs.teslaris.core.service.interfaces.assessment.IndicatorService;
import rs.teslaris.core.service.interfaces.commontypes.MultilingualContentService;
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
    public Page<IndicatorDTO> readAllIndicators(Pageable pageable) {
        return indicatorRepository.findAll(pageable).map(IndicatorConverter::toDTO);
    }

    @Override
    public IndicatorDTO readIndicatorById(
        Integer indicatorId) {
        return IndicatorConverter.toDTO(findOne(indicatorId));
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
        indicator.setCode(indicatorDTO.code());
        indicator.setTitle(
            multilingualContentService.getMultilingualContent(indicatorDTO.title()));
        indicator.setDescription(
            multilingualContentService.getMultilingualContent(indicatorDTO.description()));
    }

    @Override
    public void deleteIndicator(Integer indicatorId) {
        if (indicatorRepository.isInUse(indicatorId)) {
            throw new IndicatorReferenceConstraintViolationException("indicatorInUse.");
        }

        delete(indicatorId);
    }

}