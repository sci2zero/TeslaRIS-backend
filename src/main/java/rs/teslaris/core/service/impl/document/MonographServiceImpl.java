package rs.teslaris.core.service.impl.document;

import co.elastic.clients.elasticsearch._types.query_dsl.BoolQuery;
import co.elastic.clients.elasticsearch._types.query_dsl.Query;
import java.util.List;
import java.util.Objects;
import java.util.regex.Pattern;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import rs.teslaris.core.annotation.Traceable;
import rs.teslaris.core.converter.document.MonographConverter;
import rs.teslaris.core.dto.document.MonographDTO;
import rs.teslaris.core.indexmodel.DocumentPublicationIndex;
import rs.teslaris.core.indexmodel.DocumentPublicationType;
import rs.teslaris.core.indexrepository.DocumentPublicationIndexRepository;
import rs.teslaris.core.model.commontypes.ApproveStatus;
import rs.teslaris.core.model.document.Journal;
import rs.teslaris.core.model.document.Monograph;
import rs.teslaris.core.model.document.MonographType;
import rs.teslaris.core.repository.document.DocumentRepository;
import rs.teslaris.core.repository.document.MonographRepository;
import rs.teslaris.core.repository.institution.CommissionRepository;
import rs.teslaris.core.repository.person.InvolvementRepository;
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
import rs.teslaris.core.service.interfaces.document.PublisherService;
import rs.teslaris.core.service.interfaces.institution.OrganisationUnitOutputConfigurationService;
import rs.teslaris.core.service.interfaces.institution.OrganisationUnitService;
import rs.teslaris.core.service.interfaces.institution.OrganisationUnitTrustConfigurationService;
import rs.teslaris.core.service.interfaces.person.PersonContributionService;
import rs.teslaris.core.util.IdentifierUtil;
import rs.teslaris.core.util.exceptionhandling.exception.MonographReferenceConstraintViolationException;
import rs.teslaris.core.util.exceptionhandling.exception.NotFoundException;
import rs.teslaris.core.util.search.ExpressionTransformer;
import rs.teslaris.core.util.search.SearchFieldsLoader;
import rs.teslaris.core.util.search.StringUtil;
import rs.teslaris.core.util.tracing.SessionTrackingUtil;

