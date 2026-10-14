package rs.teslaris.project.service.impl.funding;

import co.elastic.clients.elasticsearch._types.query_dsl.BoolQuery;
import co.elastic.clients.elasticsearch._types.query_dsl.Query;
import co.elastic.clients.json.JsonData;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Objects;
import java.util.concurrent.CompletableFuture;
import lombok.RequiredArgsConstructor;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import rs.teslaris.core.applicationevent.RevisionCreateEvent;
import rs.teslaris.core.applicationevent.RevisionType;
import rs.teslaris.core.converter.document.DocumentFileConverter;
import rs.teslaris.core.dto.document.DocumentFileDTO;
import rs.teslaris.core.dto.document.DocumentFileResponseDTO;
import rs.teslaris.core.indexmodel.EntityType;
import rs.teslaris.core.model.commontypes.MonetaryAmount;
import rs.teslaris.core.model.commontypes.ResearchArea;
import rs.teslaris.core.model.document.AccessRights;
import rs.teslaris.core.service.impl.JPAServiceImpl;
import rs.teslaris.core.service.interfaces.commontypes.CurrencyService;
import rs.teslaris.core.service.interfaces.commontypes.MultilingualContentService;
import rs.teslaris.core.service.interfaces.commontypes.ResearchAreaService;
import rs.teslaris.core.service.interfaces.commontypes.SearchService;
import rs.teslaris.core.service.interfaces.document.DocumentFileService;
import rs.teslaris.core.service.interfaces.institution.OrganisationUnitService;
import rs.teslaris.core.service.interfaces.person.InvolvementService;
import rs.teslaris.core.util.exceptionhandling.exception.DateRangeException;
import rs.teslaris.core.util.functional.FunctionalUtil;
import rs.teslaris.core.util.migration.MigrationContext;
import rs.teslaris.core.util.restoration.RestorationSupport;
import rs.teslaris.core.util.search.StringUtil;
import rs.teslaris.project.converter.funding.FundingConverter;
import rs.teslaris.project.dto.funding.FundingDTO;
import rs.teslaris.project.dto.funding.FundingPartDTO;
import rs.teslaris.project.indexmodel.funding.FundingIndex;
import rs.teslaris.project.indexrepository.funding.FundingIndexRepository;
import rs.teslaris.project.indexrepository.project.ProjectIndexRepository;
import rs.teslaris.project.model.funding.Funding;
import rs.teslaris.project.model.funding.FundingPart;
import rs.teslaris.project.repository.funding.FundingPartRepository;
import rs.teslaris.project.repository.funding.FundingRepository;
import rs.teslaris.project.service.interfaces.funding.FundingCallService;
import rs.teslaris.project.service.interfaces.funding.FundingService;
import rs.teslaris.project.service.interfaces.project.ProjectService;
import rs.teslaris.project.util.FundingPartFactory;

@Service
@RequiredArgsConstructor
public class FundingServiceImpl extends JPAServiceImpl<Funding> implements FundingService {

    private final ApplicationEventPublisher applicationEventPublisher;

    private final FundingRepository fundingRepository;

    private final FundingPartRepository fundingPartRepository;

    private final FundingPartFactory fundingPartFactory;

    private final SearchService<FundingIndex> searchService;

    private final ProjectService projectService;

    private final FundingCallService fundingCallService;

    private final OrganisationUnitService organisationUnitService;

    private final MultilingualContentService multilingualContentService;

    private final ResearchAreaService researchAreaService;

    private final CurrencyService currencyService;

    private final FundingIndexRepository fundingIndexRepository;

    private final ProjectIndexRepository projectIndexRepository;

    private final DocumentFileService documentFileService;

    private final InvolvementService involvementService;

    @Override
    protected JpaRepository<Funding, Integer> getEntityRepository() {
        return fundingRepository;
    }

    @Override
    public Page<FundingIndex> searchFunding(List<String> tokens, LocalDate dateFrom,
                                            LocalDate dateTo, Integer projectId,
                                            Integer fundingCallId, Integer funderId,
                                            Pageable pageable) {
        return searchService.runQuery(buildSimpleSearchQuery(tokens, dateFrom, dateTo, projectId,
                fundingCallId, funderId),
            pageable, FundingIndex.class, "funding");
    }

    @Override
    @Transactional(readOnly = true)
    public FundingDTO readFunding(Integer fundingId) {
        return FundingConverter.toDTO(findOne(fundingId));
    }

