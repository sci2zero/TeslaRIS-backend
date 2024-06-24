package rs.teslaris.core.service.impl.person;

import co.elastic.clients.elasticsearch._types.query_dsl.BoolQuery;
import co.elastic.clients.elasticsearch._types.query_dsl.Query;
import jakarta.annotation.Nullable;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Objects;
import java.util.Set;
import java.util.function.BiConsumer;
import java.util.function.Function;
import java.util.stream.Collectors;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import rs.teslaris.core.converter.commontypes.GeoLocationConverter;
import rs.teslaris.core.converter.institution.OrganisationUnitConverter;
import rs.teslaris.core.converter.institution.RelationConverter;
import rs.teslaris.core.converter.person.ContactConverter;
import rs.teslaris.core.dto.document.DocumentFileDTO;
import rs.teslaris.core.dto.institution.OrganisationUnitDTO;
import rs.teslaris.core.dto.institution.OrganisationUnitGraphRelationDTO;
import rs.teslaris.core.dto.institution.OrganisationUnitRequestDTO;
import rs.teslaris.core.dto.institution.OrganisationUnitsRelationDTO;
import rs.teslaris.core.dto.institution.OrganisationUnitsRelationResponseDTO;
import rs.teslaris.core.dto.institution.RelationGraphDataDTO;
import rs.teslaris.core.indexmodel.OrganisationUnitIndex;
import rs.teslaris.core.indexrepository.OrganisationUnitIndexRepository;
import rs.teslaris.core.model.commontypes.ApproveStatus;
import rs.teslaris.core.model.commontypes.MultiLingualContent;
import rs.teslaris.core.model.institution.OrganisationUnit;
import rs.teslaris.core.model.institution.OrganisationUnitRelationType;
import rs.teslaris.core.model.institution.OrganisationUnitsRelation;
import rs.teslaris.core.repository.person.OrganisationUnitRepository;
import rs.teslaris.core.repository.person.OrganisationUnitsRelationRepository;
import rs.teslaris.core.service.impl.JPAServiceImpl;
import rs.teslaris.core.service.impl.person.cruddelegate.OrganisationUnitsRelationJPAServiceImpl;
import rs.teslaris.core.service.interfaces.commontypes.MultilingualContentService;
import rs.teslaris.core.service.interfaces.commontypes.ResearchAreaService;
import rs.teslaris.core.service.interfaces.commontypes.SearchService;
import rs.teslaris.core.service.interfaces.document.DocumentFileService;
import rs.teslaris.core.service.interfaces.person.OrganisationUnitService;
import rs.teslaris.core.util.exceptionhandling.exception.NotFoundException;
import rs.teslaris.core.util.exceptionhandling.exception.OrganisationUnitReferenceConstraintViolation;
import rs.teslaris.core.util.exceptionhandling.exception.SelfRelationException;
import rs.teslaris.core.util.search.ExpressionTransformer;
import rs.teslaris.core.util.search.SearchRequestType;
import rs.teslaris.core.util.search.StringUtil;

