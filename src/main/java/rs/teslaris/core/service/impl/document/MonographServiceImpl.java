package rs.teslaris.core.service.impl.document;

import co.elastic.clients.elasticsearch._types.query_dsl.BoolQuery;
import co.elastic.clients.elasticsearch._types.query_dsl.Query;
import java.util.List;
import java.util.Objects;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import rs.teslaris.core.converter.document.MonographConverter;
import rs.teslaris.core.dto.document.MonographDTO;
import rs.teslaris.core.indexmodel.DocumentPublicationIndex;
import rs.teslaris.core.indexmodel.DocumentPublicationType;
import rs.teslaris.core.indexrepository.DocumentPublicationIndexRepository;
import rs.teslaris.core.model.commontypes.ApproveStatus;
import rs.teslaris.core.model.document.Monograph;
import rs.teslaris.core.repository.document.DocumentRepository;
import rs.teslaris.core.repository.document.MonographRepository;
import rs.teslaris.core.service.impl.document.cruddelegate.MonographJPAServiceImpl;
import rs.teslaris.core.service.interfaces.commontypes.LanguageTagService;
import rs.teslaris.core.service.interfaces.commontypes.MultilingualContentService;
import rs.teslaris.core.service.interfaces.commontypes.ResearchAreaService;
import rs.teslaris.core.service.interfaces.commontypes.SearchService;
import rs.teslaris.core.service.interfaces.document.BookSeriesService;
import rs.teslaris.core.service.interfaces.document.DocumentFileService;
import rs.teslaris.core.service.interfaces.document.EventService;
import rs.teslaris.core.service.interfaces.document.JournalService;
import rs.teslaris.core.service.interfaces.document.MonographService;
import rs.teslaris.core.service.interfaces.person.OrganisationUnitService;
import rs.teslaris.core.service.interfaces.person.PersonContributionService;
import rs.teslaris.core.util.IdentifierUtil;
import rs.teslaris.core.util.exceptionhandling.exception.MonographReferenceConstraintViolationException;
import rs.teslaris.core.util.exceptionhandling.exception.NotFoundException;
import rs.teslaris.core.util.search.ExpressionTransformer;

