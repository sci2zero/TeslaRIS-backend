package rs.teslaris.core.service.impl.identifier;

import java.util.List;
import java.util.stream.Collectors;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import rs.teslaris.core.annotation.Traceable;
import rs.teslaris.core.converter.identifier.EntityIdentifierConverter;
import rs.teslaris.core.dto.identifier.DocumentIdentifierDTO;
import rs.teslaris.core.dto.identifier.EntityIdentifierResponseDTO;
import rs.teslaris.core.model.commontypes.AccessLevel;
import rs.teslaris.core.model.identifier.DocumentIdentifier;
import rs.teslaris.core.repository.identifier.DocumentIdentifierRepository;
import rs.teslaris.core.repository.identifier.EntityIdentifierRepository;
import rs.teslaris.core.service.impl.identifier.cruddelegate.DocumentIdentifierJPAServiceImpl;
import rs.teslaris.core.service.interfaces.document.DocumentLookupService;
import rs.teslaris.core.service.interfaces.identifier.DocumentIdentifierService;
import rs.teslaris.core.service.interfaces.identifier.IdentifierService;

@Service
@Traceable
public class DocumentIdentifierServiceImpl extends EntityIdentifierServiceImpl
    implements DocumentIdentifierService {

    private final DocumentIdentifierRepository documentIdentifierRepository;

    private final DocumentIdentifierJPAServiceImpl documentIdentifierJPAService;

    private final DocumentLookupService documentLookupService;


    @Autowired
    public DocumentIdentifierServiceImpl(EntityIdentifierRepository entityIdentifierRepository,
                                         IdentifierService identifierService,
                                         DocumentIdentifierRepository documentIdentifierRepository,
                                         DocumentIdentifierJPAServiceImpl documentIdentifierJPAService,
                                         DocumentLookupService documentLookupService) {
        super(entityIdentifierRepository, identifierService);
        this.documentIdentifierRepository = documentIdentifierRepository;
        this.documentIdentifierJPAService = documentIdentifierJPAService;
        this.documentLookupService = documentLookupService;
    }

    @Override
    @Transactional(readOnly = true)
    public List<EntityIdentifierResponseDTO> getIdentifiersForDocument(Integer documentId,
                                                                       AccessLevel accessLevel) {
        return documentIdentifierRepository.findIdentifiersForDocumentAndIdentifierAccessLevel(
            documentId,
            accessLevel).stream().map(
            EntityIdentifierConverter::toDTO).collect(Collectors.toList());
    }

    @Override
    @Transactional
    public DocumentIdentifier createDocumentIdentifier(DocumentIdentifierDTO documentIdentifierDTO,
                                                       Integer userId) {
        var newDocumentIdentifier = new DocumentIdentifier();

        setCommonFields(newDocumentIdentifier, documentIdentifierDTO);

        newDocumentIdentifier.setDocument(
            documentLookupService.fastDocumentLookup(documentIdentifierDTO.getDocumentId()));

        return documentIdentifierJPAService.save(newDocumentIdentifier);
    }

    @Override
    @Transactional
    public void updateDocumentIdentifier(Integer documentIdentifierId,
                                         DocumentIdentifierDTO documentIdentifierDTO) {
        var documentIdentifierToUpdate = documentIdentifierJPAService.findOne(documentIdentifierId);

        setCommonFields(documentIdentifierToUpdate, documentIdentifierDTO);

        documentIdentifierToUpdate.setDocument(
            documentLookupService.fastDocumentLookup(documentIdentifierDTO.getDocumentId()));

        documentIdentifierJPAService.save(documentIdentifierToUpdate);
    }
}
