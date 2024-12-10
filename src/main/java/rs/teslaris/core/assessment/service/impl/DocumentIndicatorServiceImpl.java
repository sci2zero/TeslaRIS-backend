package rs.teslaris.core.assessment.service.impl;

import java.util.List;
import java.util.stream.Collectors;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import rs.teslaris.core.assessment.converter.EntityIndicatorConverter;
import rs.teslaris.core.assessment.dto.DocumentIndicatorDTO;
import rs.teslaris.core.assessment.dto.EntityIndicatorResponseDTO;
import rs.teslaris.core.assessment.model.DocumentIndicator;
import rs.teslaris.core.assessment.repository.DocumentIndicatorRepository;
import rs.teslaris.core.assessment.repository.EntityIndicatorRepository;
import rs.teslaris.core.assessment.service.impl.cruddelegate.DocumentIndicatorJPAServiceImpl;
import rs.teslaris.core.assessment.service.interfaces.DocumentIndicatorService;
import rs.teslaris.core.assessment.service.interfaces.IndicatorService;
import rs.teslaris.core.model.commontypes.AccessLevel;
import rs.teslaris.core.service.interfaces.document.DocumentFileService;
import rs.teslaris.core.service.interfaces.document.DocumentPublicationService;
import rs.teslaris.core.service.interfaces.user.UserService;

@Service
@Transactional
public class DocumentIndicatorServiceImpl extends EntityIndicatorServiceImpl
    implements DocumentIndicatorService {

    private final DocumentIndicatorJPAServiceImpl documentIndicatorJPAService;

    private final DocumentIndicatorRepository documentIndicatorRepository;

    private final DocumentPublicationService documentPublicationService;

    private final UserService userService;


    @Autowired
    public DocumentIndicatorServiceImpl(
        EntityIndicatorRepository entityIndicatorRepository,
        DocumentFileService documentFileService,
        IndicatorService indicatorService,
        DocumentIndicatorJPAServiceImpl documentIndicatorJPAService,
        DocumentIndicatorRepository documentIndicatorRepository,
        DocumentPublicationService documentPublicationService, UserService userService) {
        super(entityIndicatorRepository, documentFileService, indicatorService);
        this.documentIndicatorJPAService = documentIndicatorJPAService;
        this.documentIndicatorRepository = documentIndicatorRepository;
        this.documentPublicationService = documentPublicationService;
        this.userService = userService;
    }

    @Override
    public List<EntityIndicatorResponseDTO> getIndicatorsForDocument(Integer documentId,
                                                                     AccessLevel accessLevel) {
        return documentIndicatorRepository.findIndicatorsForDocumentAndIndicatorAccessLevel(
            documentId, accessLevel).stream().map(
            EntityIndicatorConverter::toDTO).collect(Collectors.toList());
    }

    @Override
    public DocumentIndicator createDocumentIndicator(DocumentIndicatorDTO documentIndicatorDTO,
                                                     Integer userId) {
        var newDocumentIndicator = new DocumentIndicator();

        setCommonFields(newDocumentIndicator, documentIndicatorDTO);
        newDocumentIndicator.setUser(userService.findOne(userId));

        newDocumentIndicator.setDocument(
            documentPublicationService.findOne(documentIndicatorDTO.getDocumentId()));

        return documentIndicatorJPAService.save(newDocumentIndicator);
    }

    @Override
    public void updateDocumentIndicator(Integer documentIndicatorId,
                                        DocumentIndicatorDTO documentIndicatorDTO) {
        var documentIndicatorToUpdate = documentIndicatorJPAService.findOne(documentIndicatorId);

        setCommonFields(documentIndicatorToUpdate, documentIndicatorDTO);

        documentIndicatorToUpdate.setDocument(
            documentPublicationService.findOne(documentIndicatorDTO.getDocumentId()));

        documentIndicatorJPAService.save(documentIndicatorToUpdate);
    }
}
