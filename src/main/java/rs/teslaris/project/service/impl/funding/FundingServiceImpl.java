package rs.teslaris.project.service.impl.funding;

import co.elastic.clients.elasticsearch._types.query_dsl.BoolQuery;
import co.elastic.clients.elasticsearch._types.query_dsl.Query;
import co.elastic.clients.json.JsonData;
import java.time.LocalDate;
import java.util.HashSet;
import java.util.List;
import java.util.Objects;
import java.util.concurrent.CompletableFuture;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import rs.teslaris.core.converter.document.DocumentFileConverter;
import rs.teslaris.core.dto.document.DocumentFileDTO;
import rs.teslaris.core.dto.document.DocumentFileResponseDTO;
import rs.teslaris.core.model.document.AccessRights;
import rs.teslaris.core.service.impl.JPAServiceImpl;
import rs.teslaris.core.service.interfaces.commontypes.CurrencyService;
import rs.teslaris.core.service.interfaces.commontypes.MultilingualContentService;
import rs.teslaris.core.service.interfaces.commontypes.ResearchAreaService;
import rs.teslaris.core.service.interfaces.commontypes.SearchService;
import rs.teslaris.core.service.interfaces.document.DocumentFileService;
import rs.teslaris.core.service.interfaces.institution.OrganisationUnitService;
import rs.teslaris.core.util.exceptionhandling.exception.DateRangeException;
import rs.teslaris.core.util.exceptionhandling.exception.ReferenceConstraintException;
import rs.teslaris.core.util.functional.FunctionalUtil;
import rs.teslaris.core.util.search.StringUtil;
import rs.teslaris.project.converter.funding.FundingConverter;
import rs.teslaris.project.dto.funding.FundingDTO;
import rs.teslaris.project.dto.funding.FundingPartDTO;
import rs.teslaris.project.indexmodel.funding.FundingIndex;
import rs.teslaris.project.indexrepository.funding.FundingIndexRepository;
import rs.teslaris.project.model.common.MonetaryAmount;
import rs.teslaris.project.model.funding.Funding;
import rs.teslaris.project.model.funding.FundingPart;
import rs.teslaris.project.repository.funding.FundingRepository;
import rs.teslaris.project.service.interfaces.funding.FundingCallService;
import rs.teslaris.project.service.interfaces.funding.FundingService;
import rs.teslaris.project.service.interfaces.project.ProjectService;

@Service
@RequiredArgsConstructor
public class FundingServiceImpl extends JPAServiceImpl<Funding> implements FundingService {

    private final FundingRepository fundingRepository;

    private final SearchService<FundingIndex> searchService;

    private final ProjectService projectService;

    private final FundingCallService fundingCallService;

    private final OrganisationUnitService organisationUnitService;

    private final MultilingualContentService multilingualContentService;

    private final ResearchAreaService researchAreaService;

    private final CurrencyService currencyService;

    private final FundingIndexRepository fundingIndexRepository;
    private final DocumentFileService documentFileService;

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

        var savedFundingCall = save(newFunding);

        fundingIndexRepository.save(
            indexCommonFields(savedFundingCall, new FundingIndex()));

        return savedFundingCall;
    }

    @Override
    @Transactional
    public void updateFunding(Integer fundingId,
                              FundingDTO fundingDTO) {
        var fundingToUpdate = findOne(fundingId);

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

        if (Objects.nonNull(fundingDTO.getProjectId())) {
            var project = projectService.findOne(fundingDTO.getProjectId());
            funding.setProject(project);
        } else {
            throw new ReferenceConstraintException("Funding must be bound to a project.");
        }

        if (Objects.nonNull(fundingDTO.getFundingCallId())) {
            var fundingCall = fundingCallService.findOne(fundingDTO.getFundingCallId());
            funding.setFundingCall(fundingCall);
        } else {
            funding.setFundingCall(null);
        }

        if (Objects.nonNull(fundingDTO.getFunderId())) {
            funding.setFunder(organisationUnitService.findOne(fundingDTO.getFunderId()));
        } else {
            funding.setFunder(null);
        }

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

        var researchAreas = researchAreaService.getResearchAreasByIds(
            fundingDTO.getResearchAreasId().stream().toList());
        funding.setResearchAreas(new HashSet<>(researchAreas));

        funding.setFundingTypes(fundingDTO.getFundingTypes());

        if (Objects.nonNull(fundingDTO.getAmount())) {
            if (Objects.isNull(funding.getAmount())) {
                funding.setAmount(new MonetaryAmount());
            }
            funding.getAmount()
                .setCurrency(currencyService.findOne(fundingDTO.getAmount().getCurrencyId()));
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

        buildFundingParts(funding, fundingDTO);
    }

    private void buildFundingParts(Funding funding,
                                   FundingDTO fundingDTO) {
        if (Objects.isNull(funding.getFundingParts())) {
            funding.setFundingParts(new HashSet<>());
        }

        fundingDTO.getFundingParts().forEach(partDTO -> {
            var part = buildFundingPart(partDTO, funding);
            funding.getFundingParts().add(part);
        });
    }

    private FundingPart buildFundingPart(FundingPartDTO partDTO, Funding parent) {
        var part = new FundingPart();

        part.setDescription(
            multilingualContentService.getMultilingualContent(partDTO.getDescription()));

        part.setAmount(new MonetaryAmount());
        part.getAmount().setCurrency(
            currencyService.findOne(partDTO.getAmount().getCurrencyId()));
        part.getAmount().setAmount(partDTO.getAmount().getAmount());

        if (Objects.nonNull(partDTO.getFundingId())) {
            part.setFunding(parent);
        }

        return part;
    }

    @Override
    @Transactional(readOnly = true)
    public CompletableFuture<Void> reindexFunding() {
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

        if (Objects.nonNull(funding.getFundingCall())) {
            index.setFundingCallId(funding.getFundingCall().getId());
        }

        index.setDatabaseId(funding.getId());
        index.setDateFrom(funding.getDateFrom());
        index.setDateTo(funding.getDateTo());

        return index;
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
