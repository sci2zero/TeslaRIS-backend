package rs.teslaris.core.service.impl.institution;

import co.elastic.clients.elasticsearch._types.FieldValue;
import co.elastic.clients.elasticsearch._types.query_dsl.BoolQuery;
import co.elastic.clients.elasticsearch._types.query_dsl.Query;
import co.elastic.clients.elasticsearch._types.query_dsl.TermQuery;
import co.elastic.clients.elasticsearch._types.query_dsl.TermsQuery;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import rs.teslaris.core.dto.institution.OrganisationUnitTrustConfigurationDTO;
import rs.teslaris.core.indexmodel.DocumentPublicationIndex;
import rs.teslaris.core.indexmodel.DocumentPublicationType;
import rs.teslaris.core.indexrepository.DocumentPublicationIndexRepository;
import rs.teslaris.core.model.commontypes.ApproveStatus;
import rs.teslaris.core.model.document.Document;
import rs.teslaris.core.model.institution.OrganisationUnitTrustConfiguration;
import rs.teslaris.core.model.user.User;
import rs.teslaris.core.repository.commontypes.NotificationRepository;
import rs.teslaris.core.repository.document.DocumentFileRepository;
import rs.teslaris.core.repository.document.DocumentRepository;
import rs.teslaris.core.repository.institution.OrganisationUnitTrustConfigurationRepository;
import rs.teslaris.core.service.impl.JPAServiceImpl;
import rs.teslaris.core.service.interfaces.commontypes.SearchService;
import rs.teslaris.core.service.interfaces.institution.OrganisationUnitService;
import rs.teslaris.core.service.interfaces.institution.OrganisationUnitTrustConfigurationService;
import rs.teslaris.core.service.interfaces.user.UserService;
import rs.teslaris.core.util.Pair;
import rs.teslaris.core.util.notificationhandling.NotificationFactory;