    @Override
    @Transactional
    public Funding createFunding(FundingDTO fundingDTO) {
        var newFunding = new Funding();

        setCommonFields(newFunding, fundingDTO);

        var savedFunding = save(newFunding);

        buildFundingParts(savedFunding, fundingDTO);

        fundingIndexRepository.save(
            indexCommonFields(savedFunding, new FundingIndex()));

        if (!MigrationContext.isActive()) {
            applicationEventPublisher.publishEvent(
                new RevisionCreateEvent(
                    EntityType.FUNDING.name(),
                    savedFunding.getId(),
                    null,
                    FundingConverter.toDTO(savedFunding),
                    RevisionType.CREATE
                )
            );
        }

        return savedFunding;
    }

    @Override
    @Transactional
    public void updateFunding(Integer fundingId,
                              FundingDTO fundingDTO) {
        var fundingToUpdate = findOne(fundingId);

        applicationEventPublisher.publishEvent(
            new RevisionCreateEvent(
                EntityType.FUNDING.name(),
                fundingId,
                FundingConverter.toDTO(fundingToUpdate),
                fundingDTO,
                RevisionType.UPDATE
            )
        );

        clearCommonFields(fundingToUpdate);
        setCommonFields(fundingToUpdate, fundingDTO);

        fundingIndexRepository.findFundingIndexByDatabaseId(fundingId)
            .ifPresent(index -> {
                indexCommonFields(fundingToUpdate, index);
                fundingIndexRepository.save(index);
            });
    }

    @Override
    @Transactional
    public void deleteFunding(Integer fundingId) {
        delete(fundingId);
    }

    @Override
    @Transactional
    public DocumentFileResponseDTO addAgreementDocument(Integer fundingId,
                                                        DocumentFileDTO agreement) {
        var funding = findOne(fundingId);
        agreement.setAccessRights(AccessRights.ALL_RIGHTS_RESERVED);
        var documentFile = documentFileService.saveNewDocument(agreement, false);
        funding.getAgreements().add(documentFile);

        save(funding);

        return DocumentFileConverter.toDTO(documentFile);
    }

    @Override
    @Transactional
    public DocumentFileResponseDTO updateAgreementDocument(DocumentFileDTO updatedAgreement) {
        updatedAgreement.setAccessRights(
            AccessRights.ALL_RIGHTS_RESERVED);
        return documentFileService.editDocumentFile(updatedAgreement, false);
    }

    @Override
    @Transactional
    public void deleteAgreementDocument(Integer agreementFileId, Integer fundingId) {
        var documentFile = documentFileService.findOne(agreementFileId);
        var fundingCall = findOne(fundingId);
        fundingCall.getAgreements().remove(documentFile);

        documentFileService.delete(agreementFileId);
        save(fundingCall);
    }

