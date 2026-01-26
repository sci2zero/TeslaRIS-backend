package rs.teslaris.core.service.impl.person;

import co.elastic.clients.elasticsearch._types.FieldValue;
import co.elastic.clients.elasticsearch._types.query_dsl.BoolQuery;
import co.elastic.clients.elasticsearch._types.query_dsl.Query;
import jakarta.annotation.Nullable;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.Set;
import java.util.concurrent.CompletableFuture;
import java.util.function.BiConsumer;
import java.util.function.Function;
import java.util.stream.Collectors;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import rs.teslaris.core.annotation.Traceable;
import rs.teslaris.core.converter.commontypes.MultilingualContentConverter;
import rs.teslaris.core.converter.document.DocumentFileConverter;
import rs.teslaris.core.dto.document.DocumentFileDTO;
import rs.teslaris.core.dto.document.DocumentFileResponseDTO;
import rs.teslaris.core.dto.person.PrizeDTO;
import rs.teslaris.core.dto.person.PrizeResponseDTO;
import rs.teslaris.core.indexmodel.PrizeIndex;
import rs.teslaris.core.indexrepository.PersonIndexRepository;
import rs.teslaris.core.indexrepository.PrizeIndexRepository;
import rs.teslaris.core.model.commontypes.MultiLingualContent;
import rs.teslaris.core.model.person.Prize;
import rs.teslaris.core.repository.institution.CommissionRepository;
import rs.teslaris.core.repository.person.PrizeRepository;
import rs.teslaris.core.service.impl.JPAServiceImpl;
import rs.teslaris.core.service.interfaces.commontypes.MultilingualContentService;
import rs.teslaris.core.service.interfaces.commontypes.SearchService;
import rs.teslaris.core.service.interfaces.document.DocumentFileService;
import rs.teslaris.core.service.interfaces.institution.OrganisationUnitService;
import rs.teslaris.core.service.interfaces.person.PersonService;
import rs.teslaris.core.service.interfaces.person.PrizeService;
import rs.teslaris.core.util.functional.FunctionalUtil;
import rs.teslaris.core.util.functional.Triple;
import rs.teslaris.core.util.search.StringUtil;

@Service
@RequiredArgsConstructor
@Traceable
public class PrizeServiceImpl extends JPAServiceImpl<Prize> implements PrizeService {

    protected final SearchService<PrizeIndex> searchService;
    private final PrizeRepository prizeRepository;
    private final PersonService personService;
    private final MultilingualContentService multilingualContentService;
    private final DocumentFileService documentFileService;
    private final PrizeIndexRepository prizeIndexRepository;
    private final PersonIndexRepository personIndexRepository;
    private final CommissionRepository commissionRepository;
    private final OrganisationUnitService organisationUnitService;

    @Override
    protected JpaRepository<Prize, Integer> getEntityRepository() {
        return prizeRepository;
    }

    @Override
    @Transactional
    public PrizeResponseDTO addPrize(Integer personId, PrizeDTO dto) {
        var newPrize = new Prize();
        var person = personService.findOne(personId);

        setCommonFields(newPrize, dto);
        newPrize.setPerson(person);
        var savedPrize = prizeRepository.save(newPrize);
        prizeRepository.flush();

        person.addPrize(savedPrize);
        personService.save(person);

        indexPrize(savedPrize, new PrizeIndex());

        return new PrizeResponseDTO(
            MultilingualContentConverter.getMultilingualContentDTO(savedPrize.getTitle()),
            MultilingualContentConverter.getMultilingualContentDTO(savedPrize.getDescription()),
            savedPrize.getDate(), savedPrize.getId(), new ArrayList<>());
    }

    @Override
    @Transactional
    public PrizeResponseDTO updatePrize(Integer prizeId, PrizeDTO dto) {
        var prizeToUpdate = findOne(prizeId);

        setCommonFields(prizeToUpdate, dto);
        var savedPrize = prizeRepository.save(prizeToUpdate);

        indexPrize(savedPrize,
            prizeIndexRepository.findPrizeIndexByDatabaseId(prizeId).orElse(new PrizeIndex()));

        return new PrizeResponseDTO(
            MultilingualContentConverter.getMultilingualContentDTO(prizeToUpdate.getTitle()),
            MultilingualContentConverter.getMultilingualContentDTO(prizeToUpdate.getDescription()),
            prizeToUpdate.getDate(), prizeToUpdate.getId(), prizeToUpdate.getProofs().stream().map(
            DocumentFileConverter::toDTO).collect(Collectors.toList()));
    }

