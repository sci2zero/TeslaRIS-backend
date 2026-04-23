package rs.teslaris.core.service.impl.identifier;

import java.util.List;
import java.util.stream.Collectors;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import rs.teslaris.core.annotation.Traceable;
import rs.teslaris.core.converter.identifier.EntityIdentifierConverter;
import rs.teslaris.core.dto.identifier.EntityIdentifierResponseDTO;
import rs.teslaris.core.dto.identifier.OrganisationUnitIdentifierDTO;
import rs.teslaris.core.model.commontypes.AccessLevel;
import rs.teslaris.core.model.identifier.OrganisationUnitIdentifier;
import rs.teslaris.core.repository.identifier.EntityIdentifierRepository;
import rs.teslaris.core.repository.identifier.OrganisationUnitIdentifierRepository;
import rs.teslaris.core.service.impl.identifier.cruddelegate.OrganisationUnitIdentifierJPAServiceImpl;
import rs.teslaris.core.service.interfaces.identifier.IdentifierService;
import rs.teslaris.core.service.interfaces.identifier.OrganisationUnitIdentifierService;
import rs.teslaris.core.service.interfaces.institution.OrganisationUnitService;

@Service
@Traceable
public class OrganisationUnitIdentifierServiceImpl extends EntityIdentifierServiceImpl
    implements OrganisationUnitIdentifierService {

    private final OrganisationUnitIdentifierRepository organisationUnitIdentifierRepository;

    private final OrganisationUnitIdentifierJPAServiceImpl organisationUnitIdentifierJPAService;

    private final OrganisationUnitService organisationUnitService;


    @Autowired
    public OrganisationUnitIdentifierServiceImpl(
        EntityIdentifierRepository entityIdentifierRepository,
        IdentifierService identifierService,
        OrganisationUnitIdentifierRepository organisationUnitIdentifierRepository,
        OrganisationUnitIdentifierJPAServiceImpl organisationUnitIdentifierJPAService,
        OrganisationUnitService organisationUnitService) {
        super(entityIdentifierRepository, identifierService);
        this.organisationUnitIdentifierRepository = organisationUnitIdentifierRepository;
        this.organisationUnitIdentifierJPAService = organisationUnitIdentifierJPAService;
        this.organisationUnitService = organisationUnitService;
    }

    @Override
    @Transactional(readOnly = true)
    public List<EntityIdentifierResponseDTO> getIdentifiersForOrganisationUnit(
        Integer organisationUnitId,
        AccessLevel accessLevel) {
        return organisationUnitIdentifierRepository.findIdentifiersForOrganisationUnitAndIdentifierAccessLevel(
            organisationUnitId,
            accessLevel).stream().map(
            EntityIdentifierConverter::toDTO).collect(Collectors.toList());
    }

    @Override
    @Transactional
    public OrganisationUnitIdentifier createOrganisationUnitIdentifier(
        OrganisationUnitIdentifierDTO organisationUnitIdentifierDTO,
        Integer userId) {
        var newOrganisationUnitIdentifier = new OrganisationUnitIdentifier();

        setCommonFields(newOrganisationUnitIdentifier, organisationUnitIdentifierDTO);

        newOrganisationUnitIdentifier.setOrganisationUnit(
            organisationUnitService.findOne(organisationUnitIdentifierDTO.getOrganisationUnitId()));

        return organisationUnitIdentifierJPAService.save(newOrganisationUnitIdentifier);
    }

    @Override
    @Transactional
    public void updateOrganisationUnitIdentifier(Integer organisationUnitIdentifierId,
                                                 OrganisationUnitIdentifierDTO organisationUnitIdentifierDTO) {
        var organisationUnitIdentifierToUpdate =
            organisationUnitIdentifierJPAService.findOne(organisationUnitIdentifierId);

        setCommonFields(organisationUnitIdentifierToUpdate, organisationUnitIdentifierDTO);

        organisationUnitIdentifierToUpdate.setOrganisationUnit(
            organisationUnitService.findOne(organisationUnitIdentifierDTO.getOrganisationUnitId()));

        organisationUnitIdentifierJPAService.save(organisationUnitIdentifierToUpdate);
    }
}
