package rs.teslaris.core.service.impl.institution;

import co.elastic.clients.elasticsearch._types.FieldValue;
import co.elastic.clients.elasticsearch._types.query_dsl.BoolQuery;
import co.elastic.clients.elasticsearch._types.query_dsl.MatchPhraseQuery;
import co.elastic.clients.elasticsearch._types.query_dsl.MatchQuery;
import co.elastic.clients.elasticsearch._types.query_dsl.Query;
import co.elastic.clients.elasticsearch._types.query_dsl.TermQuery;
import co.elastic.clients.elasticsearch._types.query_dsl.TermsQuery;
import co.elastic.clients.elasticsearch._types.query_dsl.WildcardQuery;
import jakarta.annotation.Nullable;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Objects;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.function.BiConsumer;
import java.util.function.Function;
import java.util.stream.Collectors;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import rs.teslaris.core.annotation.Traceable;
import rs.teslaris.core.applicationevent.OrganisationUnitDeletedEvent;
import rs.teslaris.core.applicationevent.OrganisationUnitSignificantChangeEvent;
import rs.teslaris.core.converter.commontypes.GeoLocationConverter;
import rs.teslaris.core.converter.institution.OrganisationUnitConverter;
import rs.teslaris.core.converter.institution.RelationConverter;
import rs.teslaris.core.converter.person.ContactConverter;
import rs.teslaris.core.dto.commontypes.MultilingualContentDTO;
import rs.teslaris.core.dto.commontypes.ProfilePhotoOrLogoDTO;
import rs.teslaris.core.dto.document.DocumentFileDTO;
import rs.teslaris.core.dto.institution.OrganisationUnitDTO;
import rs.teslaris.core.dto.institution.OrganisationUnitGraphRelationDTO;
import rs.teslaris.core.dto.institution.OrganisationUnitRequestDTO;
import rs.teslaris.core.dto.institution.OrganisationUnitsRelationDTO;
import rs.teslaris.core.dto.institution.OrganisationUnitsRelationResponseDTO;
import rs.teslaris.core.dto.institution.RelationGraphDataDTO;
import rs.teslaris.core.indexmodel.OrganisationUnitIndex;
import rs.teslaris.core.indexrepository.OrganisationUnitIndexRepository;
import rs.teslaris.core.indexrepository.UserAccountIndexRepository;
import rs.teslaris.core.model.commontypes.ApproveStatus;
import rs.teslaris.core.model.commontypes.MultiLingualContent;
import rs.teslaris.core.model.commontypes.ProfilePhotoOrLogo;
import rs.teslaris.core.model.document.Thesis;
import rs.teslaris.core.model.document.ThesisType;
import rs.teslaris.core.model.institution.OrganisationUnit;
import rs.teslaris.core.model.institution.OrganisationUnitRelationType;
import rs.teslaris.core.model.institution.OrganisationUnitsRelation;
import rs.teslaris.core.model.user.UserRole;
import rs.teslaris.core.repository.institution.OrganisationUnitRepository;
import rs.teslaris.core.repository.institution.OrganisationUnitsRelationRepository;
import rs.teslaris.core.repository.person.InvolvementRepository;
import rs.teslaris.core.service.impl.JPAServiceImpl;
import rs.teslaris.core.service.impl.person.cruddelegate.OrganisationUnitsRelationJPAServiceImpl;
import rs.teslaris.core.service.interfaces.commontypes.IndexBulkUpdateService;
import rs.teslaris.core.service.interfaces.commontypes.MultilingualContentService;
import rs.teslaris.core.service.interfaces.commontypes.ResearchAreaService;
import rs.teslaris.core.service.interfaces.commontypes.SearchService;
import rs.teslaris.core.service.interfaces.document.DocumentFileService;
import rs.teslaris.core.service.interfaces.document.FileService;
import rs.teslaris.core.service.interfaces.institution.OrganisationUnitService;
import rs.teslaris.core.util.exceptionhandling.exception.NotFoundException;
import rs.teslaris.core.util.exceptionhandling.exception.OrganisationUnitReferenceConstraintViolationException;
import rs.teslaris.core.util.exceptionhandling.exception.SelfRelationException;
import rs.teslaris.core.util.files.ImageUtil;
import rs.teslaris.core.util.functional.Triple;
import rs.teslaris.core.util.persistence.IdentifierUtil;
import rs.teslaris.core.util.search.ExpressionTransformer;
import rs.teslaris.core.util.search.SearchFieldsLoader;
import rs.teslaris.core.util.search.SearchRequestType;
import rs.teslaris.core.util.search.StringUtil;
import rs.teslaris.core.util.session.SessionUtil;