    private void setCommonFields(Funding funding, FundingDTO fundingDTO) {
        if (Objects.nonNull(fundingDTO.getDateFrom()) &&
            Objects.nonNull(fundingDTO.getDateTo()) &&
            fundingDTO.getDateTo().isBefore(fundingDTO.getDateFrom())) {
            throw new DateRangeException("Funding must start before it ends.");
        }

        // A funding detached from its project disappears from that project's fundings, which is
        // worse than refusing the restore outright.
        RestorationSupport.requireExists(fundingDTO.getProjectId(), projectService, "projectId");

        if (Objects.nonNull(fundingDTO.getProjectId())) {
            var project = projectService.findOne(fundingDTO.getProjectId());
            funding.setProject(project);
        } else {
            funding.setProject(null);
        }

        funding.setFundingCall(RestorationSupport.resolveDegradable(
            fundingDTO.getFundingCallId(), fundingCallService, fundingCallService::findOne,
            "fundingCallId", "restoreFundingCallMissingMessage",
            List.of(String.valueOf(fundingDTO.getFundingCallId()))));

        funding.setFunder(RestorationSupport.resolveDegradable(
            fundingDTO.getFunderId(), organisationUnitService, organisationUnitService::findOne,
            "funderId", "restoreFunderMissingMessage",
            List.of(String.valueOf(fundingDTO.getFunderId()))));

        funding.setInvolvement(RestorationSupport.resolveOptional(
            fundingDTO.getInvolvementId(), involvementService, involvementService::findOne,
            "involvementId", "restoreInvolvementMissingMessage"));

        funding.setDateSubmitted(fundingDTO.getDateSubmitted());
        funding.setDateAwarded(fundingDTO.getDateAwarded());
        funding.setDateFrom(fundingDTO.getDateFrom());
        funding.setDateTo(fundingDTO.getDateTo());

        funding.setName(multilingualContentService.getMultilingualContent(fundingDTO.getName()));
        funding.setDescription(
            multilingualContentService.getMultilingualContent(fundingDTO.getDescription()));
        funding.setNameAbbreviation(
            multilingualContentService.getMultilingualContent(fundingDTO.getNameAbbreviation()));
        funding.setKeywords(
            multilingualContentService.getMultilingualContent(fundingDTO.getKeywords()));

        funding.setDisplayCall(
            multilingualContentService.getMultilingualContent(fundingDTO.getDisplayCall()));
        funding.setDisplayProgram(
            multilingualContentService.getMultilingualContent(fundingDTO.getDisplayProgram()));
        funding.setDisplayFunder(
            multilingualContentService.getMultilingualContent(fundingDTO.getDisplayFunder()));

        var requestedResearchAreaIds = fundingDTO.getResearchAreasId().stream().toList();
        var researchAreas = researchAreaService.getResearchAreasByIds(requestedResearchAreaIds);

        RestorationSupport.reportMissingFromBulkLookup(requestedResearchAreaIds,
            researchAreas.stream().map(ResearchArea::getId).toList(), "researchAreasId",
            "restoreResearchAreaMissingMessage");

        funding.setResearchAreas(new HashSet<>(researchAreas));

        funding.setFundingTypes(fundingDTO.getFundingTypes());

        // An amount without its currency is a number without a unit, so the whole amount goes.
        var currency = Objects.nonNull(fundingDTO.getAmount())
            ? RestorationSupport.resolveOptional(fundingDTO.getAmount().getCurrencyId(),
            currencyService, currencyService::findOne, "amount.currencyId",
            "restoreCurrencyMissingMessage")
            : null;

        if (Objects.nonNull(fundingDTO.getAmount()) && Objects.nonNull(currency)) {
            if (Objects.isNull(funding.getAmount())) {
                funding.setAmount(new MonetaryAmount());
            }
            funding.getAmount().setCurrency(currency);
            funding.getAmount().setAmount(fundingDTO.getAmount().getAmount());
        } else {
            funding.setAmount(null);
        }

        funding.setUris(fundingDTO.getUris());
        funding.setDoi(fundingDTO.getDoi());
        funding.setGrantAgreementId(fundingDTO.getGrantAgreementId());
        funding.setCompetitive(fundingDTO.getCompetitive());
        funding.setRenewable(fundingDTO.getRenewable());
        funding.setOaMandated(fundingDTO.getOaMandated());
        funding.setOaMandateUrl(fundingDTO.getOaMandateUrl());
        funding.setInternalIdentifiers(fundingDTO.getInternalIdentifiers());
        funding.setInternalInvestment(fundingDTO.getInternalInvestment());
    }

    private void buildFundingParts(Funding funding,
                                   FundingDTO fundingDTO) {
        if (Objects.isNull(funding.getFundingParts())) {
            funding.setFundingParts(new HashSet<>());
        }

        fundingDTO.getFundingParts().forEach(partDTO -> {
            var part = buildFundingPart(partDTO, funding);
            funding.getFundingParts().add(fundingPartRepository.save(part));
        });
    }

    private FundingPart buildFundingPart(FundingPartDTO partDTO, Funding parent) {
        var part = fundingPartFactory.buildFundingPart(partDTO);
        part.setFunding(parent);

        return part;
    }

    @Override
    @Transactional(readOnly = true)
    public CompletableFuture<Void> reindexFunding() {
        fundingIndexRepository.deleteAll();

        FunctionalUtil.processAllPages(
            100,
            Sort.by(Sort.Direction.ASC, "id"),
            this::findAll,
            funding -> indexFunding(funding, new FundingIndex())
        );

        return CompletableFuture.completedFuture(null);
    }

    @Override
    @Transactional(readOnly = true)
    public void indexFunding(Funding funding, FundingIndex index) {
        indexCommonFields(funding, index);
        fundingIndexRepository.save(index);
    }

    private void clearCommonFields(Funding funding) {
        funding.getName().clear();
        funding.getDescription().clear();
        funding.getNameAbbreviation().clear();
        funding.getKeywords().clear();
        funding.getDisplayCall().clear();
        funding.getDisplayProgram().clear();
        funding.getDisplayFunder().clear();
        funding.getResearchAreas().clear();
    }