@Service
public class MonographServiceImpl extends DocumentPublicationServiceImpl implements
    MonographService {

    private final MonographJPAServiceImpl monographJPAService;

    private final LanguageTagService languageTagService;

    private final JournalService journalService;

    private final BookSeriesService bookSeriesService;

    private final ResearchAreaService researchAreaService;

    private final MonographRepository monographRepository;


    @Autowired
    public MonographServiceImpl(MultilingualContentService multilingualContentService,
                                DocumentPublicationIndexRepository documentPublicationIndexRepository,
                                SearchService<DocumentPublicationIndex> searchService,
                                OrganisationUnitService organisationUnitService,
                                DocumentRepository documentRepository,
                                DocumentFileService documentFileService,
                                PersonContributionService personContributionService,
                                ExpressionTransformer expressionTransformer,
                                EventService eventService,
                                MonographJPAServiceImpl monographJPAService,
                                LanguageTagService languageTagService,
                                JournalService journalService,
                                BookSeriesService bookSeriesService,
                                ResearchAreaService researchAreaService,
                                MonographRepository monographRepository) {
        super(multilingualContentService, documentPublicationIndexRepository, searchService,
            organisationUnitService, documentRepository, documentFileService,
            personContributionService,
            expressionTransformer, eventService);
        this.monographJPAService = monographJPAService;
        this.languageTagService = languageTagService;
        this.journalService = journalService;
        this.bookSeriesService = bookSeriesService;
        this.researchAreaService = researchAreaService;
        this.monographRepository = monographRepository;
    }

    @Override
    public Monograph findMonographById(Integer monographId) {
        return monographJPAService.findOne(monographId);
    }

    @Override
    public Page<DocumentPublicationIndex> searchMonographs(List<String> tokens) {
        return searchService.runQuery(buildSimpleSearchQuery(tokens), PageRequest.of(0, 5),
            DocumentPublicationIndex.class, "document_publication");
    }

    @Override
    public MonographDTO readMonographById(Integer monographId) {
        var monograph = monographJPAService.findOne(monographId);

        if (monograph.getApproveStatus().equals(ApproveStatus.DECLINED)) {
            throw new NotFoundException("Monograph with ID " + monographId + " does not exist.");
        }

        return MonographConverter.toDTO(monograph);
    }

    @Override
    public Monograph createMonograph(MonographDTO monographDTO, Boolean index) {
        var newMonograph = new Monograph();

        setCommonFields(newMonograph, monographDTO);
        setMonographRelatedFields(newMonograph, monographDTO);

        newMonograph.setApproveStatus(
            documentApprovedByDefault ? ApproveStatus.APPROVED : ApproveStatus.REQUESTED);

        var savedMonograph = monographJPAService.save(newMonograph);

        if (newMonograph.getApproveStatus().equals(ApproveStatus.APPROVED) && index) {
            indexMonograph(savedMonograph, new DocumentPublicationIndex());
        }

        sendNotifications(savedMonograph);

        return savedMonograph;
    }

    @Override
    public void updateMonograph(Integer monographId, MonographDTO monographDTO) {
        var monographToUpdate = monographJPAService.findOne(monographId);

        monographToUpdate.getLanguages().clear();
        clearCommonFields(monographToUpdate);

        setCommonFields(monographToUpdate, monographDTO);
        setMonographRelatedFields(monographToUpdate, monographDTO);

        if (monographToUpdate.getApproveStatus().equals(ApproveStatus.APPROVED)) {
            var monographIndex = findDocumentPublicationIndexByDatabaseId(monographId);
            indexMonograph(monographToUpdate, monographIndex);
        }

        monographJPAService.save(monographToUpdate);

        sendNotifications(monographToUpdate);
    }

    @Override
    public void deleteMonograph(Integer monographId) {
        var monographToDelete = monographJPAService.findOne(monographId);

        if (monographRepository.hasPublication(monographId)) {
            throw new MonographReferenceConstraintViolationException(
                "Monograph with given ID is in use and cannot be deleted.");
        }

        monographJPAService.delete(monographId);

        if (monographToDelete.getApproveStatus().equals(ApproveStatus.APPROVED)) {
            documentPublicationIndexRepository.delete(
                findDocumentPublicationIndexByDatabaseId(monographId));
        }
    }

    @Override
    public void reindexMonographs() {
        // Super service does the initial deletion

        int pageNumber = 0;
        int chunkSize = 10;
        boolean hasNextPage = true;

        while (hasNextPage) {

            List<Monograph> chunk =
                monographJPAService.findAll(PageRequest.of(pageNumber, chunkSize)).getContent();

            chunk.forEach((monograph) -> indexMonograph(monograph, new DocumentPublicationIndex()));

            pageNumber++;
            hasNextPage = chunk.size() == chunkSize;
        }
    }

    private void setMonographRelatedFields(Monograph monograph,
                                           MonographDTO monographDTO) {
        setCommonIdentifiers(monograph, monographDTO);

        monograph.setMonographType(monographDTO.getMonographType());
        monograph.setEISBN(monographDTO.getEisbn());
        monograph.setPrintISBN(monographDTO.getPrintISBN());
        monograph.setNumberOfPages(monographDTO.getNumberOfPages());
        monograph.setVolume(monographDTO.getVolume());
        monograph.setNumber(monographDTO.getNumber());

        monographDTO.getLanguageTagIds().forEach(id -> {
            monograph.getLanguages().add(languageTagService.findLanguageTagById(id));
        });

        if (Objects.nonNull(monographDTO.getPublicationSeriesId())) {
            var optionalJournal =
                journalService.tryToFindById(monographDTO.getPublicationSeriesId());

            if (optionalJournal.isPresent()) {
                monograph.setPublicationSeries(optionalJournal.get());
            } else {
                var bookSeries = bookSeriesService.findBookSeriesById(
                    monographDTO.getPublicationSeriesId());
                monograph.setPublicationSeries(bookSeries);
            }
        }

        if (Objects.nonNull(monographDTO.getResearchAreaId())) {
            monograph.setResearchArea(
                researchAreaService.findOne(monographDTO.getResearchAreaId()));
        }
    }

    private void indexMonograph(Monograph monograph, DocumentPublicationIndex index) {
        indexCommonFields(monograph, index);

        if (Objects.nonNull(monograph.getPublicationSeries())) {
            index.setPublicationSeriesId(monograph.getPublicationSeries().getId());
        }

        index.setType(DocumentPublicationType.MONOGRAPH.name());

        documentPublicationIndexRepository.save(index);
    }

    private void setCommonIdentifiers(Monograph monograph, MonographDTO monographDTO) {
        IdentifierUtil.validateAndSetIdentifier(
            monographDTO.getEisbn(),
            monograph.getId(),
            "^(?:(?:\\d[\\ |-]?){9}[\\dX]|(?:\\d[\\ |-]?){13})$",
            monographRepository::existsByeISBN,
            monograph::setEISBN,
            "eisbnFormatError",
            "eisbnExistsError"
        );

        IdentifierUtil.validateAndSetIdentifier(
            monographDTO.getPrintISBN(),
            monograph.getId(),
            "^(?:(?:\\d[\\ |-]?){9}[\\dX]|(?:\\d[\\ |-]?){13})$",
            monographRepository::existsByPrintISBN,
            monograph::setPrintISBN,
            "printIsbnFormatError",
            "printIsbnExistsError"
        );
    }

    private Query buildSimpleSearchQuery(List<String> tokens) {
        var minShouldMatch = (int) Math.ceil(tokens.size() * 0.8);

        return BoolQuery.of(q -> q.must(mb -> mb.bool(b -> {
            b.must(bq -> {
                bq.bool(eq -> {
                    tokens.forEach(token -> {
                        eq.should(sb -> sb.wildcard(
                            m -> m.field("title_sr").value(token).caseInsensitive(true)));
                        eq.should(sb -> sb.match(
                            m -> m.field("title_sr").query(token)));
                        eq.should(sb -> sb.wildcard(
                            m -> m.field("title_other").value(token).caseInsensitive(true)));
                        eq.should(sb -> sb.match(
                            m -> m.field("description_sr").query(token)));
                        eq.should(sb -> sb.match(
                            m -> m.field("description_other").query(token)));
                        eq.should(sb -> sb.wildcard(
                            m -> m.field("keywords_sr").value("*" + token + "*")));
                        eq.should(sb -> sb.wildcard(
                            m -> m.field("keywords_other").value("*" + token + "*")));
                        eq.should(sb -> sb.match(
                            m -> m.field("full_text_sr").query(token)));
                        eq.should(sb -> sb.match(
                            m -> m.field("full_text_other").query(token)));
                        eq.should(sb -> sb.match(
                            m -> m.field("author_names").query(token)));
                        eq.should(sb -> sb.match(
                            m -> m.field("editor_names").query(token)));
                        eq.should(sb -> sb.match(
                            m -> m.field("reviewer_names").query(token)));
                        eq.should(sb -> sb.match(
                            m -> m.field("advisor_names").query(token)));
                        eq.should(sb -> sb.match(
                            m -> m.field("doi").query(token)));
                    });
                    return eq.minimumShouldMatch(Integer.toString(minShouldMatch));
                });
                return bq;
            });
            b.must(sb -> sb.match(
                m -> m.field("type").query(DocumentPublicationType.MONOGRAPH.name())));
            return b;
        })))._toQuery();
    }
}
