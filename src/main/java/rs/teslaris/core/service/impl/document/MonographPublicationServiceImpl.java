package rs.teslaris.core.service.impl.document;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import rs.teslaris.core.dto.document.MonographPublicationDTO;
import rs.teslaris.core.indexmodel.DocumentPublicationIndex;
import rs.teslaris.core.indexrepository.DocumentPublicationIndexRepository;
import rs.teslaris.core.model.document.MonographPublication;
import rs.teslaris.core.repository.document.DocumentRepository;
import rs.teslaris.core.service.impl.document.cruddelegate.MonographPublicationJPAServiceImpl;
import rs.teslaris.core.service.interfaces.commontypes.MultilingualContentService;
import rs.teslaris.core.service.interfaces.commontypes.SearchService;
import rs.teslaris.core.service.interfaces.document.DocumentFileService;
import rs.teslaris.core.service.interfaces.document.EventService;
import rs.teslaris.core.service.interfaces.document.MonographPublicationService;
import rs.teslaris.core.service.interfaces.person.OrganisationUnitService;
import rs.teslaris.core.service.interfaces.person.PersonContributionService;
import rs.teslaris.core.util.search.ExpressionTransformer;

@Service
public class MonographPublicationServiceImpl extends DocumentPublicationServiceImpl implements
    MonographPublicationService {

    private final MonographPublicationJPAServiceImpl monographJPAService;

    @Autowired
    public MonographPublicationServiceImpl(
        MultilingualContentService multilingualContentService,
        DocumentPublicationIndexRepository documentPublicationIndexRepository,
        DocumentRepository documentRepository,
        DocumentFileService documentFileService,
        PersonContributionService personContributionService,
        SearchService<DocumentPublicationIndex> searchService,
        ExpressionTransformer expressionTransformer,
        EventService eventService,
        OrganisationUnitService organisationUnitService,
        MonographPublicationJPAServiceImpl monographJPAService) {
        super(multilingualContentService, documentPublicationIndexRepository, documentRepository,
            documentFileService, personContributionService, searchService, expressionTransformer,
            eventService, organisationUnitService);
        this.monographJPAService = monographJPAService;
    }

    @Override
    public MonographPublicationDTO readMonographPublicationById(Integer monographPublicationId) {
        return null;
    }

    @Override
    public MonographPublication createMonographPublication(
        MonographPublicationDTO monographPublicationDTO, Boolean index) {
        return null;
    }

    @Override
    public void updateMonographPublication(Integer monographId,
                                           MonographPublicationDTO monographPublicationDTO) {

    }

    @Override
    public void deleteMonographPublication(Integer monographPublicationId) {

    }

    @Override
    public void reindexMonographPublications() {

    }
}
