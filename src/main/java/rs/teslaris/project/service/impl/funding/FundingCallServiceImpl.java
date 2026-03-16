package rs.teslaris.project.service.impl.funding;

import co.elastic.clients.elasticsearch._types.query_dsl.BoolQuery;
import co.elastic.clients.elasticsearch._types.query_dsl.Query;
import co.elastic.clients.json.JsonData;
import java.time.LocalDate;
import java.util.HashSet;
import java.util.List;
import java.util.Objects;
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
import rs.teslaris.core.util.exceptionhandling.exception.DateRangeException;
import rs.teslaris.core.util.exceptionhandling.exception.ReferenceConstraintException;
import rs.teslaris.core.util.functional.FunctionalUtil;
import rs.teslaris.core.util.search.StringUtil;
import rs.teslaris.project.converter.funding.FundingCallConverter;
import rs.teslaris.project.dto.funding.FundingCallDTO;
import rs.teslaris.project.indexmodel.funding.FundingCallIndex;
import rs.teslaris.project.indexrepository.funding.FundingCallIndexRepository;
import rs.teslaris.project.model.common.MonetaryAmount;
import rs.teslaris.project.model.funding.FundingCall;
import rs.teslaris.project.repository.funding.FundingCallRepository;
import rs.teslaris.project.service.interfaces.funding.FundingCallService;
import rs.teslaris.project.service.interfaces.funding.FundingProgramService;