    private FundingIndex indexCommonFields(Funding funding,
                                           FundingIndex index) {
        var srContent = new StringBuilder();
        var otherContent = new StringBuilder();

        multilingualContentService.buildLanguageStrings(srContent, otherContent,
            funding.getName(), true);

        if (srContent.isEmpty() && !otherContent.isEmpty()) {
            srContent.append(otherContent);
        } else if (!srContent.isEmpty() && otherContent.isEmpty()) {
            otherContent.append(srContent);
        }

        multilingualContentService.buildLanguageStrings(srContent, otherContent,
            funding.getNameAbbreviation(), false);

        StringUtil.removeTrailingDelimiters(srContent, otherContent);
        index.setNameSr(!srContent.isEmpty() ? srContent.toString() : otherContent.toString());
        index.setNameSrSortable(index.getNameSr());
        index.setNameOther(
            !otherContent.isEmpty() ? otherContent.toString() : srContent.toString());
        index.setNameOtherSortable(index.getNameOther());

        if (Objects.nonNull(funding.getFunder())) {
            indexFunderFields(funding, index);
        }

        if (Objects.nonNull(funding.getProject())) {
            index.setProjectId(funding.getProject().getId());
        }

        indexProjectScope(funding, index);

        if (Objects.nonNull(funding.getFundingCall())) {
            index.setFundingCallId(funding.getFundingCall().getId());
        } else {
            index.setFundingCallId(null);
        }

        index.setDatabaseId(funding.getId());
        index.setDateFrom(funding.getDateFrom());
        index.setDateTo(funding.getDateTo());

        return index;
    }

    /**
     * A funding has no contributors of its own, so the institutions it belongs to are the funded
     * project's. The project index already holds them expanded over the super hierarchy, so it is
     * read rather than recomputed.
     */
    private void indexProjectScope(Funding funding, FundingIndex index) {
        if (Objects.isNull(funding.getProject())) {
            index.setPersonIds(new ArrayList<>());
            index.setOrganisationUnitIds(new ArrayList<>());
            return;
        }

        projectIndexRepository.findProjectIndexByDatabaseId(funding.getProject().getId())
            .ifPresentOrElse(projectIndex -> {
                index.setPersonIds(new ArrayList<>(projectIndex.getPersonIds()));
                index.setOrganisationUnitIds(
                    new ArrayList<>(projectIndex.getOrganisationUnitIds()));
            }, () -> {
                index.setPersonIds(new ArrayList<>());
                index.setOrganisationUnitIds(new ArrayList<>());
            });
    }

    private void indexFunderFields(Funding funding,
                                   FundingIndex index) {
        var srContent = new StringBuilder();
        var otherContent = new StringBuilder();

        multilingualContentService.buildLanguageStrings(srContent, otherContent,
            funding.getFunder().getName(), true);

        if (srContent.isEmpty() && !otherContent.isEmpty()) {
            srContent.append(otherContent);
        } else if (!srContent.isEmpty() && otherContent.isEmpty()) {
            otherContent.append(srContent);
        }

        multilingualContentService.buildLanguageStrings(srContent, otherContent,
            funding.getFunder().getNameAbbreviation(), false);

        StringUtil.removeTrailingDelimiters(srContent, otherContent);
        index.setFunderNameSr(
            !srContent.isEmpty() ? srContent.toString() : otherContent.toString());
        index.setFunderNameSrSortable(index.getFunderNameSr());
        index.setFunderNameOther(
            !otherContent.isEmpty() ? otherContent.toString() : srContent.toString());
        index.setFunderNameOtherSortable(index.getFunderNameOther());

        index.setFunderId(funding.getFunder().getId());
    }

