package rs.teslaris.core.service.impl.identifier;

import java.util.List;
import java.util.stream.Collectors;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import rs.teslaris.core.annotation.Traceable;
import rs.teslaris.core.converter.identifier.EntityIdentifierConverter;
import rs.teslaris.core.dto.identifier.EntityIdentifierResponseDTO;
import rs.teslaris.core.dto.identifier.PublicationSeriesIdentifierDTO;
import rs.teslaris.core.model.commontypes.AccessLevel;
import rs.teslaris.core.model.identifier.PublicationSeriesIdentifier;
import rs.teslaris.core.repository.identifier.EntityIdentifierRepository;
import rs.teslaris.core.repository.identifier.PublicationSeriesIdentifierRepository;
import rs.teslaris.core.service.impl.identifier.cruddelegate.PublicationSeriesIdentifierJPAServiceImpl;
import rs.teslaris.core.service.interfaces.document.PublicationSeriesLookupService;
import rs.teslaris.core.service.interfaces.identifier.IdentifierService;
import rs.teslaris.core.service.interfaces.identifier.PublicationSeriesIdentifierService;

@Service
@Traceable
public class PublicationSeriesIdentifierServiceImpl extends EntityIdentifierServiceImpl
    implements PublicationSeriesIdentifierService {

    private final PublicationSeriesIdentifierRepository publicationSeriesIdentifierRepository;

    private final PublicationSeriesIdentifierJPAServiceImpl publicationSeriesIdentifierJPAService;

    private final PublicationSeriesLookupService publicationSeriesLookupService;


    @Autowired
    public PublicationSeriesIdentifierServiceImpl(
        EntityIdentifierRepository entityIdentifierRepository,
        IdentifierService identifierService,
        PublicationSeriesIdentifierRepository publicationSeriesIdentifierRepository,
        PublicationSeriesIdentifierJPAServiceImpl publicationSeriesIdentifierJPAService,
        PublicationSeriesLookupService publicationSeriesLookupService) {
        super(entityIdentifierRepository, identifierService);
        this.publicationSeriesIdentifierRepository = publicationSeriesIdentifierRepository;
        this.publicationSeriesIdentifierJPAService = publicationSeriesIdentifierJPAService;
        this.publicationSeriesLookupService = publicationSeriesLookupService;
    }

    @Override
    @Transactional(readOnly = true)
    public List<EntityIdentifierResponseDTO> getIdentifiersForPublicationSeries(
        Integer publicationSeriesId,
        AccessLevel accessLevel) {
        return publicationSeriesIdentifierRepository.findIdentifiersForPublicationSeriesAndIdentifierAccessLevel(
            publicationSeriesId,
            accessLevel).stream().map(
            EntityIdentifierConverter::toDTO).collect(Collectors.toList());
    }

    @Override
    @Transactional
    public PublicationSeriesIdentifier createPublicationSeriesIdentifier(
        PublicationSeriesIdentifierDTO publicationSeriesIdentifierDTO,
        Integer userId) {
        var newPublicationSeriesIdentifier = new PublicationSeriesIdentifier();

        setCommonFields(newPublicationSeriesIdentifier, publicationSeriesIdentifierDTO);

        newPublicationSeriesIdentifier.setPublicationSeries(
            publicationSeriesLookupService.fastPublicationSeriesLookup(
                publicationSeriesIdentifierDTO.getPublicationSeriesId()));

        return publicationSeriesIdentifierJPAService.save(newPublicationSeriesIdentifier);
    }

    @Override
    @Transactional
    public void updatePublicationSeriesIdentifier(Integer publicationSeriesIdentifierId,
                                                  PublicationSeriesIdentifierDTO publicationSeriesIdentifierDTO) {
        var publicationSeriesIdentifierToUpdate =
            publicationSeriesIdentifierJPAService.findOne(publicationSeriesIdentifierId);

        setCommonFields(publicationSeriesIdentifierToUpdate, publicationSeriesIdentifierDTO);

        publicationSeriesIdentifierToUpdate.setPublicationSeries(
            publicationSeriesLookupService.fastPublicationSeriesLookup(
                publicationSeriesIdentifierDTO.getPublicationSeriesId()));

        publicationSeriesIdentifierJPAService.save(publicationSeriesIdentifierToUpdate);
    }
}