@Service
@RequiredArgsConstructor
public class FundingCallServiceImpl extends JPAServiceImpl<FundingCall>
    implements FundingCallService {

    private final FundingCallRepository fundingCallRepository;

    private final DocumentFileService documentFileService;

    private final SearchService<FundingCallIndex> searchService;

    private final MultilingualContentService multilingualContentService;

    private final ResearchAreaService researchAreaService;

    private final FundingProgramService fundingProgramService;

    private final CurrencyService currencyService;

    private final FundingCallIndexRepository fundingCallIndexRepository;


    @Override
    protected JpaRepository<FundingCall, Integer> getEntityRepository() {
        return fundingCallRepository;
    }

    @Override
    public Page<FundingCallIndex> searchFundingCalls(List<String> tokens, LocalDate dateFrom,
                                                     LocalDate dateTo, Integer programId,
                                                     Pageable pageable) {
        return searchService.runQuery(buildSimpleSearchQuery(tokens, dateFrom, dateTo, programId),
            pageable, FundingCallIndex.class, "funding_call");
    }

    @Override
    @Transactional(readOnly = true)
    public FundingCallDTO readFundingCall(Integer fundingCallId) {
        return FundingCallConverter.toDTO(findOne(fundingCallId));
    }

    @Override
    @Transactional
    public FundingCall createFundingCall(FundingCallDTO fundingCallDTO) {
        var newFundingCall = new FundingCall();

        setCommonFields(newFundingCall, fundingCallDTO);

        var savedFundingCall = save(newFundingCall);

        fundingCallIndexRepository.save(
            indexCommonFields(savedFundingCall, new FundingCallIndex()));

        return savedFundingCall;
    }

    @Override
    @Transactional
    public void updateFundingCall(Integer fundingCallId,
                                  FundingCallDTO fundingCallDTO) {
        var fundingCallToUpdate = findOne(fundingCallId);

        clearCommonFields(fundingCallToUpdate);
        setCommonFields(fundingCallToUpdate, fundingCallDTO);

        fundingCallIndexRepository.findFundingCallIndexByDatabaseId(fundingCallId)
            .ifPresent(index -> {
                indexCommonFields(fundingCallToUpdate, index);
                fundingCallIndexRepository.save(index);
            });
    }

    @Override
    @Transactional
    public void deleteFundingCall(Integer fundingCallId) {
        if (fundingCallRepository.hasFunding(fundingCallId)) {
            throw new ReferenceConstraintException("fundingCallHasFundingMessage");
        }

        if (fundingCallRepository.hasFundingProposals(fundingCallId)) {
            throw new ReferenceConstraintException("fundingCallHasProposalsMessage");
        }

        delete(fundingCallId);
    }

    @Override
    @Transactional
    public DocumentFileResponseDTO addFundingCallDocument(Integer fundingCallId,
                                                          DocumentFileDTO call) {
        var fundingCall = findOne(fundingCallId);
        call.setAccessRights(AccessRights.ALL_RIGHTS_RESERVED); // TODO: should we keep this?
        var documentFile = documentFileService.saveNewDocument(call, false);
        fundingCall.getCallDocuments().add(documentFile);

        save(fundingCall);

        return DocumentFileConverter.toDTO(documentFile);
    }

    @Override
    @Transactional
    public DocumentFileResponseDTO updateFundingCallDocument(DocumentFileDTO updatedProgram) {
        updatedProgram.setAccessRights(
            AccessRights.ALL_RIGHTS_RESERVED); // TODO: should we keep this?
        return documentFileService.editDocumentFile(updatedProgram, false);
    }

    @Override
    @Transactional
    public void deleteFundingCallDocument(Integer callFileId, Integer fundingCallId) {
        var documentFile = documentFileService.findOne(callFileId);
        var fundingCall = findOne(fundingCallId);
        fundingCall.getCallDocuments().remove(documentFile);

        documentFileService.delete(callFileId);
        save(fundingCall);
    }

    @Override
    @Transactional(readOnly = true)
    public void reindexFundingCalls() {
        FunctionalUtil.processAllPages(
            100,
            Sort.by(Sort.Direction.ASC, "id"),
            this::findAll,
            fundingCall -> indexFundingCall(fundingCall, new FundingCallIndex())
        );
    }

    @Override
    @Transactional(readOnly = true)
    public void indexFundingCall(FundingCall fundingCall, FundingCallIndex index) {
        indexCommonFields(fundingCall, index);
        fundingCallIndexRepository.save(index);
    }

    private void setCommonFields(FundingCall fundingCall,
                                 FundingCallDTO fundingCallDTO) {
        var fundingProgram = fundingProgramService.findOne(fundingCallDTO.getFundingProgramId());
        fundingCall.setFundingProgram(fundingProgram);

        if (Objects.nonNull(fundingCallDTO.getDateFrom()) &&
            Objects.nonNull(fundingCallDTO.getDateTo()) &&
            fundingCallDTO.getDateTo().isBefore(fundingCallDTO.getDateFrom())) {
            throw new DateRangeException(
                "Funding call must opened before closing.");
        }

        if (Objects.nonNull(fundingProgram.getDateFrom()) &&
            fundingProgram.getDateFrom().isAfter(fundingCallDTO.getDateFrom())) {
            throw new DateRangeException(
                "Funding call opening must be equal or after program opening.");
        }

        if (Objects.nonNull(fundingProgram.getDateTo()) &&
            fundingProgram.getDateTo().isBefore(fundingCallDTO.getDateTo())) {
            throw new DateRangeException(
                "Funding call closing must be equal or before program closing.");
        }

        fundingCall.setDateFrom(fundingCallDTO.getDateFrom());
        fundingCall.setDateTo(fundingCallDTO.getDateTo());

        fundingCall.setName(
            multilingualContentService.getMultilingualContent(fundingCallDTO.getName()));
        fundingCall.setDescription(
            multilingualContentService.getMultilingualContent(fundingCallDTO.getDescription()));
        fundingCall.setObjectives(
            multilingualContentService.getMultilingualContent(fundingCallDTO.getObjectives()));
        fundingCall.setNameAbbreviation(multilingualContentService.getMultilingualContent(
            fundingCallDTO.getNameAbbreviation()));
        fundingCall.setKeywords(
            multilingualContentService.getMultilingualContent(fundingCallDTO.getKeywords()));

        var researchAreas = researchAreaService.getResearchAreasByIds(
            fundingCallDTO.getResearchAreasId().stream().toList());
        fundingCall.setResearchAreas(new HashSet<>(researchAreas));

        fundingCall.setTypes(fundingCallDTO.getFundingTypes());

        if (Objects.nonNull(fundingCallDTO.getMonetaryAmount())) {
            if (Objects.isNull(fundingCall.getAmount())) {
                fundingCall.setAmount(new MonetaryAmount());
            }

            fundingCall.getAmount().setCurrency(
                currencyService.findOne(fundingCallDTO.getMonetaryAmount().getCurrencyId()));
            fundingCall.getAmount().setAmount(fundingCallDTO.getMonetaryAmount().getAmount());
        } else {
            fundingCall.setAmount(null);
        }

        fundingCall.setUris(fundingCallDTO.getUris());
        fundingCall.setOaMandated(fundingCallDTO.getOaMandated());
        fundingCall.setOaMandateUrl(fundingCallDTO.getOaMandateUrl());
    }

    private void clearCommonFields(FundingCall fundingCall) {
        fundingCall.getName().clear();
        fundingCall.getDescription().clear();
        fundingCall.getObjectives().clear();
        fundingCall.getNameAbbreviation().clear();
        fundingCall.getKeywords().clear();
        fundingCall.getResearchAreas().clear();
    }

    private FundingCallIndex indexCommonFields(FundingCall fundingCall,
                                               FundingCallIndex index) {
        var srContent = new StringBuilder();
        var otherContent = new StringBuilder();

        multilingualContentService.buildLanguageStrings(srContent, otherContent,
            fundingCall.getName(), true);

        if (srContent.isEmpty() && !otherContent.isEmpty()) {
            srContent.append(otherContent);
        } else if (!srContent.isEmpty() && otherContent.isEmpty()) {
            otherContent.append(srContent);
        }

        multilingualContentService.buildLanguageStrings(srContent, otherContent,
            fundingCall.getNameAbbreviation(), false);

        StringUtil.removeTrailingDelimiters(srContent, otherContent);
        index.setNameSr(!srContent.isEmpty() ? srContent.toString() : otherContent.toString());
        index.setNameSrSortable(index.getNameSr());
        index.setNameOther(
            !otherContent.isEmpty() ? otherContent.toString() : srContent.toString());
        index.setNameOtherSortable(index.getNameOther());

        index.setProgramId(fundingCall.getFundingProgram().getId());
        index.setDatabaseId(fundingCall.getId());
        index.setDateFrom(fundingCall.getDateFrom());
        index.setDateTo(fundingCall.getDateTo());

        return index;
    }

    private Query buildSimpleSearchQuery(List<String> tokens,
                                         LocalDate dateFrom,
                                         LocalDate dateTo,
                                         Integer programId) {
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
                                ));
                            }
                        });

                        return eq.minimumShouldMatch(
                            Integer.toString(minShouldMatch)
                        );
                    });
                    return bq;
                });
            }

            if (Objects.nonNull(dateFrom) || Objects.nonNull(dateTo)) {
                b.must(sb -> sb.bool(dateBool -> {
                    if (Objects.nonNull(dateFrom)) {
                        dateBool.must(m -> m.range(r ->
                            r.field("call_closes")
                                .gte(JsonData.of(dateFrom.toString()))
                        ));
                    }
                    if (Objects.nonNull(dateTo)) {
                        dateBool.must(m -> m.range(r ->
                            r.field("call_opens")
                                .lte(JsonData.of(dateTo.toString()))
                        ));
                    }

                    return dateBool;
                }));
            }

            b.must(sb -> sb.term(
                m -> m.field("program_id").value(programId)
            ));

            return b;
        })))._toQuery();
    }
}
