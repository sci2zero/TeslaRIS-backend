package rs.teslaris.assessment.service.impl.indicator;

import java.util.List;
import java.util.stream.Collectors;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import rs.teslaris.assessment.converter.EntityIndicatorConverter;
import rs.teslaris.assessment.dto.indicator.EntityIndicatorResponseDTO;
import rs.teslaris.assessment.repository.indicator.EntityIndicatorRepository;
import rs.teslaris.assessment.repository.indicator.PersonIndicatorRepository;
import rs.teslaris.assessment.service.interfaces.PersonIndicatorService;
import rs.teslaris.assessment.service.interfaces.indicator.IndicatorService;
import rs.teslaris.core.annotation.Traceable;
import rs.teslaris.core.model.commontypes.AccessLevel;
import rs.teslaris.core.service.interfaces.document.DocumentFileService;

@Service
@Traceable
public class PersonIndicatorServiceImpl extends EntityIndicatorServiceImpl
    implements PersonIndicatorService {

    private final PersonIndicatorRepository personIndicatorRepository;

    @Autowired
    public PersonIndicatorServiceImpl(
        EntityIndicatorRepository entityIndicatorRepository,
        DocumentFileService documentFileService,
        IndicatorService indicatorService, PersonIndicatorRepository personIndicatorRepository) {
        super(indicatorService, entityIndicatorRepository, documentFileService);
        this.personIndicatorRepository = personIndicatorRepository;
    }

    @Override
    public List<EntityIndicatorResponseDTO> getIndicatorsForPerson(Integer personId,
                                                                   AccessLevel accessLevel) {
        return personIndicatorRepository.findIndicatorsForPersonAndIndicatorAccessLevel(personId,
            accessLevel).stream().map(
            EntityIndicatorConverter::toDTO).collect(Collectors.toList());
    }
}
