package rs.teslaris.assessment.service.impl;

import java.util.List;
import java.util.stream.Collectors;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import rs.teslaris.assessment.converter.EntityIndicatorConverter;
import rs.teslaris.assessment.dto.EntityIndicatorResponseDTO;
import rs.teslaris.assessment.repository.EntityIndicatorRepository;
import rs.teslaris.assessment.repository.OrganisationUnitIndicatorRepository;
import rs.teslaris.assessment.service.interfaces.IndicatorService;
import rs.teslaris.assessment.service.interfaces.OrganisationUnitIndicatorService;
import rs.teslaris.core.model.commontypes.AccessLevel;
import rs.teslaris.core.service.interfaces.document.DocumentFileService;

@Service
public class OrganisationUnitIndicatorServiceImpl extends EntityIndicatorServiceImpl
    implements OrganisationUnitIndicatorService {

    private final OrganisationUnitIndicatorRepository organisationUnitIndicatorRepository;

    @Autowired
    public OrganisationUnitIndicatorServiceImpl(
        EntityIndicatorRepository entityIndicatorRepository,
        DocumentFileService documentFileService,
        IndicatorService indicatorService,
        OrganisationUnitIndicatorRepository organisationUnitIndicatorRepository) {
        super(indicatorService, entityIndicatorRepository, documentFileService);
        this.organisationUnitIndicatorRepository = organisationUnitIndicatorRepository;
    }

    @Override
    public List<EntityIndicatorResponseDTO> getIndicatorsForOrganisationUnit(
        Integer organisationUnitId,
        AccessLevel accessLevel) {
        return organisationUnitIndicatorRepository.findIndicatorsForOrganisationUnitAndIndicatorAccessLevel(
            organisationUnitId,
            accessLevel).stream().map(
            EntityIndicatorConverter::toDTO).collect(Collectors.toList());
    }
}