@Service
@Traceable
public class MonographServiceImpl extends DocumentPublicationServiceImpl implements
    MonographService {

    private final MonographJPAServiceImpl monographJPAService;

    private final LanguageTagService languageTagService;

    private final JournalService journalService;

    private final BookSeriesService bookSeriesService;

    private final ResearchAreaService researchAreaService;

    private final MonographRepository monographRepository;

    private final PublisherService publisherService;

    private final Pattern doiPattern =
        Pattern.compile("^10\\.\\d{4,9}\\/[-,._;()/:A-Z0-9]+$", Pattern.CASE_INSENSITIVE);


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
                                CommissionRepository commissionRepository,
                                SearchFieldsLoader searchFieldsLoader,
                                OrganisationUnitTrustConfigurationService organisationUnitTrustConfigurationService,
                                InvolvementRepository involvementRepository,
                                OrganisationUnitOutputConfigurationService organisationUnitOutputConfigurationService,
                                MonographJPAServiceImpl monographJPAService,
                                LanguageTagService languageTagService,
                                JournalService journalService,
                                BookSeriesService bookSeriesService,
                                ResearchAreaService researchAreaService,
                                MonographRepository monographRepository,
                                PublisherService publisherService) {
        super(multilingualContentService, documentPublicationIndexRepository, searchService,
            organisationUnitService, documentRepository, documentFileService,
            personContributionService,
            expressionTransformer, eventService, commissionRepository, searchFieldsLoader,
            organisationUnitTrustConfigurationService, involvementRepository,
            organisationUnitOutputConfigurationService);
        this.monographJPAService = monographJPAService;
        this.languageTagService = languageTagService;
        this.journalService = journalService;
        this.bookSeriesService = bookSeriesService;
        this.researchAreaService = researchAreaService;
        this.monographRepository = monographRepository;
        this.publisherService = publisherService;
    }

    @Override
    public Monograph findMonographById(Integer monographId) {
        return monographJPAService.findOne(monographId);
    }

    @Override
    public Monograph findRaw(Integer monographId) {
        return monographRepository.findRaw(monographId)
            .orElseThrow(() -> new NotFoundException("Monograph with given ID does not exist."));
    }

    @Override
    public Page<DocumentPublicationIndex> searchMonographs(List<String> tokens, boolean onlyBooks) {
        return searchService.runQuery(buildSimpleSearchQuery(tokens, onlyBooks),
            PageRequest.of(0, 5),
            DocumentPublicationIndex.class, "document_publication");
    }

    @Override
    public MonographDTO readMonographById(Integer monographId) {
        Monograph monograph;
        try {
            monograph = monographJPAService.findOne(monographId);
        } catch (NotFoundException e) {
            this.clearIndexWhenFailedRead(monographId);
            throw e;
        }

        if (!SessionTrackingUtil.isUserLoggedIn() &&
            !monograph.getApproveStatus().equals(ApproveStatus.APPROVED)) {
            throw new NotFoundException("Monograph with ID " + monographId + " does not exist.");
        }

        return MonographConverter.toDTO(monograph);
    }

    @Override
    public Monograph findMonographByIsbn(String eIsbn, String printIsbn) {
        boolean isEisbnBlank = (Objects.isNull(eIsbn) || eIsbn.isBlank());
        boolean isPrintIsbnBlank = (Objects.isNull(printIsbn) || printIsbn.isBlank());

        if (isEisbnBlank && isPrintIsbnBlank) {
            return null;
        }

        if (isEisbnBlank) {
            eIsbn = printIsbn;
        } else if (isPrintIsbnBlank) {
            printIsbn = eIsbn;
        }

        var results = monographRepository.findByISBN(eIsbn, printIsbn);
        if (results.isEmpty()) {
            return null;
        }

        return results.getFirst();
    }

    @Override
    public Monograph createMonograph(MonographDTO monographDTO, Boolean index) {
        var newMonograph = new Monograph();

        setCommonFields(newMonograph, monographDTO);
        setMonographRelatedFields(newMonograph, monographDTO);

        var savedMonograph = monographJPAService.save(newMonograph);

        if (index) {
            indexMonograph(savedMonograph, new DocumentPublicationIndex());
        }

        sendNotifications(savedMonograph);

        return savedMonograph;
    }

    @Override
    public void editMonograph(Integer monographId, MonographDTO monographDTO) {
        var monographToUpdate = monographJPAService.findOne(monographId);

        monographToUpdate.getLanguages().clear();
        clearCommonFields(monographToUpdate);

        setCommonFields(monographToUpdate, monographDTO);
        setMonographRelatedFields(monographToUpdate, monographDTO);

        var monographIndex = findDocumentPublicationIndexByDatabaseId(monographId);
        indexMonograph(monographToUpdate, monographIndex);

        monographJPAService.save(monographToUpdate);

        sendNotifications(monographToUpdate);
    }

    @Override
    public void deleteMonograph(Integer monographId) {
        monographJPAService.findOne(monographId);

        if (monographRepository.hasPublication(monographId)) {
            throw new MonographReferenceConstraintViolationException(
                "Monograph with given ID is in use and cannot be deleted.");
        }

        monographJPAService.delete(monographId);

        documentPublicationIndexRepository.delete(
            findDocumentPublicationIndexByDatabaseId(monographId));
    }

    @Override
    public void forceDeleteMonograph(Integer monographId) {
        monographRepository.deleteAllPublicationsInMonograph(monographId);

        monographJPAService.delete(monographId);

        var index = documentPublicationIndexRepository.findDocumentPublicationIndexByDatabaseId(
            monographId);
        index.ifPresent(documentPublicationIndexRepository::delete);

        documentPublicationIndexRepository.deleteByMonographId(monographId);
    }

    @Override
    public void reindexMonographs() {
        // Super service does the initial deletion

        int pageNumber = 0;
        int chunkSize = 100;
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

        if (Objects.nonNull(monographDTO.getPublisherId())) {
            monograph.setPublisher(publisherService.findOne(monographDTO.getPublisherId()));
        }
    }

    @Override
    public void indexMonograph(Monograph monograph, DocumentPublicationIndex index) {
        indexCommonFields(monograph, index);

        if (Objects.nonNull(monograph.getPublicationSeries())) {
            index.setPublicationSeriesId(monograph.getPublicationSeries().getId());
            if (monograph.getPublicationSeries() instanceof Journal journal) {
                index.setJournalId(journal.getId());
            }
        }

        if (Objects.nonNull(monograph.getPublisher())) {
            index.setPublisherId(monograph.getPublisher().getId());
        }

        index.setType(DocumentPublicationType.MONOGRAPH.name());

        if (Objects.nonNull(monograph.getMonographType())) {
            index.setPublicationType(monograph.getMonographType().name());
        }

        documentPublicationIndexRepository.save(index);
    }

    @Override
    public void indexMonograph(Monograph monograph) {
        indexMonograph(monograph,
            documentPublicationIndexRepository.findDocumentPublicationIndexByDatabaseId(
                monograph.getId()).orElse(new DocumentPublicationIndex()));
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

    @Override
    public boolean isIdentifierInUse(String identifier, Integer monographId) {
        return monographRepository.existsByeISBN(identifier, monographId) ||
            monographRepository.existsByPrintISBN(identifier, monographId) ||
            super.isIdentifierInUse(identifier, monographId);
    }

    @Override
    public void addOldId(Integer id, Integer oldId) {
        var monograph = findOne(id);
        monograph.getOldIds().add(oldId);
        save(monograph);
    }

    private Query buildSimpleSearchQuery(List<String> tokens, boolean onlyBooks) {
        var minShouldMatch = String.valueOf(Math.ceil(0.8 * tokens.size()));

        return BoolQuery.of(q -> q.must(mb -> mb.bool(b -> {
            b.must(bq -> {
                bq.bool(eq -> {
                    tokens.forEach(token -> {
                        if (token.startsWith("\\\"") && token.endsWith("\\\"")) {
                            b.must(mp ->
                                mp.bool(m -> {
                                    {
                                        m.should(sb -> sb.matchPhrase(
                                            mq -> mq.field("title_sr")
                                                .query(token.replace("\\\"", ""))));
                                        m.should(sb -> sb.matchPhrase(
                                            mq -> mq.field("title_other")
                                                .query(token.replace("\\\"", ""))));
                                    }
                                    return m;
                                }));
                        } else if (token.contains("\\-") &&
                            doiPattern.matcher(token.replace("\\-", "-")).matches()) {
                            String normalizedToken = token.replace("\\-", "-");

                            b.should(mp -> mp.bool(m -> m
                                .should(sb -> sb.wildcard(
                                    mq -> mq.field("e_issn").value(normalizedToken)
                                        .caseInsensitive(true)))
                                .should(sb -> sb.wildcard(
                                    mq -> mq.field("print_issn").value(normalizedToken)
                                        .caseInsensitive(true)))
                            ));
                        } else if (token.endsWith("\\*") || token.endsWith(".")) {
                            var wildcard = token.replace("\\*", "").replace(".", "");
                            b.should(mp -> mp.bool(m -> m
                                .should(sb -> sb.wildcard(
                                    mq -> mq.field("title_sr").value(
                                            StringUtil.performSimpleLatinPreprocessing(wildcard) +
                                                "*")
                                        .caseInsensitive(true)))
                                .should(sb -> sb.wildcard(
                                    mq -> mq.field("title_other").value(wildcard + "*")
                                        .caseInsensitive(true)))
                            ));
                        } else {
                            var wildcard = token + "*";
                            b.should(mp -> mp.bool(m -> m
                                .should(sb -> sb.wildcard(
                                    mq -> mq.field("title_sr").value(
                                        StringUtil.performSimpleLatinPreprocessing(token) + "*")))
                                .should(sb -> sb.wildcard(
                                    mq -> mq.field("title_other").value(wildcard)))
                                .should(sb -> sb.match(
                                    mq -> mq.field("title_sr").query(token)))
                                .should(sb -> sb.match(
                                    mq -> mq.field("title_other").query(token)))
                                .should(sb -> sb.match(
                                    mq -> mq.field("author_names").query(token)))
                                .should(
                                    sb -> sb.wildcard(mq -> mq.field("author_names").value(
                                            StringUtil.performSimpleLatinPreprocessing(token) + "*")
                                        .caseInsensitive(true)))
                                .should(sb -> sb.match(
                                    mq -> mq.field("editor_names").query(token).boost(0.7f)))
                                .should(
                                    sb -> sb.wildcard(
                                        mq -> mq.field("editor_names").value(
                                                StringUtil.performSimpleLatinPreprocessing(token) +
                                                    "*").boost(0.7f)
                                            .caseInsensitive(true)))
                                .should(sb -> sb.match(
                                    mq -> mq.field("reviewer_names").query(token).boost(0.7f)))
                                .should(
                                    sb -> sb.wildcard(
                                        mq -> mq.field("reviewer_names").value(
                                                StringUtil.performSimpleLatinPreprocessing(token) +
                                                    "*").boost(0.7f)
                                            .caseInsensitive(true)))
                                .should(sb -> sb.match(
                                    mq -> mq.field("advisor_names").query(token).boost(0.7f)))
                                .should(
                                    sb -> sb.wildcard(
                                        mq -> mq.field("advisor_names").value(
                                                StringUtil.performSimpleLatinPreprocessing(token) +
                                                    "*").boost(0.7f)
                                            .caseInsensitive(true)))
                                .should(sb -> sb.match(
                                    mq -> mq.field("description_sr").query(token).boost(0.5f)))
                                .should(sb -> sb.match(
                                    mq -> mq.field("description_other").query(token).boost(0.5f)))
                                .should(sb -> sb.term(
                                    mq -> mq.field("keywords_sr").value(token)))
                                .should(sb -> sb.term(
                                    mq -> mq.field("keywords_other").value(token)))
                                .should(sb -> sb.match(
                                    mq -> mq.field("full_text_sr").query(token).boost(0.3f)))
                                .should(sb -> sb.match(
                                    mq -> mq.field("full_text_other").query(token).boost(0.3f)))
                            ));
                        }
                    });
                    return eq.minimumShouldMatch(minShouldMatch);
                });
                return bq;
            });
            b.must(sb -> sb.match(
                m -> m.field("type").query(DocumentPublicationType.MONOGRAPH.name())));

            if (onlyBooks) {
                b.must(sb -> sb.match(
                    m -> m.field("publication_type").query(MonographType.BOOK.name())));
            }
            return b;
        })))._toQuery();
    }
}