    @Override
    @Transactional
    public void deletePrize(Integer prizeId, Integer personId) {
        var person = personService.findOne(personId);

        person.setPrizes(
            person.getPrizes().stream().filter(prize -> !Objects.equals(prize.getId(), prizeId))
                .collect(
                    Collectors.toSet()));

        delete(prizeId);
    }

    @Override
    @Transactional
    public DocumentFileResponseDTO addProof(Integer prizeId, DocumentFileDTO proof) {
        var prize = findOne(prizeId);
        var documentFile =
            documentFileService.saveNewPersonalDocument(proof, false, prize.getPerson());
        prize.getProofs().add(documentFile);
        save(prize);

        return DocumentFileConverter.toDTO(documentFile);
    }

    @Override
    @Transactional
    public DocumentFileResponseDTO updateProof(DocumentFileDTO updatedProof) {
        return documentFileService.editDocumentFile(updatedProof, false);
    }

    @Override
    @Transactional
    public void deleteProof(Integer proofId, Integer prizeId) {
        var prize = findOne(prizeId);
        var documentFile = documentFileService.findDocumentFileById(proofId);

        prize.setProofs(prize.getProofs().stream()
            .filter(proof -> !Objects.equals(proof.getId(), proofId)).collect(
                Collectors.toSet()));
        save(prize);

        documentFileService.deleteDocumentFile(documentFile.getServerFilename());
    }

    @Override
    @Async("reindexExecutor")
    @Transactional(readOnly = true)
    public CompletableFuture<Void> reindexPrizes() {
        prizeIndexRepository.deleteAll();

        FunctionalUtil.performBulkReindex(
            this::findAll,
            Sort.by(Sort.Direction.ASC, "id"),
            (prize) -> indexPrize(prize, new PrizeIndex())
        );

        return null;
    }

    @Override
    @Transactional(readOnly = true)
    public void reindexPrizeVolatileInformation(Prize prize, @Nullable PrizeIndex prizeIndex,
                                                boolean indexRelations, boolean indexAssessments) {
        if (!indexAssessments && !indexRelations) {
            return;
        }

        boolean shouldSave;

        if (Objects.isNull(prizeIndex)) {
            var prizeIndexOpt = prizeIndexRepository.findPrizeIndexByDatabaseId(prize.getId());
            if (prizeIndexOpt.isEmpty()) {
                return;
            }

            prizeIndex = prizeIndexOpt.get();

            shouldSave = true;
        } else {
            shouldSave = false;
        }

        PrizeIndex finalPrizeIndex = prizeIndex;

        if (indexRelations) {
            personIndexRepository.findByDatabaseId(prize.getPerson().getId())
                .ifPresent(personIndex ->
                    finalPrizeIndex.setRelatedInstitutionsIdHierarchy(
                        personIndex.getEmploymentInstitutionsIdHierarchy()));
        }

        if (indexAssessments) {
            finalPrizeIndex.setAssessedBy(
                commissionRepository.findCommissionsThatAssessedPrize(prize.getId()));
            commissionRepository.findAssessmentClassificationBasicInfoForPrizeAndCommissions(
                prize.getId(), finalPrizeIndex.getAssessedBy()).forEach(assessment ->
                finalPrizeIndex.getCommissionAssessments().add(
                    new Triple<>(assessment.commissionId(),
                        assessment.assessmentCode(),
                        assessment.manual())));
        }

        if (shouldSave) {
            prizeIndexRepository.save(finalPrizeIndex);
        }
    }

    @Override
    public Page<PrizeIndex> searchPrizes(List<String> tokens, Pageable pageable, Integer personId,
                                         Integer institutionId, Integer commissionId) {
        return searchService.runQuery(
            buildSimpleSearchQuery(tokens, institutionId, commissionId),
            pageable,
            PrizeIndex.class, "prize");
    }

    private void setCommonFields(Prize prize, PrizeDTO dto) {
        prize.setTitle(multilingualContentService.getMultilingualContent(dto.getTitle()));
        prize.setDescription(
            multilingualContentService.getMultilingualContent(dto.getDescription()));
        prize.setDate(dto.getDate());
    }

    private void indexPrize(Prize prize, PrizeIndex index) {
        index.setDatabaseId(prize.getId());

        indexMultilingualContent(index, prize, Prize::getTitle, PrizeIndex::setTitleSr,
            PrizeIndex::setTitleOther);
        index.setTitleSrSortable(index.getTitleSr());
        index.setTitleOtherSortable(index.getTitleOther());

        indexMultilingualContent(index, prize, Prize::getDescription, PrizeIndex::setDescriptionSr,
            PrizeIndex::setDescriptionOther);

        index.setDateOfAcquisition(prize.getDate());
        index.setPersonId(prize.getPerson().getId());

        reindexPrizeVolatileInformation(prize, index, true, true);

        prizeIndexRepository.save(index);
    }

