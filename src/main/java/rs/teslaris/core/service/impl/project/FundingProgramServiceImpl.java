package rs.teslaris.core.service.impl.project;

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
import rs.teslaris.core.converter.project.FundingProgramConverter;
import rs.teslaris.core.dto.document.DocumentFileDTO;
import rs.teslaris.core.dto.document.DocumentFileResponseDTO;
import rs.teslaris.core.dto.project.FundingProgramDTO;
import rs.teslaris.core.indexmodel.project.FundingProgramIndex;
import rs.teslaris.core.indexrepository.project.FundingProgramIndexRepository;
import rs.teslaris.core.model.document.AccessRights;
import rs.teslaris.core.model.project.FundingProgram;
import rs.teslaris.core.model.project.MonetaryAmount;
import rs.teslaris.core.repository.project.FundingProgramRepository;
import rs.teslaris.core.service.impl.JPAServiceImpl;
import rs.teslaris.core.service.interfaces.commontypes.CurrencyService;
import rs.teslaris.core.service.interfaces.commontypes.MultilingualContentService;
import rs.teslaris.core.service.interfaces.commontypes.ResearchAreaService;
import rs.teslaris.core.service.interfaces.commontypes.SearchService;
import rs.teslaris.core.service.interfaces.document.DocumentFileService;
import rs.teslaris.core.service.interfaces.institution.OrganisationUnitService;
import rs.teslaris.core.service.interfaces.project.FundingProgramService;
import rs.teslaris.core.util.exceptionhandling.exception.DateRangeException;
import rs.teslaris.core.util.exceptionhandling.exception.ReferenceConstraintException;
import rs.teslaris.core.util.functional.FunctionalUtil;
import rs.teslaris.core.util.search.StringUtil;

