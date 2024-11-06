package rs.teslaris.core.service.impl.document;

import java.util.List;
import java.util.Objects;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import rs.teslaris.core.converter.document.ThesisConverter;
import rs.teslaris.core.dto.document.ThesisDTO;
import rs.teslaris.core.dto.document.ThesisResponseDTO;
import rs.teslaris.core.indexmodel.DocumentPublicationIndex;
import rs.teslaris.core.indexmodel.DocumentPublicationType;
import rs.teslaris.core.indexrepository.DocumentPublicationIndexRepository;
import rs.teslaris.core.model.commontypes.ApproveStatus;
import rs.teslaris.core.model.document.Thesis;
import rs.teslaris.core.repository.document.DocumentRepository;
import rs.teslaris.core.service.impl.document.cruddelegate.ThesisJPAServiceImpl;
import rs.teslaris.core.service.interfaces.commontypes.LanguageTagService;
import rs.teslaris.core.service.interfaces.commontypes.MultilingualContentService;
import rs.teslaris.core.service.interfaces.commontypes.ResearchAreaService;
import rs.teslaris.core.service.interfaces.commontypes.SearchService;
import rs.teslaris.core.service.interfaces.document.DocumentFileService;
import rs.teslaris.core.service.interfaces.document.EventService;
import rs.teslaris.core.service.interfaces.document.PublisherService;
import rs.teslaris.core.service.interfaces.document.ThesisService;
import rs.teslaris.core.service.interfaces.person.OrganisationUnitService;
import rs.teslaris.core.service.interfaces.person.PersonContributionService;
import rs.teslaris.core.util.exceptionhandling.exception.NotFoundException;
import rs.teslaris.core.util.search.ExpressionTransformer;

@Service
@Transactional
public class ThesisServiceImpl extends DocumentPublicationServiceImpl implements ThesisService {

    private final ThesisJPAServiceImpl thesisJPAService;

    private final PublisherService publisherService;

    private final ResearchAreaService researchAreaService;

    private final LanguageTagService languageService;


    @Autowired
    public ThesisServiceImpl(MultilingualContentService multilingualContentService,
                             DocumentPublicationIndexRepository documentPublicationIndexRepository,
                             SearchService<DocumentPublicationIndex> searchService,
                             OrganisationUnitService organisationUnitService,
                             DocumentRepository documentRepository,
                             DocumentFileService documentFileService,
                             PersonContributionService personContributionService,
                             ExpressionTransformer expressionTransformer, EventService eventService,
                             ThesisJPAServiceImpl thesisJPAService,
                             PublisherService publisherService,
                             ResearchAreaService researchAreaService,
                             LanguageTagService languageService) {
        super(multilingualContentService, documentPublicationIndexRepository, searchService,
            organisationUnitService, documentRepository, documentFileService,
            personContributionService,
            expressionTransformer, eventService);
        this.thesisJPAService = thesisJPAService;
        this.publisherService = publisherService;
        this.researchAreaService = researchAreaService;
        this.languageService = languageService;
    }

    @Override
    public ThesisResponseDTO readThesisById(Integer thesisId) {
        return ThesisConverter.toDTO(thesisJPAService.findOne(thesisId));
    }

    @Override
    public Thesis createThesis(ThesisDTO thesisDTO, Boolean index) {
        var newThesis = new Thesis();

        if (Objects.nonNull(thesisDTO.getContributions()) &&
            !thesisDTO.getContributions().isEmpty()) {
            thesisDTO.setContributions(thesisDTO.getContributions().subList(0, 1));
        }

        setCommonFields(newThesis, thesisDTO);
        setThesisRelatedFields(newThesis, thesisDTO);

        newThesis.setApproveStatus(
            documentApprovedByDefault ? ApproveStatus.APPROVED : ApproveStatus.REQUESTED);

        var savedThesis = thesisJPAService.save(newThesis);

        if (newThesis.getApproveStatus().equals(ApproveStatus.APPROVED) && index) {
            indexThesis(savedThesis, new DocumentPublicationIndex());
        }

        sendNotifications(savedThesis);

        return newThesis;
    }

    @Override
    public void editThesis(Integer thesisId, ThesisDTO thesisDTO) {
        var thesisToUpdate = thesisJPAService.findOne(thesisId);

        clearCommonFields(thesisToUpdate);
        thesisToUpdate.setOrganisationUnit(null);

        if (Objects.nonNull(thesisDTO.getContributions()) &&
            !thesisDTO.getContributions().isEmpty()) {
            thesisDTO.setContributions(thesisDTO.getContributions().subList(0, 1));
        }

        setCommonFields(thesisToUpdate, thesisDTO);
        setThesisRelatedFields(thesisToUpdate, thesisDTO);

        thesisJPAService.save(thesisToUpdate);

        if (thesisToUpdate.getApproveStatus().equals(ApproveStatus.APPROVED)) {
            indexThesis(thesisToUpdate,
                documentPublicationIndexRepository.findDocumentPublicationIndexByDatabaseId(
                        thesisId)
                    .orElse(new DocumentPublicationIndex()));
        }

        sendNotifications(thesisToUpdate);
    }

    @Override
    public void deleteThesis(Integer thesisId) {
        thesisJPAService.delete(thesisId);
    }

    @Override
    public void reindexTheses() {
        // Super service does the initial deletion

        int pageNumber = 0;
        int chunkSize = 10;
        boolean hasNextPage = true;

        while (hasNextPage) {

            List<Thesis> chunk =
                thesisJPAService.findAll(PageRequest.of(pageNumber, chunkSize)).getContent();

            chunk.forEach((thesis) -> indexThesis(thesis, new DocumentPublicationIndex()));

            pageNumber++;
            hasNextPage = chunk.size() == chunkSize;
        }
    }

    private void setThesisRelatedFields(Thesis thesis, ThesisDTO thesisDTO) {
        thesis.setThesisType(thesisDTO.getThesisType());
        thesis.setNumberOfPages(thesisDTO.getNumberOfPages());

        if (Objects.nonNull(thesisDTO.getPublisherId())) {
            thesis.setPublisher(publisherService.findPublisherById(thesisDTO.getPublisherId()));
        }

        if (Objects.nonNull(thesisDTO.getResearchAreaId())) {
            thesis.setResearchArea(researchAreaService.findOne(thesisDTO.getResearchAreaId()));
        }

        if (Objects.nonNull(thesisDTO.getLanguageTagIds())) {
            thesisDTO.getLanguageTagIds().forEach(languageTagId -> {
                thesis.getLanguages().add(languageService.findOne(languageTagId));
            });
        }

        if (Objects.nonNull(thesisDTO.getOrganisationUnitId())) {
            thesis.setOrganisationUnit(
                organisationUnitService.findOrganisationUnitById(
                    thesisDTO.getOrganisationUnitId()));
        } else {
            if (Objects.isNull(thesisDTO.getExternalOrganisationUnitName())) {
                throw new NotFoundException(
                    "No organisation unit ID provided without external OU name reference.");
            }

            thesis.setExternalOrganisationUnitName(
                multilingualContentService.getMultilingualContent(
                    thesisDTO.getExternalOrganisationUnitName()));
        }
    }

    private void indexThesis(Thesis thesis, DocumentPublicationIndex index) {
        indexCommonFields(thesis, index);

        index.setType(DocumentPublicationType.THESIS.name());
        if (Objects.nonNull(thesis.getPublisher())) {
            index.setPublisherId(thesis.getPublisher().getId());
        }

        documentPublicationIndexRepository.save(index);
    }
}
