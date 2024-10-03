package rs.teslaris.core.assessment.service.impl;

import java.util.List;
import java.util.stream.Collectors;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import rs.teslaris.core.assessment.converter.EntityIndicatorConverter;
import rs.teslaris.core.assessment.dto.EntityIndicatorResponseDTO;
import rs.teslaris.core.assessment.model.IndicatorAccessLevel;
import rs.teslaris.core.assessment.repository.EntityIndicatorRepository;
import rs.teslaris.core.assessment.repository.PublicationSeriesIndicatorRepository;
import rs.teslaris.core.assessment.service.interfaces.IndicatorService;
import rs.teslaris.core.assessment.service.interfaces.PublicationSeriesIndicatorService;
import rs.teslaris.core.service.interfaces.document.DocumentFileService;

@Service
public class PublicationSeriesIndicatorServiceImpl extends EntityIndicatorServiceImpl
    implements PublicationSeriesIndicatorService {

    private final PublicationSeriesIndicatorRepository publicationSeriesIndicatorRepository;

    @Autowired
    public PublicationSeriesIndicatorServiceImpl(
        EntityIndicatorRepository entityIndicatorRepository,
        DocumentFileService documentFileService,
        IndicatorService indicatorService,
        PublicationSeriesIndicatorRepository publicationSeriesIndicatorRepository) {
        super(entityIndicatorRepository, documentFileService, indicatorService);
        this.publicationSeriesIndicatorRepository = publicationSeriesIndicatorRepository;
    }

    @Override
    public List<EntityIndicatorResponseDTO> getIndicatorsForPublicationSeries(
        Integer publicationSeriesId,
        IndicatorAccessLevel accessLevel) {
        return publicationSeriesIndicatorRepository.findIndicatorsForPublicationSeriesAndIndicatorAccessLevel(
            publicationSeriesId,
            accessLevel).stream().map(
            EntityIndicatorConverter::toDTO).collect(Collectors.toList());
    }
}