    private Query buildSimpleSearchQuery(List<String> tokens, LocalDate dateFrom,
                                         LocalDate dateTo, Integer projectId,
                                         Integer fundingCallId, Integer funderId) {
        var minShouldMatch = (Objects.isNull(tokens) || tokens.isEmpty())
            ? 0
            : (int) Math.ceil(tokens.size() * 0.8);

        return BoolQuery.of(q -> q.must(mb -> mb.bool(b -> {
            if (Objects.nonNull(tokens) && !tokens.isEmpty()) {
                b.must(bq -> {
                    bq.bool(eq -> {
                        tokens.forEach(token -> {
                            if (token.startsWith("\"") && token.endsWith("\"")) {
                                eq.must(mp ->
                                    mp.bool(m -> m
                                        .should(sb -> sb.matchPhrase(
                                            mq -> mq.field("name_sr")
                                                .query(token.replace("\"", ""))))
                                        .should(sb -> sb.matchPhrase(
                                            mq -> mq.field("name_other")
                                                .query(token.replace("\"", ""))))
                                        .should(sb -> sb.matchPhrase(
                                            mq -> mq.field("funder_name_sr")
                                                .query(token.replace("\"", ""))))
                                        .should(sb -> sb.matchPhrase(
                                            mq -> mq.field("funder_name_other")
                                                .query(token.replace("\"", ""))))
                                    )
                                );
                            } else if (token.endsWith("*")) {
                                var wildcard = token.replace("*", "").replace(".", "");

                                eq.should(mp -> mp.bool(m -> m
                                    .should(sb -> sb.wildcard(
                                        mq -> mq.field("name_sr")
                                            .value(StringUtil.performSimpleLatinPreprocessing(
                                                wildcard) + "*")
                                            .caseInsensitive(true)))
                                    .should(sb -> sb.wildcard(
                                        mq -> mq.field("name_other")
                                            .value(wildcard + "*")
                                            .caseInsensitive(true)))
                                    .should(sb -> sb.wildcard(
                                        mq -> mq.field("funder_name_sr")
                                            .value(StringUtil.performSimpleLatinPreprocessing(
                                                wildcard) + "*")
                                            .caseInsensitive(true)))
                                    .should(sb -> sb.wildcard(
                                        mq -> mq.field("funder_name_other")
                                            .value(wildcard + "*")
                                            .caseInsensitive(true)))
                                ));
                            } else {
                                var wildcard = token + "*";

                                eq.should(mp -> mp.bool(m -> m
                                    .should(sb -> sb.wildcard(
                                        mq -> mq.field("name_sr")
                                            .value(
                                                StringUtil.performSimpleLatinPreprocessing(token) +
                                                    "*")
                                            .caseInsensitive(true)))
                                    .should(sb -> sb.wildcard(
                                        mq -> mq.field("name_other")
                                            .value(wildcard)
                                            .caseInsensitive(true)))
                                    .should(sb -> sb.match(
                                        mq -> mq.field("name_sr")
                                            .query(token)))
                                    .should(sb -> sb.match(
                                        mq -> mq.field("name_other")
                                            .query(token)))
                                    .should(sb -> sb.wildcard(
                                        mq -> mq.field("funder_name_sr")
                                            .value(
                                                StringUtil.performSimpleLatinPreprocessing(token) +
                                                    "*")
                                            .caseInsensitive(true)))
                                    .should(sb -> sb.wildcard(
                                        mq -> mq.field("funder_name_other")
                                            .value(wildcard)
                                            .caseInsensitive(true)))
                                    .should(sb -> sb.match(
                                        mq -> mq.field("funder_name_sr")
                                            .query(token)))
                                    .should(sb -> sb.match(
                                        mq -> mq.field("funder_name_other")
                                            .query(token)))
                                ));
                            }
                        });

                        return eq.minimumShouldMatch(Integer.toString(minShouldMatch));
                    });
                    return bq;
                });
            }

            if (Objects.nonNull(dateFrom) || Objects.nonNull(dateTo)) {
                b.must(sb -> sb.bool(dateBool -> {
                    if (Objects.nonNull(dateFrom)) {
                        dateBool.must(m -> m.range(r ->
                            r.field("date_from")
                                .gte(JsonData.of(dateFrom.toString()))
                        ));
                    }
                    if (Objects.nonNull(dateTo)) {
                        dateBool.must(m -> m.range(r ->
                            r.field("date_to")
                                .lte(JsonData.of(dateTo.toString()))
                        ));
                    }
                    return dateBool;
                }));
            }

            // Check if these are necessary?
            if (Objects.nonNull(projectId)) {
                b.must(sb -> sb.term(
                    m -> m.field("project_id").value(projectId)
                ));
            }

            if (Objects.nonNull(fundingCallId)) {
                b.must(sb -> sb.term(
                    m -> m.field("funding_call_id").value(fundingCallId)
                ));
            }

            if (Objects.nonNull(funderId)) {
                b.must(sb -> sb.term(
                    m -> m.field("funder_id").value(funderId)
                ));
            }

            return b;
        })))._toQuery();
    }

}