    private void indexMultilingualContent(PrizeIndex index,
                                          Prize prize,
                                          Function<Prize, Set<MultiLingualContent>> contentExtractor,
                                          BiConsumer<PrizeIndex, String> srSetter,
                                          BiConsumer<PrizeIndex, String> otherSetter) {
        Set<MultiLingualContent> contentList = contentExtractor.apply(prize);

        var srContent = new StringBuilder();
        var otherContent = new StringBuilder();

        multilingualContentService.buildLanguageStrings(srContent, otherContent, contentList, true);

        StringUtil.removeTrailingDelimiters(srContent, otherContent);
        srSetter.accept(index,
            !srContent.isEmpty() ? srContent.toString() : otherContent.toString());
        otherSetter.accept(index,
            !otherContent.isEmpty() ? otherContent.toString() : srContent.toString());
    }

    private Query buildSimpleSearchQuery(List<String> tokens,
                                         Integer institutionId,
                                         Integer commissionId) {
        String minShouldMatch;
        if (tokens.size() <= 2) {
            minShouldMatch = "1"; // Allow partial match for very short queries
        } else {
            minShouldMatch = String.valueOf(Math.min((int) Math.ceil(tokens.size() * 0.7), 10));
        }

        return BoolQuery.of(q -> q.must(mb -> mb.bool(b -> {
            b.must(buildSimpleMetadataQuery(institutionId, commissionId));
            b.must(buildSimpleTokenQuery(tokens, minShouldMatch));
            return b;
        })))._toQuery();
    }

    private Query buildSimpleMetadataQuery(Integer institutionId,
                                           Integer commissionId) {
        return BoolQuery.of(b -> {
            if (Objects.nonNull(institutionId) && institutionId > 0) {
                var allSubInstitutions =
                    organisationUnitService.getOrganisationUnitIdsFromSubHierarchy(institutionId);

                b.must(q -> q.terms(t -> t.field("organisation_unit_ids").terms(
                    terms -> terms.value(
                        allSubInstitutions.stream().map(id -> FieldValue.of(id.toString()))
                            .collect(Collectors.toList())))));
            }

            if (Objects.nonNull(commissionId) && commissionId > 0) {
                b.mustNot(q -> q.term(t -> t.field("assessed_by").value(commissionId)));
            }

            return b;
        })._toQuery();
    }

    private Query buildSimpleTokenQuery(List<String> tokens, String minShouldMatch) {
        return BoolQuery.of(eq -> {
            tokens.forEach(token -> {
                if (token.startsWith("\\\"") && token.endsWith("\\\"")) {
                    eq.must(mp -> mp.bool(m -> m
                        .should(sb -> sb.matchPhrase(
                            mq -> mq.field("title_sr").query(token.replace("\\\"", ""))))
                        .should(sb -> sb.matchPhrase(
                            mq -> mq.field("title_other").query(token.replace("\\\"", ""))))
                    ));
                } else if (token.endsWith("\\*") || token.endsWith(".")) {
                    var wildcard = token.replace("\\*", "").replace(".", "");
                    eq.should(mp -> mp.bool(m -> m
                        .should(sb -> sb.wildcard(
                            mq -> mq.field("title_sr")
                                .value(StringUtil.performSimpleLatinPreprocessing(wildcard) + "*")
                                .caseInsensitive(true)))
                        .should(sb -> sb.wildcard(
                            mq -> mq.field("title_other").value(wildcard + "*")
                                .caseInsensitive(true)))
                    ));
                } else {
                    var wildcard = token + "*";
                    eq.should(mp -> mp.bool(m -> m
                        .should(sb -> sb.wildcard(
                            mq -> mq.field("title_sr")
                                .value(StringUtil.performSimpleLatinPreprocessing(token) + "*")
                                .caseInsensitive(true)))
                        .should(sb -> sb.wildcard(
                            mq -> mq.field("title_other").value(wildcard)
                                .caseInsensitive(true)))
                        .should(sb -> sb.match(
                            mq -> mq.field("title_sr").query(wildcard)))
                        .should(sb -> sb.match(
                            mq -> mq.field("title_other").query(wildcard)))
                    ));
                }

                eq
                    .should(sb -> sb.match(m ->
                        m.field("description_sr").query(token).boost(0.7f)))
                    .should(
                        sb -> sb.match(m ->
                            m.field("description_other").query(token).boost(0.7f)));
            });

            return eq.minimumShouldMatch(minShouldMatch);
        })._toQuery();
    }
}
