package rs.teslaris.core.assessment.service.impl;

import java.util.List;
import java.util.stream.Collectors;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import rs.teslaris.core.assessment.converter.EntityIndicatorConverter;
import rs.teslaris.core.assessment.dto.EntityIndicatorResponseDTO;
import rs.teslaris.core.assessment.repository.EntityIndicatorRepository;
import rs.teslaris.core.assessment.repository.PersonIndicatorRepository;
import rs.teslaris.core.assessment.service.interfaces.IndicatorService;
import rs.teslaris.core.assessment.service.interfaces.PersonIndicatorService;
import rs.teslaris.core.model.commontypes.AccessLevel;
import rs.teslaris.core.service.interfaces.document.DocumentFileService;

@Service
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