@Service
@RequiredArgsConstructor
@Transactional
public class OrganisationUnitTrustConfigurationServiceImpl
    extends JPAServiceImpl<OrganisationUnitTrustConfiguration> implements
    OrganisationUnitTrustConfigurationService {

    private static final ConcurrentHashMap<Integer, Object> locks = new ConcurrentHashMap<>();

    private final OrganisationUnitTrustConfigurationRepository
        organisationUnitTrustConfigurationRepository;

    private final OrganisationUnitService organisationUnitService;

    private final DocumentRepository documentRepository;

    private final DocumentPublicationIndexRepository documentPublicationIndexRepository;

    private final DocumentFileRepository documentFileRepository;

    private final SearchService<DocumentPublicationIndex> searchService;

    private final NotificationRepository notificationRepository;

    private final UserService userService;


    @Override
    protected JpaRepository<OrganisationUnitTrustConfiguration, Integer> getEntityRepository() {
        return organisationUnitTrustConfigurationRepository;
    }

    @Override
    public OrganisationUnitTrustConfigurationDTO readTrustConfigurationForOrganisationUnit(
        Integer organisationUnitId) {
        var configuration =
            organisationUnitTrustConfigurationRepository.findConfigurationForOrganisationUnit(
                organisationUnitId);

        return configuration.map(
                organisationUnitTrustConfiguration -> new OrganisationUnitTrustConfigurationDTO(
                    organisationUnitTrustConfiguration.getTrustNewPublications(),
                    organisationUnitTrustConfiguration.getTrustNewDocumentFiles()))
            .orElseGet(() -> new OrganisationUnitTrustConfigurationDTO(true, false));
    }

    @Override
    public OrganisationUnitTrustConfigurationDTO saveConfiguration(
        OrganisationUnitTrustConfigurationDTO dto, Integer organisationUnitId) {
        var configuration =
            organisationUnitTrustConfigurationRepository.findConfigurationForOrganisationUnit(
                organisationUnitId).orElseGet(() -> {
                var config = new OrganisationUnitTrustConfiguration();
                config.setOrganisationUnit(
                    organisationUnitService.findOrganisationUnitById(organisationUnitId));
                return config;
            });


        configuration.setTrustNewPublications(dto.trustNewPublications());
        configuration.setTrustNewDocumentFiles(dto.trustNewDocumentFiles());
        save(configuration);

        return dto;
    }

    @Override
    public void approvePublicationMetadata(Integer documentId) {
        var lock = locks.computeIfAbsent(documentId, id -> new Object());
        documentRepository.findById(documentId).ifPresent(document -> {
            if (document.getIsMetadataValid()) {
                return;
            }

            synchronized (lock) {
                try {
                    document.setIsMetadataValid(true);
                    document.setApproveStatus(ApproveStatus.APPROVED);
                    updateDocumentIndex(document);
                    documentRepository.save(document);
                } finally {
                    locks.remove(documentId);
                }
            }
        });
    }

    @Override
    public void approvePublicationUploadedDocuments(Integer documentId) {
        var lock = locks.computeIfAbsent(documentId, id -> new Object());
        documentRepository.findById(documentId).ifPresent(document -> {
            if (document.getAreFilesValid()) {
                return;
            }

            synchronized (lock) {
                try {
                    document.getFileItems().forEach(file -> {
                        file.setIsVerifiedData(true);
                        documentFileRepository.save(file);
                    });

                    document.getProofs().forEach(file -> {
                        file.setIsVerifiedData(true);
                        documentFileRepository.save(file);
                    });

                    document.setAreFilesValid(true);
                    updateDocumentIndex(document);
                    documentRepository.save(document);
                } finally {
                    locks.remove(documentId);
                }
            }
        });
    }

    @Override
    public Pair<Boolean, Boolean> fetchValidationStatusForDocument(Integer documentId) {
        var document = documentRepository.findById(documentId);

        return document.map(
                value -> new Pair<>(value.getIsMetadataValid(), value.getAreFilesValid()))
            .orElseGet(() -> new Pair<>(false, false));
    }

    @Override
    public Page<DocumentPublicationIndex> fetchNonValidatedPublications(Integer institutionId,
                                                                        Boolean nonValidatedMetadata,
                                                                        Boolean nonValidatedFiles,
                                                                        List<DocumentPublicationType> allowedTypes,
                                                                        Pageable pageable) {
        if (Objects.isNull(nonValidatedMetadata) || Objects.isNull(nonValidatedFiles) ||
            (!nonValidatedMetadata && !nonValidatedFiles)) {
            return new PageImpl<>(Collections.emptyList());
        }

        var finalQuery = BoolQuery.of(b -> b.must(
            buildNonValidatedPublicationsSearchQuery(institutionId, nonValidatedMetadata,
                nonValidatedFiles, allowedTypes)))._toQuery();

        return searchService.runQuery(finalQuery, pageable, DocumentPublicationIndex.class,
            "document_publication");
    }

    private void updateDocumentIndex(Document document) {
        documentPublicationIndexRepository.findDocumentPublicationIndexByDatabaseId(
            document.getId()).ifPresent(index -> {
            index.setIsApproved(document.getIsMetadataValid());
            index.setAreFilesValid(document.getAreFilesValid());
            documentPublicationIndexRepository.save(index);
        });
    }

    private Set<Integer> getInstitutionIds(Integer institutionId) {
        if (Objects.nonNull(institutionId)) {
            return new HashSet<>(
                organisationUnitService.getOrganisationUnitIdsFromSubHierarchy(institutionId));
        }
        return Collections.emptySet();
    }

    private Query buildInstitutionQuery(Set<Integer> institutionIds) {
        return TermsQuery.of(t -> t
            .field("organisation_unit_ids")
            .terms(ts -> ts.value(
                institutionIds.stream().map(FieldValue::of).toList()
            ))
        )._toQuery();
    }

    private Query createTypeTermsQuery(List<DocumentPublicationType> values) {
        return TermsQuery.of(t -> t
            .field("type")
            .terms(v -> v.value(values.stream()
                .map(DocumentPublicationType::name)
                .map(FieldValue::of)
                .toList()))
        )._toQuery();
    }

    private List<Query> constructValidationStatusQuery(Boolean nonValidatedMetadata,
                                                       Boolean nonValidatedFiles) {
        List<Query> validationStatusQueries = new ArrayList<>();

        if (nonValidatedMetadata) {
            validationStatusQueries.add(
                TermQuery.of(t -> t
                    .field("is_approved")
                    .value(false)
                )._toQuery()
            );
        }
        if (nonValidatedFiles) {
            validationStatusQueries.add(
                TermQuery.of(t -> t
                    .field("are_files_valid")
                    .value(false)
                )._toQuery()
            );
        }

        return validationStatusQueries;
    }

    private List<Query> buildNonValidatedPublicationsSearchQuery(Integer institutionId,
                                                                 Boolean nonValidatedMetadata,
                                                                 Boolean nonValidatedFiles,
                                                                 List<DocumentPublicationType> allowedTypes) {
        List<Query> mustQueries = new ArrayList<>();

        if (Objects.nonNull(institutionId)) {
            Set<Integer> institutionIds = getInstitutionIds(institutionId);
            mustQueries.add(buildInstitutionQuery(institutionIds));
        }

        mustQueries.add(
            BoolQuery.of(b -> b
                .should(constructValidationStatusQuery(nonValidatedMetadata, nonValidatedFiles))
                .minimumShouldMatch("1") // Important, otherwise it will return the full index!
            )._toQuery()
        );

        if (Objects.nonNull(allowedTypes) && !allowedTypes.isEmpty()) {
            mustQueries.add(createTypeTermsQuery(allowedTypes));
        }

        return mustQueries;
    }

    @Scheduled(cron = "${entity-validation.document.notify-period}")
    protected void dispatchValidationNotifications() {
        int pageNumber = 0;
        int chunkSize = 50;
        boolean hasNextPage = true;

        var institutionValidationCount = new HashMap<Integer, Integer>();
        var finalQuery = BoolQuery.of(b -> b.must(
            buildNonValidatedPublicationsSearchQuery(null, true, true,
                Arrays.asList(DocumentPublicationType.values()))))._toQuery();

        while (hasNextPage) {
            List<DocumentPublicationIndex> chunk =
                searchService.runQuery(finalQuery, PageRequest.of(pageNumber, chunkSize),
                    DocumentPublicationIndex.class, "document_publication").getContent();

            chunk.forEach((document) -> {
                if (Objects.nonNull(document.getThesisInstitutionId())) {
                    institutionValidationCount.put(document.getThesisInstitutionId(),
                        institutionValidationCount.getOrDefault(document.getThesisInstitutionId(),
                            0) + 1);
                } else {
                    document.getOrganisationUnitIds().forEach(
                        orgUnitId -> institutionValidationCount.put(orgUnitId,
                            institutionValidationCount.getOrDefault(orgUnitId, 0) + 1));
                }
            });

            pageNumber++;
            hasNextPage = chunk.size() == chunkSize;
        }

        institutionValidationCount.forEach((institutionId, amountToValidate) -> {
            findInstitutionalEditorsForInstitution(institutionId).forEach(user -> {
                notificationRepository.save(
                    NotificationFactory.contructNewDocumentsForValidationNotification(
                        Map.of("nonValidatedDocumentsCount", String.valueOf(amountToValidate)),
                        user));
            });
        });
    }

    public List<User> findInstitutionalEditorsForInstitution(Integer institutionId) {
        var potentialMatches =
            userService.findInstitutionalEditorUsersForInstitutionId(institutionId);

        if (potentialMatches.isEmpty()) {
            var topLevelInstitutionIds =
                organisationUnitService.getSuperOUsHierarchyRecursive(institutionId);
            for (int id : topLevelInstitutionIds) {
                potentialMatches = userService.findInstitutionalEditorUsersForInstitutionId(id);
                if (!potentialMatches.isEmpty()) {
                    break;
                }
            }
        }

        return potentialMatches;
    }
}