@Service
@RequiredArgsConstructor
@Transactional
public class OrganisationUnitServiceImpl extends JPAServiceImpl<OrganisationUnit>
    implements OrganisationUnitService {

    private final OrganisationUnitsRelationJPAServiceImpl organisationUnitsRelationJPAService;

    private final OrganisationUnitRepository organisationUnitRepository;

    private final OrganisationUnitIndexRepository organisationUnitIndexRepository;

    private final MultilingualContentService multilingualContentService;

    private final OrganisationUnitsRelationRepository organisationUnitsRelationRepository;

    private final ResearchAreaService researchAreaService;

    private final DocumentFileService documentFileService;

    private final SearchService<OrganisationUnitIndex> searchService;

    private final ExpressionTransformer expressionTransformer;

    @Value("${relation.approved_by_default}")
    private Boolean relationApprovedByDefault;

    @Value("${organisation_unit.approved_by_default}")
    private Boolean organisationUnitApprovedByDefault;


    @Override
    protected JpaRepository<OrganisationUnit, Integer> getEntityRepository() {
        return organisationUnitRepository;
    }

    @Override
    public OrganisationUnit findOrganisationUnitById(Integer id) {
        return findOne(id);
    }

    @Override
    @Nullable
    public OrganisationUnitIndex findOrganisationUnitByScopusAfid(String scopusAfid) {
        return organisationUnitIndexRepository.findOrganisationUnitIndexByScopusAfid(scopusAfid)
            .orElse(null);
    }

    @Override
    @Nullable
    public OrganisationUnit findOrganisationUnitByOldId(Integer oldId) {
        return organisationUnitRepository.findOrganisationUnitByOldId(oldId).orElse(null);
    }

    @Override
    public OrganisationUnit findOne(Integer id) {
        return organisationUnitRepository.findByIdWithLangDataAndResearchArea(id)
            .orElseThrow(
                () -> new NotFoundException("Organisation unit with given ID does not exist."));
    }

    @Override
    @Transactional
    public Page<OrganisationUnitDTO> findOrganisationUnits(Pageable pageable) {
        return organisationUnitRepository.findAllWithLangData(pageable)
            .map(OrganisationUnitConverter::toDTO);
    }

    @Override
    public Page<OrganisationUnitIndex> searchOrganisationUnits(List<String> tokens,
                                                               Pageable pageable,
                                                               SearchRequestType type) {
        if (type.equals(SearchRequestType.SIMPLE)) {
            return searchService.runQuery(buildSimpleSearchQuery(tokens),
                pageable,
                OrganisationUnitIndex.class, "organisation-unit");
        }

        return searchService.runQuery(
            expressionTransformer.parseAdvancedQuery(tokens), pageable,
            OrganisationUnitIndex.class, "organisation-unit");
    }

    private Query buildSimpleSearchQuery(List<String> tokens) {
        var minShouldMatch = (int) Math.ceil(tokens.size() * 0.8);

        return BoolQuery.of(q -> q.must(mb -> mb.bool(b -> {
            tokens.forEach(token -> {
                b.should(sb -> sb.wildcard(
                    m -> m.field("name_sr").value("*" + token + "*").caseInsensitive(true)));
                b.should(sb -> sb.match(
                    m -> m.field("name_sr").query(token)));
                b.should(sb -> sb.wildcard(
                    m -> m.field("name_other").value("*" + token + "*").caseInsensitive(true)));
                b.should(sb -> sb.match(
                    m -> m.field("super_ou_name_sr").query(token)));
                b.should(sb -> sb.match(
                    m -> m.field("super_ou_name_other").query(token)));
                b.should(sb -> sb.wildcard(
                    m -> m.field("keywords_sr").value("*" + token + "*").caseInsensitive(true)));
                b.should(sb -> sb.wildcard(
                    m -> m.field("keywords_other").value("*" + token + "*").caseInsensitive(true)));
                b.should(sb -> sb.match(
                    m -> m.field("research_areas_sr").query(token)));
                b.should(sb -> sb.match(
                    m -> m.field("research_areas_other").query(token)));
                b.should(sb -> sb.match(
                    m -> m.field("orcid").query(token)));
            });
            return b.minimumShouldMatch(Integer.toString(minShouldMatch));
        })))._toQuery();
    }

    @Override
    public OrganisationUnitsRelation findOrganisationUnitsRelationById(Integer id) {
        return organisationUnitsRelationJPAService.findOne(id);
    }

    @Override
    public List<OrganisationUnitsRelationResponseDTO> getOrganisationUnitsRelations(
        Integer sourceId) {
        return organisationUnitsRelationRepository.getRelationsForOrganisationUnits(sourceId)
            .stream()
            .map(RelationConverter::toResponseDTO).collect(Collectors.toList());
    }

    @Override
    public RelationGraphDataDTO getOrganisationUnitsRelationsChain(
        Integer leafId) {
        var nodes = new ArrayList<OrganisationUnitDTO>();
        var links = new ArrayList<OrganisationUnitGraphRelationDTO>();
        nodes.add(OrganisationUnitConverter.toDTO(findOne(leafId)));

        var currentBelongsToRelation = organisationUnitsRelationRepository.getSuperOU(leafId);

        if (currentBelongsToRelation.isEmpty()) {
            addMembershipOrganisations(nodes, links, leafId);
            return new RelationGraphDataDTO(nodes, links);
        }

        while (currentBelongsToRelation.isPresent()) {
            links.add(new OrganisationUnitGraphRelationDTO(
                currentBelongsToRelation.get().getSourceOrganisationUnit().getId(),
                currentBelongsToRelation.get().getTargetOrganisationUnit().getId(),
                OrganisationUnitRelationType.BELONGS_TO));

            addMembershipOrganisations(nodes, links,
                currentBelongsToRelation.get().getSourceOrganisationUnit().getId());

            nodes.add(OrganisationUnitConverter.toDTO(
                currentBelongsToRelation.get().getTargetOrganisationUnit()));
            currentBelongsToRelation = organisationUnitsRelationRepository.getSuperOU(
                currentBelongsToRelation.get().getTargetOrganisationUnit().getId());
        }

        return new RelationGraphDataDTO(nodes, links);
    }

    private void addMembershipOrganisations(ArrayList<OrganisationUnitDTO> nodes,
                                            ArrayList<OrganisationUnitGraphRelationDTO> links,
                                            Integer currentNodeId) {
        organisationUnitsRelationRepository.getSuperOUsMemberOf(currentNodeId)
            .forEach((relation -> {
                links.add(new OrganisationUnitGraphRelationDTO(
                    relation.getSourceOrganisationUnit().getId(),
                    relation.getTargetOrganisationUnit().getId(),
                    OrganisationUnitRelationType.MEMBER_OF));
                nodes.add(
                    OrganisationUnitConverter.toDTO(relation.getTargetOrganisationUnit()));
            }));
    }

    @Override
    public List<Integer> getOrganisationUnitIdsFromSubHierarchy(Integer currentOUNodeId) {
        var nodeIds = new ArrayList<Integer>();
        nodeIds.add(currentOUNodeId);

        var subUnits = organisationUnitsRelationRepository.getOuSubUnits(currentOUNodeId);

        for (var subUnit : subUnits) {
            var childrenSubUnits =
                getOrganisationUnitIdsFromSubHierarchy(subUnit.getSourceOrganisationUnit().getId());
            nodeIds.addAll(childrenSubUnits);
        }

        return nodeIds;
    }

    @Override
    public OrganisationUnit getReferenceToOrganisationUnitById(Integer id) {
        return id == null ? null : organisationUnitRepository.getReferenceById(id);
    }

    @Override
    public Long getOrganisationUnitsCount() {
        return organisationUnitIndexRepository.count();
    }

    @Override
    public OrganisationUnitDTO createOrganisationUnit(
        OrganisationUnitRequestDTO organisationUnitRequestDTO, Boolean index) {

        OrganisationUnit organisationUnit = new OrganisationUnit();

        setCommonOUFields(organisationUnit, organisationUnitRequestDTO);

        if (organisationUnitApprovedByDefault) {
            organisationUnit.setApproveStatus(ApproveStatus.APPROVED);
        } else {
            organisationUnit.setApproveStatus(ApproveStatus.REQUESTED);
        }

        var savedOU = this.save(organisationUnit);

        if (organisationUnit.getApproveStatus().equals(ApproveStatus.APPROVED) && index) {
            indexOrganisationUnit(organisationUnit, new OrganisationUnitIndex());
        }

        return OrganisationUnitConverter.toDTO(savedOU);
    }

    @Override
    public OrganisationUnit editOrganisationUnit(
        OrganisationUnitRequestDTO organisationUnitDTORequest, Integer organisationUnitId) {

        var organisationUnitToUpdate = getReferenceToOrganisationUnitById(organisationUnitId);

        organisationUnitToUpdate.getName().clear();
        organisationUnitToUpdate.getKeyword().clear();
        organisationUnitToUpdate.getResearchAreas().clear();

        setCommonOUFields(organisationUnitToUpdate, organisationUnitDTORequest);

        organisationUnitToUpdate = save(organisationUnitToUpdate);

        if (organisationUnitToUpdate.getApproveStatus().equals(ApproveStatus.APPROVED)) {
            var index = organisationUnitIndexRepository.findOrganisationUnitIndexByDatabaseId(
                organisationUnitId).orElse(new OrganisationUnitIndex());
            indexOrganisationUnit(organisationUnitToUpdate, index);
        }

        return organisationUnitToUpdate;
    }

    private void setCommonOUFields(OrganisationUnit organisationUnit,
                                   OrganisationUnitRequestDTO organisationUnitDTO) {
        organisationUnit.setName(
            multilingualContentService.getMultilingualContent(organisationUnitDTO.getName())
        );
        organisationUnit.setNameAbbreviation(organisationUnitDTO.getNameAbbreviation());
        organisationUnit.setKeyword(
            multilingualContentService.getMultilingualContent(
                organisationUnitDTO.getKeyword())
        );

        organisationUnit.setScopusAfid(organisationUnitDTO.getScopusAfid());
        organisationUnit.setOldId(organisationUnitDTO.getOldId());

        var researchAreas = researchAreaService.getResearchAreasByIds(
            organisationUnitDTO.getResearchAreasId());
        organisationUnit.setResearchAreas(new HashSet<>(researchAreas));

        organisationUnit.setLocation(
            GeoLocationConverter.fromDTO(organisationUnitDTO.getLocation()));


        organisationUnit.setContact(
            ContactConverter.fromDTO(organisationUnitDTO.getContact()));
    }

    private void indexOrganisationUnit(OrganisationUnit organisationUnit,
                                       OrganisationUnitIndex index) {
        index.setDatabaseId(organisationUnit.getId());

        indexCommonFields(organisationUnit, index);
        organisationUnitIndexRepository.save(index);
    }

    private void indexCommonFields(OrganisationUnit organisationUnit, OrganisationUnitIndex index) {
        indexMultilingualContent(index, organisationUnit, OrganisationUnit::getName,
            OrganisationUnitIndex::setNameSr,
            OrganisationUnitIndex::setNameOther);
        index.setNameSr(index.getNameSr() + " " + organisationUnit.getNameAbbreviation());
        index.setNameSrSortable(index.getNameSr());
        index.setNameOtherSortable(index.getNameOther());

        index.setScopusAfid(organisationUnit.getScopusAfid());

        indexMultilingualContent(index, organisationUnit, OrganisationUnit::getKeyword,
            OrganisationUnitIndex::setKeywordsSr,
            OrganisationUnitIndex::setKeywordsOther);

        var researchAreaSrContent = new StringBuilder();
        var researchAreaOtherContent = new StringBuilder();

        organisationUnit.getResearchAreas().forEach(
            researchArea -> multilingualContentService.buildLanguageStrings(researchAreaSrContent,
                researchAreaOtherContent,
                researchArea.getName()));

        StringUtil.removeTrailingPipeDelimiter(researchAreaSrContent, researchAreaOtherContent);
        index.setResearchAreasSr(
            researchAreaSrContent.length() > 0 ? researchAreaSrContent.toString() :
                researchAreaOtherContent.toString());
        index.setResearchAreasOther(
            researchAreaOtherContent.length() > 0 ? researchAreaOtherContent.toString() :
                researchAreaSrContent.toString());

        indexBelongsToSuperOURelation(organisationUnit, index);
    }

    private void indexBelongsToSuperOURelation(OrganisationUnit organisationUnit,
                                               OrganisationUnitIndex index) {
        var belongsToRelation =
            organisationUnitsRelationRepository.getSuperOU(organisationUnit.getId());
        belongsToRelation.ifPresent(organisationUnitsRelation -> indexMultilingualContent(index,
            organisationUnitsRelation.getTargetOrganisationUnit(),
            OrganisationUnit::getName,
            OrganisationUnitIndex::setSuperOUNameSr,
            OrganisationUnitIndex::setSuperOUNameOther));
    }

    private void indexMultilingualContent(OrganisationUnitIndex index,
                                          OrganisationUnit organisationUnit,
                                          Function<OrganisationUnit, Set<MultiLingualContent>> contentExtractor,
                                          BiConsumer<OrganisationUnitIndex, String> srSetter,
                                          BiConsumer<OrganisationUnitIndex, String> otherSetter) {
        Set<MultiLingualContent> contentList = contentExtractor.apply(organisationUnit);

        var srContent = new StringBuilder();
        var otherContent = new StringBuilder();

        multilingualContentService.buildLanguageStrings(srContent, otherContent, contentList);

        StringUtil.removeTrailingPipeDelimiter(srContent, otherContent);
        srSetter.accept(index,
            srContent.length() > 0 ? srContent.toString() : otherContent.toString());
        otherSetter.accept(index,
            otherContent.length() > 0 ? otherContent.toString() : srContent.toString());
    }

    @Override
    @Transactional
    public OrganisationUnit editOrganisationalUnitApproveStatus(ApproveStatus approveStatus,
                                                                Integer organisationUnitId) {
        OrganisationUnit organisationUnit =
            organisationUnitRepository.findByIdWithLangDataAndResearchArea(
                    organisationUnitId)
                .orElseThrow(
                    () -> new NotFoundException(
                        "Organisation units relation with given ID does not exist."));

        if (organisationUnit.getApproveStatus().equals(ApproveStatus.REQUESTED)) {
            organisationUnit.setApproveStatus(approveStatus);
        }

        this.save(organisationUnit);
        return organisationUnit;
    }

    @Override
    public void deleteOrganisationUnit(Integer organisationUnitId) {
        if (organisationUnitRepository.hasEmployees(organisationUnitId) ||
            organisationUnitRepository.hasThesis(organisationUnitId) ||
            organisationUnitRepository.hasRelation(organisationUnitId) ||
            organisationUnitRepository.hasInvolvement(organisationUnitId)) {
            throw new OrganisationUnitReferenceConstraintViolation(
                "Organisation unit is already in use.");
        }

        this.delete(organisationUnitId);
        var index = organisationUnitIndexRepository.findOrganisationUnitIndexByDatabaseId(
            organisationUnitId);
        index.ifPresent(organisationUnitIndexRepository::delete);
    }

    @Override
    public OrganisationUnitsRelation createOrganisationUnitsRelation(
        OrganisationUnitsRelationDTO relationDTO) {
        if (Objects.equals(relationDTO.getSourceOrganisationUnitId(),
            relationDTO.getTargetOrganisationUnitId())) {
            throw new SelfRelationException("Organisation unit cannot relate to itself.");
        }

        if (relationDTO.getRelationType().equals(OrganisationUnitRelationType.BELONGS_TO) &&
            organisationUnitsRelationRepository.getSuperOU(
                relationDTO.getSourceOrganisationUnitId()).isPresent()) {
            return new OrganisationUnitsRelation();
        }

        var newRelation = new OrganisationUnitsRelation();
        setCommonOURelationFields(newRelation, relationDTO);

        if (relationApprovedByDefault) {
            newRelation.setApproveStatus(ApproveStatus.APPROVED);
        } else {
            newRelation.setApproveStatus(ApproveStatus.REQUESTED);
        }

        var savedRelation = organisationUnitsRelationJPAService.save(newRelation);

        var index = organisationUnitIndexRepository.findOrganisationUnitIndexByDatabaseId(
            savedRelation.getSourceOrganisationUnit().getId());
        index.ifPresent(organisationUnitIndex -> {
            indexBelongsToSuperOURelation(savedRelation.getSourceOrganisationUnit(),
                organisationUnitIndex);
            organisationUnitIndexRepository.save(organisationUnitIndex);
        });

        return savedRelation;
    }

    @Override
    public void editOrganisationUnitsRelation(OrganisationUnitsRelationDTO relationDTO,
                                              Integer id) {
        var relationToUpdate = findOrganisationUnitsRelationById(id);

        relationToUpdate.getSourceAffiliationStatement().clear();
        relationToUpdate.getTargetAffiliationStatement().clear();

        setCommonOURelationFields(relationToUpdate, relationDTO);

        organisationUnitsRelationJPAService.save(relationToUpdate);
    }

    @Override
    public void deleteOrganisationUnitsRelation(Integer id) {
        organisationUnitsRelationJPAService.delete(id);
    }

    @Override
    public void approveRelation(Integer relationId, Boolean approve) {
        var relationToApprove = findOrganisationUnitsRelationById(relationId);
        if (relationToApprove.getApproveStatus().equals(ApproveStatus.REQUESTED)) {
            relationToApprove.setApproveStatus(
                approve ? ApproveStatus.APPROVED : ApproveStatus.DECLINED);
        }
        organisationUnitsRelationJPAService.save(relationToApprove);
    }

    private void setCommonOURelationFields(OrganisationUnitsRelation relation,
                                           OrganisationUnitsRelationDTO relationDTO) {
        relation.setSourceAffiliationStatement(multilingualContentService.getMultilingualContent(
            relationDTO.getSourceAffiliationStatement()));
        relation.setTargetAffiliationStatement(multilingualContentService.getMultilingualContent(
            relationDTO.getTargetAffiliationStatement()));

        relation.setRelationType(relationDTO.getRelationType());
        relation.setDateFrom(relationDTO.getDateFrom());
        relation.setDateTo(relationDTO.getDateTo());

        relation.setSourceOrganisationUnit(
            findOrganisationUnitById(relationDTO.getSourceOrganisationUnitId()));
        relation.setTargetOrganisationUnit(
            findOrganisationUnitById(relationDTO.getTargetOrganisationUnitId()));
    }

    @Override
    public void addRelationProofs(List<DocumentFileDTO> proofs, Integer relationId) {
        var relation = findOrganisationUnitsRelationById(relationId);
        proofs.forEach(proof -> {
            var documentFile = documentFileService.saveNewDocument(proof, false);
            relation.getProofs().add(documentFile);
            organisationUnitsRelationJPAService.save(relation);
        });
    }

    @Override
    public void deleteRelationProof(Integer relationId, Integer proofId) {
        var documentFile = documentFileService.findDocumentFileById(proofId);
        documentFileService.delete(proofId);
        documentFileService.deleteDocumentFile(documentFile.getServerFilename());
    }

    @Override
    public boolean recursiveCheckIfOrganisationUnitBelongsTo(Integer sourceOrganisationUnitId,
                                                             Integer targetOrganisationUnit) {
        List<OrganisationUnitsRelation> relationsToCheck =
            organisationUnitsRelationRepository.findBySourceOrganisationUnitIdAndRelationType(
                sourceOrganisationUnitId, OrganisationUnitRelationType.BELONGS_TO);

        while (!relationsToCheck.isEmpty()) {
            OrganisationUnitsRelation newTargetOrganisationUnit = relationsToCheck.remove(0);

            if (newTargetOrganisationUnit.getId().equals(targetOrganisationUnit)) {
                return true;
            }

            List<OrganisationUnitsRelation> newRelationToCheck =
                organisationUnitsRelationRepository.findBySourceOrganisationUnitIdAndRelationType(
                    newTargetOrganisationUnit.getId(), OrganisationUnitRelationType.BELONGS_TO);
            relationsToCheck.addAll(newRelationToCheck);

        }
        return false;

    }

    @Override
    @Transactional(readOnly = true)
    public void reindexOrganisationUnits() {
        organisationUnitIndexRepository.deleteAll();
        int pageNumber = 0;
        int chunkSize = 10;
        boolean hasNextPage = true;

        while (hasNextPage) {

            List<OrganisationUnit> chunk =
                findAll(PageRequest.of(pageNumber, chunkSize)).getContent();

            chunk.forEach((organisationUnit) -> indexOrganisationUnit(organisationUnit,
                new OrganisationUnitIndex()));

            pageNumber++;
            hasNextPage = chunk.size() == chunkSize;
        }
    }
}