@Service
@RequiredArgsConstructor
@Transactional
@Traceable
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

    private final IndexBulkUpdateService indexBulkUpdateService;

    private final UserAccountIndexRepository userAccountIndexRepository;

    private final InvolvementRepository involvementRepository;

    private final SearchFieldsLoader searchFieldsLoader;

    private final FileService fileService;

    private final ApplicationEventPublisher applicationEventPublisher;

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
    public OrganisationUnitDTO readOrganisationUnitById(Integer id) {
        OrganisationUnit ou;
        try {
            ou = findOne(id);
        } catch (NotFoundException e) {
            organisationUnitIndexRepository.findOrganisationUnitIndexByDatabaseId(id)
                .ifPresent(organisationUnitIndexRepository::delete);
            throw e;
        }

        if (!ou.getApproveStatus().equals(ApproveStatus.APPROVED)) {
            throw new NotFoundException("OrganisationUnit with given ID does not exist.");
        }

        return OrganisationUnitConverter.toDTO(ou);
    }

    @Override
    @Nullable
    public OrganisationUnitIndex findOrganisationUnitByImportId(String importId) {
        if (Objects.isNull(importId) || importId.isBlank()) {
            return null;
        }

        return organisationUnitIndexRepository.findByScopusAfidOrOpenAlexId(importId)
            .orElse(null);
    }

    @Override
    @Nullable
    public OrganisationUnit findOrganisationUnitByOldId(Integer oldId) {
        return organisationUnitRepository.findOrganisationUnitByOldIdsContains(oldId).orElse(null);
    }

    @Override
    public OrganisationUnitDTO readOrganisationUnitForOldId(Integer oldId) {
        var ouToReturn = findOrganisationUnitByOldId(oldId);

        if (Objects.isNull(ouToReturn)) {
            throw new NotFoundException("Organisation unit with given 'OLD ID' does not exist.");
        }

        return OrganisationUnitConverter.toDTO(ouToReturn);
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
                                                               SearchRequestType type,
                                                               Integer personId,
                                                               Integer topLevelInstitutionId,
                                                               Boolean onlyReturnOnesWhichCanHarvest,
                                                               Boolean onlyIndependent,
                                                               ThesisType allowedThesisType,
                                                               Boolean onlyClientInstitutions) {
        if (type.equals(SearchRequestType.SIMPLE)) {
            return searchService.runQuery(
                buildSimpleSearchQuery(tokens, personId, topLevelInstitutionId,
                    onlyReturnOnesWhichCanHarvest, onlyIndependent, allowedThesisType,
                    onlyClientInstitutions),
                pageable,
                OrganisationUnitIndex.class, "organisation_unit");
        }

        return searchService.runQuery(
            expressionTransformer.parseAdvancedQuery(tokens), pageable,
            OrganisationUnitIndex.class, "organisation_unit");
    }

    private Query buildSimpleSearchQuery(List<String> tokens, Integer personId,
                                         Integer topLevelInstitutionId,
                                         Boolean onlyReturnOnesWhichCanHarvest,
                                         Boolean onlyIndependent, ThesisType allowedThesisType,
                                         Boolean onlyClientInstitutions) {
        StringUtil.removeNotableStopwords(tokens);

        return BoolQuery.of(q -> q.must(mb -> mb.bool(b -> {

            addInstitutionFilter(b, personId, topLevelInstitutionId);

            if (Objects.nonNull(onlyReturnOnesWhichCanHarvest) && onlyReturnOnesWhichCanHarvest) {
                b.must(mbb -> mbb.bool(bq -> bq
                    .should(sh -> sh.exists(e -> e.field("scopus_afid")))
                    .should(sh -> sh.exists(e -> e.field("open_alex_id")))
                ));
            }

            if (Objects.nonNull(allowedThesisType)) {
                b.must(sb -> sb.term(
                    m -> m.field("allowed_thesis_types").value(allowedThesisType.name())));
            }

            if (Objects.nonNull(onlyClientInstitutions) && onlyClientInstitutions) {
                b.must(sb -> sb.term(
                    m -> m.field("is_client_institution").value(true)));
            }

            tokens.forEach(token -> {
                if (StringUtil.isInteger(token, 10)) {
                    b.should(sb -> sb.match(
                        m -> m.field("scopus_afid").query(token)));
                    return;
                }

                var perTokenShould = new ArrayList<Query>();
                var cleanedToken = token.replace("\\\"", "");

                if (token.startsWith("\\\"") && token.endsWith("\\\"")) {
                    perTokenShould.add(MatchPhraseQuery.of(
                        mq -> mq.field("name_sr").query(cleanedToken))._toQuery());
                    perTokenShould.add(MatchPhraseQuery.of(
                        mq -> mq.field("name_other").query(cleanedToken))._toQuery());
                } else if (token.endsWith("\\*") || token.endsWith(".")) {
                    var wildcard = token.replace("\\*", "").replace(".", "");
                    perTokenShould.add(WildcardQuery.of(
                            m -> m.field("name_sr")
                                .value(StringUtil.performSimpleLatinPreprocessing(wildcard) + "*")
                                .caseInsensitive(true))
                        ._toQuery());
                    perTokenShould.add(WildcardQuery.of(
                            m -> m.field("name_other").value(wildcard + "*").caseInsensitive(true))
                        ._toQuery());
                } else {
                    perTokenShould.add(WildcardQuery.of(
                            m -> m.field("name_sr")
                                .value(StringUtil.performSimpleLatinPreprocessing(token) + "*")
                                .caseInsensitive(true))
                        ._toQuery());
                    perTokenShould.add(WildcardQuery.of(
                            m -> m.field("name_other").value(token + "*").caseInsensitive(true))
                        ._toQuery());
                    perTokenShould.add(MatchQuery.of(
                        m -> m.field("name_sr").query(token))._toQuery());
                    perTokenShould.add(MatchQuery.of(
                        m -> m.field("name_other").query(token))._toQuery());
                    perTokenShould.add(TermQuery.of(
                        m -> m.field("keywords_sr").value(token).boost(0.7f))._toQuery());
                    perTokenShould.add(TermQuery.of(
                        m -> m.field("keywords_other").value(token).boost(0.7f))._toQuery());
                    perTokenShould.add(MatchQuery.of(
                        m -> m.field("research_areas_sr").query(token).boost(0.5f))._toQuery());
                    perTokenShould.add(MatchQuery.of(
                            m -> m.field("research_areas_other").query(token).boost(0.5f))
                        ._toQuery());
                }

                b.must(m -> m.bool(bb -> bb.should(perTokenShould)));

                b.should(sb -> sb.match(
                    m -> m.field("super_ou_name_sr").query(token).boost(0.3f)));
                b.should(sb -> sb.match(
                    m -> m.field("super_ou_name_other").query(token).boost(0.3f)));
            });

            if (Objects.nonNull(onlyIndependent) && onlyIndependent) {
                b.mustNot(mn -> mn.exists(e -> e.field("super_ou_id")));
            }

            return b;
        })))._toQuery();
    }

    private void addInstitutionFilter(BoolQuery.Builder b, Integer personId,
                                      Integer topLevelInstitutionId) {
        if (Objects.nonNull(personId)) {
            var allowedInstitutions =
                involvementRepository.findActiveEmploymentInstitutionIds(personId);
            b.must(createTermsQuery("databaseId", allowedInstitutions));
        }
        if (Objects.nonNull(topLevelInstitutionId)) {
            var allowedInstitutions =
                organisationUnitsRelationRepository.getSubOUsRecursive(topLevelInstitutionId);
            allowedInstitutions.add(topLevelInstitutionId);
            b.must(createTermsQuery("databaseId", allowedInstitutions));
        }
    }

    private Query createTermsQuery(String field, List<Integer> values) {
        return TermsQuery.of(t -> t
            .field(field)
            .terms(v -> v.value(values.stream()
                .map(String::valueOf)
                .map(FieldValue::of)
                .toList()))
        )._toQuery();
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
    @Nullable
    public OrganisationUnitsRelation getSuperOrganisationUnitRelation(Integer organisationUnitId) {
        return organisationUnitsRelationRepository.getSuperOU(organisationUnitId).orElse(null);
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
        var ouSubUnits =
            organisationUnitsRelationRepository.getSubOUsRecursive(currentOUNodeId);
        ouSubUnits.add(currentOUNodeId);

        return ouSubUnits;
    }

    @Override
    public List<Integer> getSuperOUsHierarchyRecursive(Integer sourceOUId) {
        return organisationUnitsRelationRepository.getSuperOUsRecursive(sourceOUId);
    }

    @Override
    public Page<OrganisationUnitIndex> getOUSubUnits(Integer organisationUnitId,
                                                     Pageable pageable) {
        return organisationUnitIndexRepository.findOrganisationUnitIndexesBySuperOUId(
            organisationUnitId, pageable);
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
    public OrganisationUnit editOrganisationUnit(Integer organisationUnitId,
                                                 OrganisationUnitRequestDTO organisationUnitDTORequest) {

        var organisationUnitToUpdate = getReferenceToOrganisationUnitById(organisationUnitId);

        var oldNames = organisationUnitToUpdate.getName().stream()
            .map(MultiLingualContent::getContent)
            .collect(Collectors.toSet());

        var newNames = organisationUnitDTORequest.getName().stream()
            .map(MultilingualContentDTO::getContent)
            .collect(Collectors.toSet());

        boolean isNameChanged = !oldNames.equals(newNames);

        organisationUnitToUpdate.getName().clear();
        organisationUnitToUpdate.getKeyword().clear();
        organisationUnitToUpdate.getResearchAreas().clear();
        organisationUnitToUpdate.getUris().clear();

        setCommonOUFields(organisationUnitToUpdate, organisationUnitDTORequest);

        organisationUnitToUpdate = save(organisationUnitToUpdate);

        if (organisationUnitToUpdate.getApproveStatus().equals(ApproveStatus.APPROVED)) {
            var index = organisationUnitIndexRepository.findOrganisationUnitIndexByDatabaseId(
                organisationUnitId).orElse(new OrganisationUnitIndex());
            indexOrganisationUnit(organisationUnitToUpdate, index);
        }

        if (isNameChanged) {
            applicationEventPublisher.publishEvent(
                new OrganisationUnitSignificantChangeEvent(organisationUnitId));
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

        IdentifierUtil.validateAndSetIdentifier(
            organisationUnitDTO.getScopusAfid(),
            organisationUnit.getId(),
            "^\\d+$",
            organisationUnitRepository::existsByScopusAfid,
            organisationUnit::setScopusAfid,
            "scopusAfidFormatError",
            "scopusAfidExistsError"
        );

        IdentifierUtil.validateAndSetIdentifier(
            organisationUnitDTO.getOpenAlexId(),
            organisationUnit.getId(),
            "^I\\d{4,10}$",
            organisationUnitRepository::existsByOpenAlexId,
            organisationUnit::setOpenAlexId,
            "openAlexIdFormatError",
            "openAlexIdExistsError"
        );

        IdentifierUtil.validateAndSetIdentifier(
            organisationUnitDTO.getRor(),
            organisationUnit.getId(),
            "^0[0-9a-hj-km-np-z]{8}$",
            organisationUnitRepository::existsByROR,
            organisationUnit::setRor,
            "rorFormatError",
            "rorExistsError"
        );

        if (Objects.nonNull(organisationUnitDTO.getOldId())) {
            organisationUnit.getOldIds().add(organisationUnitDTO.getOldId());
        }

        var researchAreas = researchAreaService.getResearchAreasByIds(
            organisationUnitDTO.getResearchAreasId());
        organisationUnit.setResearchAreas(new HashSet<>(researchAreas));

        organisationUnit.setLocation(
            GeoLocationConverter.fromDTO(organisationUnitDTO.getLocation()));

        organisationUnit.setContact(
            ContactConverter.fromDTO(organisationUnitDTO.getContact()));

        if (Objects.nonNull(organisationUnitDTO.getUris())) {
            IdentifierUtil.setUris(organisationUnit.getUris(), organisationUnitDTO.getUris());
        }

        if (Objects.nonNull(organisationUnitDTO.getAllowedThesisTypes())) {
            organisationUnit.setAllowedThesisTypes(
                organisationUnitDTO.getAllowedThesisTypes().stream().map(Enum::name)
                    .collect(Collectors.toSet()));
        }

        if (SessionUtil.isUserLoggedIn() && Objects.requireNonNull(
                SessionUtil.getLoggedInUser()).getAuthority().getName()
            .equals(UserRole.ADMIN.name())) {
            organisationUnit.setLegalEntity(organisationUnitDTO.isLegalEntity());

            if (!organisationUnit.getIsClientInstitution()
                .equals(organisationUnitDTO.isClientInstitution())) {
                organisationUnit.setIsClientInstitution(organisationUnitDTO.isClientInstitution());

                getOrganisationUnitIdsFromSubHierarchy(organisationUnit.getId()).forEach(
                    organisationUnitId -> {
                        var subOU = findOne(organisationUnitId);
                        subOU.setIsClientInstitution(organisationUnitDTO.isClientInstitution());
                        subOU.setValidateEmailDomain(organisationUnitDTO.isValidatingEmailDomain());
                        subOU.setAllowSubdomains(organisationUnitDTO.isAllowingSubdomains());
                        subOU.setInstitutionEmailDomain(
                            organisationUnitDTO.getInstitutionEmailDomain());
                        save(subOU);
                    });
            }
        }

        organisationUnit.setValidateEmailDomain(organisationUnitDTO.isValidatingEmailDomain());
        organisationUnit.setAllowSubdomains(organisationUnitDTO.isAllowingSubdomains());

        if (organisationUnit.getValidateEmailDomain() &&
            (Objects.isNull(organisationUnitDTO.getInstitutionEmailDomain()) ||
                organisationUnitDTO.getInstitutionEmailDomain().isBlank())) {
            throw new IllegalArgumentException(
                "You have to specify the domain when domain validation is specified.");
        }

        organisationUnit.setInstitutionEmailDomain(organisationUnitDTO.getInstitutionEmailDomain());
    }

    @Override
    public boolean isIdentifierInUse(String identifier, Integer organisationUnitId) {
        return organisationUnitRepository.existsByScopusAfid(identifier, organisationUnitId) ||
            organisationUnitRepository.existsByOpenAlexId(identifier, organisationUnitId);
    }

    @Override
    public List<Triple<String, List<MultilingualContentDTO>, String>> getSearchFields(
        Boolean onlyExportFields) {
        return searchFieldsLoader.getSearchFields("organisationUnitSearchFieldConfiguration.json",
            onlyExportFields);
    }

    @Override
    public OrganisationUnit findOrganisationUnitByAccountingId(String accountingId) {
        return organisationUnitRepository.findApprovedOrganisationUnitByAccountingId(accountingId)
            .orElseThrow(
                () -> new NotFoundException(
                    "Organisation unit with accounting ID " + accountingId + " does not exist"));
    }

    @Override
    public String setOrganisationUnitLogo(Integer organisationUnitId, ProfilePhotoOrLogoDTO logoDTO)
        throws IOException {
        if (ImageUtil.isMIMETypeInvalid(logoDTO.getFile(), true)) {
            throw new IllegalArgumentException("mimeTypeValidationFailed");
        }

        var organisationUnit = findOne(organisationUnitId);

        if (Objects.nonNull(organisationUnit.getLogo()) &&
            Objects.nonNull(organisationUnit.getLogo().getImageServerName()) &&
            !logoDTO.getFile().isEmpty()) {
            fileService.delete(organisationUnit.getLogo().getImageServerName());
        } else if (Objects.isNull(organisationUnit.getLogo())) {
            organisationUnit.setLogo(new ProfilePhotoOrLogo());
        }

        organisationUnit.getLogo().setTopOffset(logoDTO.getTop());
        organisationUnit.getLogo().setLeftOffset(logoDTO.getLeft());
        organisationUnit.getLogo().setHeight(logoDTO.getHeight());
        organisationUnit.getLogo().setWidth(logoDTO.getWidth());
        organisationUnit.getLogo().setBackgroundHex(logoDTO.getBackgroundHex().trim());

        var serverFilename = organisationUnit.getLogo().getImageServerName();
        if (!logoDTO.getFile().isEmpty()) {
            serverFilename =
                fileService.store(logoDTO.getFile(), UUID.randomUUID().toString());
            organisationUnit.getLogo().setImageServerName(serverFilename);
        }

        save(organisationUnit);
        return serverFilename;
    }

    @Override
    public void removeOrganisationUnitLogo(Integer organisationUnitId) {
        var organisationUnit = findOne(organisationUnitId);

        if (Objects.nonNull(organisationUnit.getLogo()) &&
            Objects.nonNull(organisationUnit.getLogo().getImageServerName())) {
            fileService.delete(organisationUnit.getLogo().getImageServerName());
            organisationUnit.getLogo().setImageServerName(null);
            organisationUnit.getLogo().setTopOffset(null);
            organisationUnit.getLogo().setLeftOffset(null);
            organisationUnit.getLogo().setHeight(null);
            organisationUnit.getLogo().setWidth(null);
            organisationUnit.getLogo().setBackgroundHex(null);
        }

        save(organisationUnit);
    }

    @Override
    public void indexOrganisationUnit(OrganisationUnit organisationUnit,
                                      Integer organisationUnitId) {
        organisationUnitIndexRepository.findOrganisationUnitIndexByDatabaseId(organisationUnitId)
            .ifPresent(index -> {
                indexOrganisationUnit(organisationUnit, index);
            });
    }

    @Override
    public void indexOrganisationUnit(OrganisationUnit organisationUnit) {
        indexOrganisationUnit(organisationUnit,
            organisationUnitIndexRepository.findOrganisationUnitIndexByDatabaseId(
                organisationUnit.getId()).orElse(new OrganisationUnitIndex()));
    }

    @Override
    public OrganisationUnit findRaw(Integer organisationUnitId) {
        return organisationUnitRepository.findRaw(organisationUnitId).orElseThrow(
            () -> new NotFoundException("Organisation Unit with given ID does not exist."));
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

        index.setScopusAfid(
            (Objects.nonNull(organisationUnit.getScopusAfid()) &&
                !organisationUnit.getScopusAfid().isBlank()) ? organisationUnit.getScopusAfid() :
                null);
        index.setOpenAlexId(
            (Objects.nonNull(organisationUnit.getOpenAlexId()) &&
                !organisationUnit.getOpenAlexId().isBlank()) ? organisationUnit.getOpenAlexId() :
                null);
        index.setRor(organisationUnit.getRor());

        indexMultilingualContent(index, organisationUnit, OrganisationUnit::getKeyword,
            OrganisationUnitIndex::setKeywordsSr,
            OrganisationUnitIndex::setKeywordsOther);

        var researchAreaSrContent = new StringBuilder();
        var researchAreaOtherContent = new StringBuilder();

        organisationUnit.getResearchAreas().forEach(
            researchArea -> multilingualContentService.buildLanguageStrings(researchAreaSrContent,
                researchAreaOtherContent,
                researchArea.getName(), true));

        StringUtil.removeTrailingDelimiters(researchAreaSrContent, researchAreaOtherContent);
        index.setResearchAreasSr(
            !researchAreaSrContent.isEmpty() ? researchAreaSrContent.toString() :
                researchAreaOtherContent.toString());
        index.setResearchAreasOther(
            !researchAreaOtherContent.isEmpty() ? researchAreaOtherContent.toString() :
                researchAreaSrContent.toString());

        indexBelongsToSuperOURelation(organisationUnit, index);

        index.getAllowedThesisTypes().clear();
        index.getAllowedThesisTypes().addAll(organisationUnit.getAllowedThesisTypes());

        index.setIsClientInstitution(organisationUnit.getIsClientInstitution());
    }

    private void indexBelongsToSuperOURelation(OrganisationUnit organisationUnit,
                                               OrganisationUnitIndex index) {
        var belongsToRelation =
            organisationUnitsRelationRepository.getSuperOU(organisationUnit.getId());
        belongsToRelation.ifPresent(organisationUnitsRelation -> {
            indexMultilingualContent(index,
                organisationUnitsRelation.getTargetOrganisationUnit(),
                OrganisationUnit::getName,
                OrganisationUnitIndex::setSuperOUNameSr,
                OrganisationUnitIndex::setSuperOUNameOther);

            indexMultilingualContent(index,
                organisationUnitsRelation.getTargetOrganisationUnit(),
                OrganisationUnit::getName,
                OrganisationUnitIndex::setSuperOUNameSrSortable,
                OrganisationUnitIndex::setSuperOUNameOtherSortable);

            index.setSuperOUId(belongsToRelation.get().getTargetOrganisationUnit().getId());
        });
    }

    private void indexMultilingualContent(OrganisationUnitIndex index,
                                          OrganisationUnit organisationUnit,
                                          Function<OrganisationUnit, Set<MultiLingualContent>> contentExtractor,
                                          BiConsumer<OrganisationUnitIndex, String> srSetter,
                                          BiConsumer<OrganisationUnitIndex, String> otherSetter) {
        Set<MultiLingualContent> contentList = contentExtractor.apply(organisationUnit);

        var srContent = new StringBuilder();
        var otherContent = new StringBuilder();

        multilingualContentService.buildLanguageStrings(srContent, otherContent, contentList, true);

        StringUtil.removeTrailingDelimiters(srContent, otherContent);
        srSetter.accept(index,
            !srContent.isEmpty() ? srContent.toString() : otherContent.toString());
        otherSetter.accept(index,
            !otherContent.isEmpty() ? otherContent.toString() : srContent.toString());
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
            throw new OrganisationUnitReferenceConstraintViolationException(
                "Organisation unit is already in use.");
        }

        delete(organisationUnitId);
        var index = organisationUnitIndexRepository.findOrganisationUnitIndexByDatabaseId(
            organisationUnitId);
        index.ifPresent(organisationUnitIndexRepository::delete);

        // TODO: Add listeners wherever this could raise a side effect (index-wise)
        applicationEventPublisher.publishEvent(
            new OrganisationUnitDeletedEvent(organisationUnitId));
    }

    @Override
    public void forceDeleteOrganisationUnit(Integer organisationUnitId) {
        organisationUnitRepository.deleteInvolvementsForOrganisationUnit(organisationUnitId);
        organisationUnitRepository.deleteRelationsForOrganisationUnit(organisationUnitId);

        // Migrate to non-managed OU for theses
        migrateThesesToUnmanagedOU(organisationUnitId);

        // Delete all institutional editors and their user account index
        organisationUnitRepository.fetchInstitutionalEditorsForOrganisationUnit(organisationUnitId)
            .forEach(user -> {
                user.setDeleted(true);
                userAccountIndexRepository.findByDatabaseId(user.getId())
                    .ifPresent(userAccountIndexRepository::delete);
            });

        delete(organisationUnitId);
        var index = organisationUnitIndexRepository.findOrganisationUnitIndexByDatabaseId(
            organisationUnitId);
        index.ifPresent(organisationUnitIndexRepository::delete);

        indexBulkUpdateService.removeIdFromListField("document_publication",
            "organisation_unit_ids",
            organisationUnitId);
    }

    public void migrateThesesToUnmanagedOU(Integer organisationUnitId) {
        int pageNumber = 0;
        int chunkSize = 100;
        boolean hasNextPage = true;

        while (hasNextPage) {

            List<Thesis> chunk =
                organisationUnitRepository.fetchAllThesesForOU(organisationUnitId,
                    PageRequest.of(pageNumber, chunkSize)).getContent();

            chunk.forEach((thesis) -> {
                thesis.getOrganisationUnit().getName().forEach(mc -> {
                    thesis.getExternalOrganisationUnitName().add(
                        new MultiLingualContent(mc.getLanguage(), mc.getContent(),
                            mc.getPriority()));
                });

                thesis.setOrganisationUnit(null);
            });

            pageNumber++;
            hasNextPage = chunk.size() == chunkSize;
        }
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
        updateIndex(savedRelation);

        return savedRelation;
    }

    @Override
    public OrganisationUnitsRelationDTO addSubOrganisationUnit(Integer sourceId, Integer targetId) {
        if (organisationUnitsRelationRepository.getSuperOU(targetId).isPresent()) {
            throw new OrganisationUnitReferenceConstraintViolationException(
                "Organisation unit already has a super relation.");
        } else if (sourceId.equals(targetId)) {
            throw new OrganisationUnitReferenceConstraintViolationException(
                "cyclicOrgUnitRelationLabel");
        }

        var newRelation = new OrganisationUnitsRelation();
        var creationDTO = new OrganisationUnitsRelationDTO() {{
            setSourceOrganisationUnitId(targetId);
            setTargetOrganisationUnitId(sourceId);
            setRelationType(OrganisationUnitRelationType.BELONGS_TO);
            setSourceAffiliationStatement(Collections.emptyList());
            setTargetAffiliationStatement(Collections.emptyList());
        }};
        setCommonOURelationFields(newRelation, creationDTO);
        newRelation.setApproveStatus(ApproveStatus.APPROVED);

        var savedRelation = organisationUnitsRelationRepository.save(newRelation);
        creationDTO.setId(savedRelation.getId());

        updateIndex(savedRelation);

        return creationDTO;
    }

    private void updateIndex(OrganisationUnitsRelation savedRelation) {
        var index = organisationUnitIndexRepository.findOrganisationUnitIndexByDatabaseId(
            savedRelation.getSourceOrganisationUnit().getId());
        index.ifPresent(organisationUnitIndex -> {
            indexBelongsToSuperOURelation(savedRelation.getSourceOrganisationUnit(),
                organisationUnitIndex);
            organisationUnitIndex.setIsClientInstitution(
                savedRelation.getTargetOrganisationUnit().getIsClientInstitution());
            organisationUnitIndexRepository.save(organisationUnitIndex);
        });
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
        var relationToDelete = findOrganisationUnitsRelationById(id);
        organisationUnitsRelationJPAService.delete(id);

        reindexSubUnitRelationsAndTerminateClientStatus(
            relationToDelete.getSourceOrganisationUnit());
    }

    @Override
    public void deleteOrganisationUnitsRelation(Integer sourceOrganisationUnitId,
                                                Integer targetOrganisationUnitId) {
        var relations =
            organisationUnitsRelationRepository.findBySourceOrganisationUnitIdAndTargetOrganisationUnitIdAndRelationType(
                sourceOrganisationUnitId, targetOrganisationUnitId,
                OrganisationUnitRelationType.BELONGS_TO);
        if (relations.isEmpty()) {
            return;
        }

        var subOu = relations.getFirst().getSourceOrganisationUnit();
        reindexSubUnitRelationsAndTerminateClientStatus(subOu);

        organisationUnitsRelationRepository.delete(relations.getFirst());
    }

    private void reindexSubUnitRelationsAndTerminateClientStatus(
        OrganisationUnit organisationUnit) {
        var index = organisationUnitIndexRepository.findOrganisationUnitIndexByDatabaseId(
            organisationUnit.getId());
        index.ifPresent(organisationUnitIndex -> {
            organisationUnitIndex.setSuperOUId(null);
            organisationUnitIndex.setSuperOUNameSr(null);
            organisationUnitIndex.setSuperOUNameSrSortable(null);
            organisationUnitIndex.setSuperOUNameOther(null);
            organisationUnitIndex.setSuperOUNameOtherSortable(null);
            organisationUnitIndexRepository.save(organisationUnitIndex);
        });

        getOrganisationUnitIdsFromSubHierarchy(organisationUnit.getId()).forEach(
            organisationUnitId -> {
                var subOU = findOne(organisationUnitId);
                subOU.setIsClientInstitution(false);
                subOU.setValidateEmailDomain(false);
                subOU.setAllowSubdomains(false);
                save(subOU);
            });
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

        if (relation.getTargetOrganisationUnit().getIsClientInstitution() &&
            !relation.getSourceOrganisationUnit().getIsClientInstitution()) {
            getOrganisationUnitIdsFromSubHierarchy(
                relation.getTargetOrganisationUnit().getId()).forEach(
                organisationUnitId -> {
                    var subOU = findOne(organisationUnitId);
                    subOU.setIsClientInstitution(
                        relation.getTargetOrganisationUnit().getIsClientInstitution());
                    subOU.setValidateEmailDomain(
                        relation.getTargetOrganisationUnit().getValidateEmailDomain());
                    subOU.setAllowSubdomains(
                        relation.getTargetOrganisationUnit().getAllowSubdomains());
                    save(subOU);
                });
        }
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
    public boolean checkIfInstitutionalAdminsExist(Integer organisationUnitId) {
        return organisationUnitRepository.checkIfInstitutionalAdminsExist(organisationUnitId);
    }

    @Override
    @Async("reindexExecutor")
    @Transactional(readOnly = true)
    public CompletableFuture<Void> reindexOrganisationUnits() {
        organisationUnitIndexRepository.deleteAll();
        int pageNumber = 0;
        int chunkSize = 100;
        boolean hasNextPage = true;

        while (hasNextPage) {

            List<OrganisationUnit> chunk =
                findAll(PageRequest.of(pageNumber, chunkSize)).getContent();

            chunk.forEach((organisationUnit) -> indexOrganisationUnit(organisationUnit,
                new OrganisationUnitIndex()));

            pageNumber++;
            hasNextPage = chunk.size() == chunkSize;
        }
        return null;
    }
}