@Service
@RequiredArgsConstructor
public class FundingProgramServiceImpl extends JPAServiceImpl<FundingProgram>
    implements FundingProgramService {

    private final FundingProgramRepository fundingProgramRepository;

    private final DocumentFileService documentFileService;

    private final SearchService<FundingProgramIndex> searchService;

    private final MultilingualContentService multilingualContentService;

    private final ResearchAreaService researchAreaService;

    private final OrganisationUnitService organisationUnitService;

    private final CurrencyService currencyService;

    private final FundingProgramIndexRepository fundingProgramIndexRepository;


    @Override
    protected JpaRepository<FundingProgram, Integer> getEntityRepository() {
        return fundingProgramRepository;
    }

    @Override
    public Page<FundingProgramIndex> searchFundingPrograms(List<String> tokens, LocalDate dateFrom,
                                                           LocalDate dateTo, Integer funderId,
                                                           Pageable pageable) {
        return searchService.runQuery(buildSimpleSearchQuery(tokens, dateFrom, dateTo, funderId),
            pageable, FundingProgramIndex.class, "funding_program");
    }

    @Override
    @Transactional(readOnly = true)
    public FundingProgramDTO readFundingProgram(Integer fundingProgramId) {
        return FundingProgramConverter.toDTO(findOne(fundingProgramId));
    }

    @Override
    @Transactional
    public FundingProgram createFundingProgram(FundingProgramDTO fundingProgramDTO) {
        var newFundingProgram = new FundingProgram();

        setCommonFields(newFundingProgram, fundingProgramDTO);

        var savedFundingProgram = save(newFundingProgram);

        fundingProgramIndexRepository.save(
            indexCommonFields(savedFundingProgram, new FundingProgramIndex()));

        return savedFundingProgram;
    }

    @Override
    @Transactional
    public void updateFundingProgram(Integer fundingProgramId,
                                     FundingProgramDTO fundingProgramDTO) {
        var fundingProgramToUpdate = findOne(fundingProgramId);

        clearCommonFields(fundingProgramToUpdate);
        setCommonFields(fundingProgramToUpdate, fundingProgramDTO);

        fundingProgramIndexRepository.findFundingProgramIndexByDatabaseId(fundingProgramId)
            .ifPresent(index -> {
                indexCommonFields(fundingProgramToUpdate, index);
                fundingProgramIndexRepository.save(index);
            });
    }

    @Override
    @Transactional
    public void deleteFundingProgram(Integer fundingProgramId) {
        if (fundingProgramRepository.hasFundingCalls(fundingProgramId)) {
            throw new ReferenceConstraintException("fundingProgramHasCallsMessage");
        }

        delete(fundingProgramId);
    }

    @Override
    @Transactional
    public DocumentFileResponseDTO addFundingProgramDocument(Integer fundingProgramId,
                                                             DocumentFileDTO program) {
        var fundingProgram = findOne(fundingProgramId);
        program.setAccessRights(AccessRights.ALL_RIGHTS_RESERVED); // TODO: should we keep this?
        var documentFile = documentFileService.saveNewDocument(program, false);
        fundingProgram.getProgramDocuments().add(documentFile);

        save(fundingProgram);

        return DocumentFileConverter.toDTO(documentFile);
    }

    @Override
    @Transactional
    public DocumentFileResponseDTO updateFundingProgramDocument(DocumentFileDTO updatedProgram) {
        updatedProgram.setAccessRights(
            AccessRights.ALL_RIGHTS_RESERVED); // TODO: should we keep this?
        return documentFileService.editDocumentFile(updatedProgram, false);
    }

    @Override
    @Transactional
    public void deleteFundingProgramDocument(Integer programFileId, Integer fundingProgramId) {
        var documentFile = documentFileService.findOne(programFileId);
        var fundingProgram = findOne(fundingProgramId);
        fundingProgram.getProgramDocuments().remove(documentFile);

        documentFileService.delete(programFileId);
        save(fundingProgram);
    }

    @Override
    @Transactional(readOnly = true)
    public void reindexFundingPrograms() {
        FunctionalUtil.processAllPages(
            100,
            Sort.by(Sort.Direction.ASC, "id"),
            this::findAll,
            fundingProgram -> indexFundingProgram(fundingProgram, new FundingProgramIndex())
        );
    }

    @Override
    @Transactional(readOnly = true)
    public void indexFundingProgram(FundingProgram fundingProgram, FundingProgramIndex index) {
        indexCommonFields(fundingProgram, index);
        fundingProgramIndexRepository.save(index);
    }

    private void setCommonFields(FundingProgram fundingProgram,
                                 FundingProgramDTO fundingProgramDTO) {
        if (Objects.nonNull(fundingProgramDTO.getDateFrom()) &&
            Objects.nonNull(fundingProgramDTO.getDateTo()) &&
            fundingProgramDTO.getDateTo().isBefore(fundingProgramDTO.getDateFrom())) {
            throw new DateRangeException(
                "Funding program must opened before closing.");
        }

        fundingProgram.setName(
            multilingualContentService.getMultilingualContent(fundingProgramDTO.getName()));
        fundingProgram.setDescription(
            multilingualContentService.getMultilingualContent(fundingProgramDTO.getDescription()));
        fundingProgram.setObjectives(
            multilingualContentService.getMultilingualContent(fundingProgramDTO.getObjectives()));
        fundingProgram.setNameAbbreviation(multilingualContentService.getMultilingualContent(
            fundingProgramDTO.getNameAbbreviation()));
        fundingProgram.setKeywords(
            multilingualContentService.getMultilingualContent(fundingProgramDTO.getKeywords()));

        var researchAreas = researchAreaService.getResearchAreasByIds(
            fundingProgramDTO.getResearchAreasId().stream().toList());
        fundingProgram.setResearchAreas(new HashSet<>(researchAreas));

        fundingProgram.setFunder(organisationUnitService.findOne(fundingProgramDTO.getFunderId()));

        fundingProgram.setTypes(fundingProgramDTO.getFundingTypes());

        if (Objects.nonNull(fundingProgramDTO.getTotalAmount())) {
            if (Objects.isNull(fundingProgram.getTotalAmount())) {
                fundingProgram.setTotalAmount(new MonetaryAmount());
            }

            fundingProgram.getTotalAmount().setCurrency(
                currencyService.findOne(fundingProgramDTO.getTotalAmount().getCurrencyId()));
            fundingProgram.getTotalAmount()
                .setAmount(fundingProgramDTO.getTotalAmount().getAmount());
        } else {
            fundingProgram.setTotalAmount(null);
        }

        fundingProgram.setDateFrom(fundingProgramDTO.getDateFrom());
        fundingProgram.setDateTo(fundingProgramDTO.getDateTo());
        fundingProgram.setUris(fundingProgramDTO.getUris());
        fundingProgram.setOaMandated(fundingProgramDTO.getOaMandated());
        fundingProgram.setOaMandateUrl(fundingProgramDTO.getOaMandateUrl());
    }

    private void clearCommonFields(FundingProgram fundingProgram) {
        fundingProgram.getName().clear();
        fundingProgram.getDescription().clear();
        fundingProgram.getObjectives().clear();
        fundingProgram.getNameAbbreviation().clear();
        fundingProgram.getKeywords().clear();
        fundingProgram.getResearchAreas().clear();
    }

    private FundingProgramIndex indexCommonFields(FundingProgram fundingProgram,
                                                  FundingProgramIndex index) {
        var srContent = new StringBuilder();
        var otherContent = new StringBuilder();

        multilingualContentService.buildLanguageStrings(srContent, otherContent,
            fundingProgram.getName(), true);

        if (srContent.isEmpty() && !otherContent.isEmpty()) {
            srContent.append(otherContent);
        } else if (!srContent.isEmpty() && otherContent.isEmpty()) {
            otherContent.append(srContent);
        }

        multilingualContentService.buildLanguageStrings(srContent, otherContent,
            fundingProgram.getNameAbbreviation(), false);

        StringUtil.removeTrailingDelimiters(srContent, otherContent);
        index.setNameSr(!srContent.isEmpty() ? srContent.toString() : otherContent.toString());
        index.setNameSrSortable(index.getNameSr());
        index.setNameOther(
            !otherContent.isEmpty() ? otherContent.toString() : srContent.toString());
        index.setNameOtherSortable(index.getNameOther());

        index.setFunderId(fundingProgram.getFunder().getId());
        index.setDatabaseId(fundingProgram.getId());
        index.setDateFrom(fundingProgram.getDateFrom());
        index.setDateTo(fundingProgram.getDateTo());

        return index;
    }

    private Query buildSimpleSearchQuery(List<String> tokens,
                                         LocalDate dateFrom,
                                         LocalDate dateTo,
                                         Integer funderId) {
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
                            r.field("program_closes")
                                .gte(JsonData.of(dateFrom.toString()))
                        ));
                    }
                    if (Objects.nonNull(dateTo)) {
                        dateBool.must(m -> m.range(r ->
                            r.field("program_opens")
                                .lte(JsonData.of(dateTo.toString()))
                        ));
                    }

                    return dateBool;
                }));
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
