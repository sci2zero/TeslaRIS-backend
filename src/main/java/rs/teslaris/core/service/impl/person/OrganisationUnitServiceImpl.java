package rs.teslaris.core.service.impl.person;

import co.elastic.clients.elasticsearch._types.query_dsl.BoolQuery;
import co.elastic.clients.elasticsearch._types.query_dsl.Query;
import java.util.HashSet;
import java.util.List;
import java.util.Objects;
import java.util.Set;
import java.util.function.BiConsumer;
import java.util.function.Function;
import javax.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Service;
import rs.teslaris.core.converter.commontypes.GeoLocationConverter;
import rs.teslaris.core.converter.institution.OrganisationUnitConverter;
import rs.teslaris.core.converter.institution.RelationConverter;
import rs.teslaris.core.converter.person.ContactConverter;
import rs.teslaris.core.dto.document.DocumentFileDTO;
import rs.teslaris.core.dto.institution.OrganisationUnitDTO;
import rs.teslaris.core.dto.institution.OrganisationUnitDTORequest;
import rs.teslaris.core.dto.institution.OrganisationUnitsRelationDTO;
import rs.teslaris.core.dto.institution.OrganisationUnitsRelationResponseDTO;
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
        return BoolQuery.of(q -> q.must(mb -> mb.bool(b -> {
            tokens.forEach(token -> {
                b.should(sb -> sb.wildcard(
                    m -> m.field("name_sr").value("*" + token + "*").caseInsensitive(true)));
                b.should(sb -> sb.match(
                    m -> m.field("name_sr").query(token)));
                b.should(sb -> sb.wildcard(
                    m -> m.field("name_other").value("*" + token + "*").caseInsensitive(true)));
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
            return b;
        })))._toQuery();
    }

    @Override
    public OrganisationUnitsRelation findOrganisationUnitsRelationById(Integer id) {
        return organisationUnitsRelationJPAService.findOne(id);
    }

    @Override
    public Page<OrganisationUnitsRelationResponseDTO> getOrganisationUnitsRelations(
        Integer sourceId, Integer targetId, Pageable pageable) {
        return organisationUnitsRelationRepository.getRelationsForOrganisationUnits(pageable,
            sourceId, targetId).map(RelationConverter::toResponseDTO);
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
        OrganisationUnitDTORequest organisationUnitDTORequest) {
        OrganisationUnit organisationUnit = new OrganisationUnit();
        OrganisationUnitIndex organisationUnitIndex = new OrganisationUnitIndex();

        setCommonOUFields(organisationUnit, organisationUnitDTORequest);
        indexCommonFields(organisationUnit, organisationUnitIndex);

        if (organisationUnitApprovedByDefault) {
            organisationUnit.setApproveStatus(ApproveStatus.APPROVED);
        } else {
            organisationUnit.setApproveStatus(ApproveStatus.REQUESTED);
        }

        var savedOU = this.save(organisationUnit);
        organisationUnitIndex.setDatabaseId(savedOU.getId());
        organisationUnitIndexRepository.save(organisationUnitIndex);

        return OrganisationUnitConverter.toDTO(organisationUnit);
    }

    @Override
    public OrganisationUnit editOrganisationUnit(
        OrganisationUnitDTORequest organisationUnitDTORequest, Integer organisationUnitId) {

        var organisationUnitToUpdate = getReferenceToOrganisationUnitById(organisationUnitId);
        var indexToUpdate = organisationUnitIndexRepository.findOrganisationUnitIndexByDatabaseId(
                organisationUnitId)
            .orElse(new OrganisationUnitIndex());
        indexToUpdate.setDatabaseId(organisationUnitId);

        organisationUnitToUpdate.getName().clear();
        organisationUnitToUpdate.getKeyword().clear();
        organisationUnitToUpdate.getResearchAreas().clear();

        setCommonOUFields(organisationUnitToUpdate, organisationUnitDTORequest);

        organisationUnitToUpdate = this.save(organisationUnitToUpdate);
        return organisationUnitToUpdate;
    }

    private void setCommonOUFields(OrganisationUnit organisationUnit,
                                   OrganisationUnitDTORequest organisationUnitDTO) {
        organisationUnit.setName(
            multilingualContentService.getMultilingualContent(organisationUnitDTO.getName())
        );
        organisationUnit.setNameAbbreviation(organisationUnitDTO.getNameAbbreviation());
        organisationUnit.setKeyword(
            multilingualContentService.getMultilingualContent(
                organisationUnitDTO.getKeyword())
        );

        var researchAreas = researchAreaService.getResearchAreasByIds(
            organisationUnitDTO.getResearchAreasId());
        organisationUnit.setResearchAreas(new HashSet<>(researchAreas));

        organisationUnit.setLocation(
            GeoLocationConverter.fromDTO(organisationUnitDTO.getLocation()));


        organisationUnit.setContact(
            ContactConverter.fromDTO(organisationUnitDTO.getContact()));
    }

    private void indexCommonFields(OrganisationUnit organisationUnit, OrganisationUnitIndex index) {
        indexMultilingualContent(index, organisationUnit, OrganisationUnit::getName,
            OrganisationUnitIndex::setNameSr,
            OrganisationUnitIndex::setNameOther);
        index.setNameSr(index.getNameSr() + " " + organisationUnit.getNameAbbreviation());
        index.setNameSrSortable(index.getNameSr());
        index.setNameOtherSortable(index.getNameOther());

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

        var newRelation = new OrganisationUnitsRelation();
        setCommonOURelationFields(newRelation, relationDTO);

        if (relationApprovedByDefault) {
            newRelation.setApproveStatus(ApproveStatus.APPROVED);
        } else {
            newRelation.setApproveStatus(ApproveStatus.REQUESTED);
        }

        return organisationUnitsRelationJPAService.save(newRelation);
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
            var documentFile = documentFileService.saveNewDocument(proof, true);
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
            organisationUnitsRelationRepository.findBySourceOrganisationUnitAndRelationType(
                sourceOrganisationUnitId, OrganisationUnitRelationType.BELONGS_TO);

        while (!relationsToCheck.isEmpty()) {
            OrganisationUnitsRelation newTargetOrganisationUnit = relationsToCheck.remove(0);

            if (newTargetOrganisationUnit.getId().equals(targetOrganisationUnit)) {
                return true;
            }

            List<OrganisationUnitsRelation> newRelationToCheck =
                organisationUnitsRelationRepository.findBySourceOrganisationUnitAndRelationType(
                    newTargetOrganisationUnit.getId(), OrganisationUnitRelationType.BELONGS_TO);
            relationsToCheck.addAll(newRelationToCheck);

        }
        return false;

    }
}
