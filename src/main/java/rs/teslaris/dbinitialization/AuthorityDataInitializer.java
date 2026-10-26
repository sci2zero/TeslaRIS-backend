package rs.teslaris.dbinitialization;

import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;
import rs.teslaris.core.model.user.Authority;
import rs.teslaris.core.model.user.Privilege;
import rs.teslaris.core.model.user.UserRole;
import rs.teslaris.core.repository.user.AuthorityRepository;
import rs.teslaris.core.repository.user.PrivilegeRepository;

/**
 * Seeds privileges and the authorities (roles) that group them.
 * <p>
 * The order of the {@code saveAll} lists below determines the generated IDs, which integration
 * tests rely on. Both lists are kept exactly as they were, so entries may only ever be appended -
 * never reordered or removed.
 */
@Component
@RequiredArgsConstructor
@Transactional
public class AuthorityDataInitializer {

    private final PrivilegeRepository privilegeRepository;

    private final AuthorityRepository authorityRepository;


    public SeededAuthorities initializeAuthorities() {
        var p = savePrivileges();

        var adminAuthority = adminAuthority(p);
        var researcherAuthority = researcherAuthority(p);
        var institutionalEditorAuthority = institutionalEditorAuthority(p);
        var commissionAuthority = commissionAuthority(p);
        var viceDeanForScienceAuthority = viceDeanForScienceAuthority(p);
        var institutionalLibrarianAuthority = institutionalLibrarianAuthority(p);
        var headOfLibraryAuthority = headOfLibraryAuthority(p);
        var promotionRegistryAdministratorAuthority = promotionRegistryAdministratorAuthority(p);

        authorityRepository.saveAll(
            List.of(adminAuthority, researcherAuthority, institutionalEditorAuthority,
                commissionAuthority, viceDeanForScienceAuthority, institutionalLibrarianAuthority,
                headOfLibraryAuthority, promotionRegistryAdministratorAuthority));

        return new SeededAuthorities(adminAuthority, researcherAuthority,
            institutionalEditorAuthority, commissionAuthority, viceDeanForScienceAuthority,
            institutionalLibrarianAuthority, headOfLibraryAuthority,
            promotionRegistryAdministratorAuthority);
    }

    private Privileges savePrivileges() {
        var allowAccountTakeover = new Privilege("ALLOW_ACCOUNT_TAKEOVER");
        var takeRoleOfUser = new Privilege("TAKE_ROLE");
        var deactivateUser = new Privilege("DEACTIVATE_USER");
        var updateProfile = new Privilege("UPDATE_PROFILE");
        var createUserBasic = new Privilege("REGISTER_PERSON");
        var editPersonalInfo = new Privilege("EDIT_PERSON_INFORMATION");
        var approvePerson = new Privilege("APPROVE_PERSON");
        var editDocumentFiles = new Privilege("EDIT_DOCUMENT_FILES");
        var approvePublication = new Privilege("APPROVE_PUBLICATION");
        var editResearchAreas = new Privilege("EDIT_RESEARCH_AREAS");
        var editOrganisationUnit = new Privilege("EDIT_ORGANISATION_UNITS");
        var editEmploymentInstitution = new Privilege("EDIT_EMPLOYMENT_INSTITUTION");
        var editOURelations = new Privilege("EDIT_OU_RELATIONS");
        var editPublishers = new Privilege("EDIT_PUBLISHERS");
        var createPublisher = new Privilege("CREATE_PUBLISHER");
        var editPublicationSeries = new Privilege("EDIT_PUBLICATION_SERIES");
        var editConferences = new Privilege("EDIT_CONFERENCES");
        var createConference = new Privilege("CREATE_CONFERENCE");
        var editEventRelations = new Privilege("EDIT_EVENT_RELATIONS");
        var mergeJournalPublications = new Privilege("MERGE_JOURNAL_PUBLICATIONS");
        var mergePersonPublications = new Privilege("MERGE_PERSON_PUBLICATIONS");
        var mergePersonMetadata = new Privilege("MERGE_PERSON_METADATA");
        var mergeOUEmployments = new Privilege("MERGE_OU_EMPLOYMENTS");
        var mergeOUMetadata = new Privilege("MERGE_OU_METADATA");
        var mergeConferenceProceedings = new Privilege("MERGE_CONFERENCE_PROCEEDINGS");
        var mergeProceedingsPublications = new Privilege("MERGE_PROCEEDINGS_PUBLICATIONS");
        var startDeduplicationProcess = new Privilege("START_DEDUPLICATION_PROCESS");
        var performDeduplication = new Privilege("PERFORM_DEDUPLICATION");
        var mergeDocumentsMetadata = new Privilege("MERGE_DOCUMENTS_METADATA");
        var mergeEventMetadata = new Privilege("MERGE_EVENT_METADATA");
        var mergePublicationSeriesMetadata = new Privilege("MERGE_PUBLICATION_SERIES_METADATA");
        var mergeMonographPublications = new Privilege("MERGE_MONOGRAPH_PUBLICATIONS");
        var prepareExportData = new Privilege("PREPARE_EXPORT_DATA");
        var editIndicators = new Privilege("EDIT_INDICATORS");
        var editAssessmentClassifications = new Privilege("EDIT_ASSESSMENT_CLASSIFICATIONS");
        var editAssessmentMeasures = new Privilege("EDIT_ASSESSMENT_MEASURES");
        var editAssessmentRulebooks = new Privilege("EDIT_ASSESSMENT_RULEBOOKS");
        var editDocumentIndicators = new Privilege("EDIT_DOCUMENT_INDICATORS");
        var editCommissions = new Privilege("EDIT_COMMISSIONS");
        var updateCommission = new Privilege("UPDATE_COMMISSION");
        var editEntityIndicatorProofs = new Privilege("EDIT_ENTITY_INDICATOR_PROOFS");
        var editEntityIndicators = new Privilege("EDIT_ENTITY_INDICATOR");
        var listMyJournalPublications = new Privilege("LIST_MY_JOURNAL_PUBLICATIONS");
        var deletePerson = new Privilege("DELETE_PERSON");
        var registerEmployee = new Privilege("REGISTER_EMPLOYEE");
        var reindexPrivilege = new Privilege("REINDEX_DATABASE");
        var mergeBookSeriesPublications = new Privilege("MERGE_BOOK_SERIES_PUBLICATIONS");
        var editCountries = new Privilege("EDIT_COUNTRIES");
        var forceDelete = new Privilege("FORCE_DELETE_ENTITIES");
        var switchEntityToUnmanaged = new Privilege("SWITCH_ENTITY_TO_UNMANAGED");
        var claimDocument = new Privilege("CLAIM_DOCUMENT");
        var mergePublisherPublications = new Privilege("MERGE_PUBLISHER_PUBLICATIONS");
        var mergePublishersMetadata = new Privilege("MERGE_PUBLISHERS_METADATA");
        var unbindYourselfFromPublication = new Privilege("UNBIND_YOURSELF_FROM_PUBLICATION");
        var unbindEmployeesFromPublication = new Privilege("UNBIND_EMPLOYEES_FROM_PUBLICATION");
        var editEntityAssessmentClassifications =
            new Privilege("EDIT_ENTITY_ASSESSMENT_CLASSIFICATION");
        var editEventAssessmentClassification =
            new Privilege("EDIT_EVENT_ASSESSMENT_CLASSIFICATION");
        var editLanguageTags = new Privilege("EDIT_LANGUAGE_TAGS");
        var editEventIndicators = new Privilege("EDIT_EVENT_INDICATORS");
        var scheduleTask = new Privilege("SCHEDULE_TASK");
        var editPublicationSeriesAssessmentClassifications =
            new Privilege("EDIT_PUB_SERIES_ASSESSMENT_CLASSIFICATION");
        var editPubSeriesIndicators = new Privilege("EDIT_PUB_SERIES_INDICATORS");
        var assessDocument = new Privilege("ASSESS_DOCUMENT");
        var editDocumentAssessment = new Privilege("EDIT_DOCUMENT_ASSESSMENT");
        var editAssessmentResearchArea = new Privilege("EDIT_ASSESSMENT_RESEARCH_AREA");
        var scheduleReportGeneration = new Privilege("SCHEDULE_REPORT_GENERATION");
        var downloadReports = new Privilege("DOWNLOAD_REPORTS");
        var listAssessmentClassifications = new Privilege("LIST_ASSESSMENT_CLASSIFICATIONS");
        var updateBrandingInformation = new Privilege("UPDATE_BRANDING_INFORMATION");
        var manageApiKeys = new Privilege("MANAGE_API_KEYS");
        var manageThesisAttachments = new Privilege("MANAGE_THESIS_ATTACHMENTS");
        var deleteThesisAttachments = new Privilege("DELETE_THESIS_ATTACHMENTS");
        var putThesisOnPublicReview = new Privilege("PUT_THESIS_ON_PUBLIC_REVIEW");
        var removeThesisFromPublicReview = new Privilege("REMOVE_THESIS_FROM_PUBLIC_REVIEW");
        var archiveThesis = new Privilege("ARCHIVE_THESIS");
        var unarchiveThesis = new Privilege("UNARCHIVE_THESIS");
        var performThesisReport = new Privilege("PERFORM_THESIS_REPORT");
        var addToPromotion = new Privilege("ADD_TO_PROMOTION");
        var removeFromPromotion = new Privilege("REMOVE_FROM_PROMOTION");
        var addToRegistryBook = new Privilege("ADD_TO_REGISTRY_BOOK");
        var updateRegistryBook = new Privilege("UPDATE_REGISTRY_BOOK");
        var removeFromRegistryBook = new Privilege("REMOVE_FROM_REGISTRY_BOOK");
        var managePromotions = new Privilege("MANAGE_PROMOTIONS");
        var generatePromotionReport = new Privilege("GENERATE_PROMOTION_REPORT");
        var allowRegEntrySingleUpdate = new Privilege("ALLOW_REG_ENTRY_SINGLE_UPDATE");
        var generateRegBookReport = new Privilege("GENERATE_REG_BOOK_REPORT");
        var performMigration = new Privilege("PERFORM_MIGRATION");
        var createJournal = new Privilege("CREATE_JOURNAL");
        var generateThesisLibraryBackup = new Privilege("GENERATE_THESIS_LIBRARY_BACKUP");
        var generateOutputBackup = new Privilege("GENERATE_OUTPUT_BACKUP");
        var performHealthCheck = new Privilege("PERFORM_HEALTH_CHECK");
        var deleteUserAccount = new Privilege("DELETE_USER_ACCOUNT");
        var generateNewEmployeePassword = new Privilege("GENERATE_NEW_EMPLOYEE_PASSWORD");
        var saveLoadingConfiguration = new Privilege("SAVE_LOADING_CONFIG");
        var performLoading = new Privilege("PERFORM_IMPORT_AND_LOADING");
        var editExternalIndicatorConfiguration = new Privilege("EDIT_EXT_INDICATOR_CONFIGURATION");
        var harvestIdfMetadata = new Privilege("HARVEST_IDF_METADATA");
        var addSubUnit = new Privilege("ADD_SUB_UNIT");
        var deleteOrganisationUnit = new Privilege("DELETE_ORGANISATION_UNITS");
        var saveOUPageConfiguration = new Privilege("SAVE_OU_PAGE_CONFIGURATION");
        var migrateAllEntities = new Privilege("MIGRATE_ALL_ENTITIES");
        var migrateInstitutionEntities = new Privilege("MIGRATE_INSTITUTION_ENTITIES");
        var performOaiMigration = new Privilege("PERFORM_OAI_MIGRATION");
        var saveOUTrustConfiguration = new Privilege("SAVE_OU_TRUST_CONFIGURATION");
        var validateMetadata = new Privilege("VALIDATE_METADATA");
        var validateUploadedFiles = new Privilege("VALIDATE_UPLOADED_FILES");
        var archiveDocument = new Privilege("ARCHIVE_DOCUMENT");
        var promotePreliminaryAttachments = new Privilege("PROMOTE_PRELIMINARY_ATTACHMENTS");
        var scheduleDocumentHarvest = new Privilege("SCHEDULE_DOCUMENT_HARVEST");
        var configureHarvestSources = new Privilege("CONFIGURE_HARVEST_SOURCES");
        var performOAIPMHHarvest = new Privilege("PERFORM_OAIPMH_HARVEST");
        var setDefaultContent = new Privilege("SET_DEFAULT_CONTENT");
        var saveOUOutputConfiguration = new Privilege("SAVE_OU_OUTPUT_CONFIGURATION");
        var createBookSeries = new Privilege("CREATE_BOOK_SERIES");
        var getTopCollaborators = new Privilege("GET_TOP_COLLABORATORS");
        var performExtraMigration = new Privilege("PERFORM_EXTRA_MIGRATION_OPERATIONS");
        var readRegistryBook = new Privilege("READ_REGISTRY_BOOK");
        var saveChartDisplayConfiguration = new Privilege("SAVE_CHART_DISPLAY_CONFIGURATION");
        var readDigitalLibraryAnalytics = new Privilege("READ_DIGITAL_LIBRARY_ANALYTICS");
        var forceEmailChange = new Privilege("FORCE_EMAIL_CHANGE");
        var performSKGIFHarvest = new Privilege("PERFORM_SKGIF_HARVEST");
        var readPromotions = new Privilege("READ_PROMOTIONS");
        var configureAppSettings = new Privilege("CONFIGURE_APP_SETTINGS");
        var registerResearcher = new Privilege("REGISTER_RESEARCHER");
        var unpromoteRbEntries = new Privilege("UNPROMOTE_RB_ENTRIES");
        var changePublicationType = new Privilege("CHANGE_PUBLICATION_TYPE");
        var assessPrizes = new Privilege("ASSESS_PRIZE");
        var editExhibitions = new Privilege("EDIT_EXHIBITIONS");
        var createExhibitions = new Privilege("CREATE_EXHIBITIONS");
        var mergeExhibitions = new Privilege("MERGE_EXHIBITIONS");
        var substituteThesis = new Privilege("SUBSTITUTE_THESIS");
        var setPersonFieldVisibility = new Privilege("SET_PERSON_FIELD_VISIBILITY");
        var enrichDocumentMetadata = new Privilege("ENRICH_DOCUMENT_METADATA");
        var enrichInstitutionMetadata = new Privilege("ENRICH_INSTITUTION_METADATA");
        var scheduleMetadataEnrichment = new Privilege("SCHEDULE_METADATA_ENRICHMENT");
        var readFundingPrograms = new Privilege("READ_FUNDING_PROGRAMS");
        var editFundingPrograms = new Privilege("EDIT_FUNDING_PROGRAMS");
        var readFundingCalls = new Privilege("READ_FUNDING_CALLS");
        var editFundingCalls = new Privilege("EDIT_FUNDING_CALLS");
        var editFundingParts = new Privilege("EDIT_FUNDING_PARTS");
        var editEmploymentPositions = new Privilege("EDIT_EMPLOYMENT_POSITIONS");
        var editCourses = new Privilege("EDIT_COURSES");
        var createCourses = new Privilege("CREATE_COURSES");
        var createOtherEvents = new Privilege("CREATE_OTHER_EVENTS");
        var editOtherEvents = new Privilege("EDIT_OTHER_EVENTS");
        var mergeCourses = new Privilege("MERGE_COURSES");
        var mergeOtherEvents = new Privilege("MERGE_OTHER_EVENTS");
        var editIdentifiers = new Privilege("EDIT_IDENTIFIERS");
        var editEntityIdentifier = new Privilege("EDIT_ENTITY_IDENTIFIER");
        var editEventIdentifiers = new Privilege("EDIT_EVENT_IDENTIFIERS");
        var editPersonIdentifiers = new Privilege("EDIT_PERSON_IDENTIFIERS");
        var editDocumentIdentifiers = new Privilege("EDIT_DOCUMENT_IDENTIFIERS");
        var editOrganisationUnitIdentifiers = new Privilege("EDIT_ORGANISATION_UNIT_IDENTIFIERS");
        var editPublicationSeriesIdentifiers = new Privilege("EDIT_PUBLICATION_SERIES_IDENTIFIERS");
        var readFundingApplications = new Privilege("READ_FUNDING_APPLICATIONS");
        var editFundingApplications = new Privilege("EDIT_FUNDING_APPLICATIONS");
        var readFunding = new Privilege("READ_FUNDING");
        var editFunding = new Privilege("EDIT_FUNDING");
        var readProjects = new Privilege("READ_PROJECTS");
        var editProjects = new Privilege("EDIT_PROJECTS");
        var deleteProjects = new Privilege("DELETE_PROJECTS");
        var unbindYourselfFromProject = new Privilege("UNBIND_YOURSELF_FROM_PROJECT");
        var unbindEmployeesFromProject = new Privilege("UNBIND_EMPLOYEES_FROM_PROJECT");
        var restoreEntityRevision = new Privilege("RESTORE_ENTITY_REVISION");
        var backfillEntityRevision = new Privilege("BACKFILL_ENTITY_REVISION");
        var assessDataQuality = new Privilege("ASSESS_DATA_QUALITY");
        var saveModuleAccessConfiguration = new Privilege("SAVE_MODULE_ACCESS_CONFIGURATION");
        var manageDataQualityFixators = new Privilege("MANAGE_DATA_QUALITY_FIXATORS");

        privilegeRepository.saveAll(
            Arrays.asList(allowAccountTakeover, takeRoleOfUser, deactivateUser, updateProfile,
                createUserBasic, editPersonalInfo, approvePerson, editDocumentFiles,
                editOrganisationUnit, editResearchAreas, approvePublication, editOURelations,
                editPublishers, editPublicationSeries, editConferences, editEventRelations,
                mergeOUEmployments, mergeJournalPublications, mergePersonPublications,
                mergePersonMetadata, mergeConferenceProceedings, mergeProceedingsPublications,
                prepareExportData, editIndicators, editAssessmentClassifications, editCommissions,
                editAssessmentMeasures, editAssessmentRulebooks, editDocumentIndicators,
                editEntityIndicatorProofs, listMyJournalPublications, deletePerson, scheduleTask,
                registerEmployee, reindexPrivilege, startDeduplicationProcess, performDeduplication,
                mergeDocumentsMetadata, mergeEventMetadata, mergePublicationSeriesMetadata,
                mergeMonographPublications, prepareExportData, mergeBookSeriesPublications,
                mergeOUMetadata, editCountries, forceDelete, switchEntityToUnmanaged,
                claimDocument, mergePublisherPublications, mergePublishersMetadata,
                unbindYourselfFromPublication, editEntityIndicators, editLanguageTags,
                editEntityAssessmentClassifications, editEventIndicators, editPubSeriesIndicators,
                editEventAssessmentClassification, editPublicationSeriesAssessmentClassifications,
                assessDocument, updateCommission, editDocumentAssessment, scheduleReportGeneration,
                editAssessmentResearchArea, downloadReports, listAssessmentClassifications,
                updateBrandingInformation, manageApiKeys, manageThesisAttachments, createJournal,
                editEmploymentInstitution, archiveThesis, unarchiveThesis, performThesisReport,
                putThesisOnPublicReview, deleteThesisAttachments, removeThesisFromPublicReview,
                addToPromotion, removeFromPromotion, addToRegistryBook, deleteUserAccount,
                removeFromRegistryBook, updateRegistryBook, managePromotions, performMigration,
                generatePromotionReport, allowRegEntrySingleUpdate, generateRegBookReport,
                generateThesisLibraryBackup, generateOutputBackup, performHealthCheck, addSubUnit,
                generateNewEmployeePassword, createConference, createPublisher, harvestIdfMetadata,
                saveLoadingConfiguration, performLoading, editExternalIndicatorConfiguration,
                deleteOrganisationUnit, saveOUPageConfiguration, migrateAllEntities,
                migrateInstitutionEntities, performOaiMigration, saveOUTrustConfiguration,
                validateMetadata, validateUploadedFiles, archiveDocument, configureHarvestSources,
                promotePreliminaryAttachments, scheduleDocumentHarvest, performOAIPMHHarvest,
                setDefaultContent, saveOUOutputConfiguration, createBookSeries, readRegistryBook,
                unbindEmployeesFromPublication, getTopCollaborators, performExtraMigration,
                saveChartDisplayConfiguration, readDigitalLibraryAnalytics, forceEmailChange,
                performSKGIFHarvest, readPromotions, configureAppSettings, registerResearcher,
                unpromoteRbEntries, changePublicationType, assessPrizes, editExhibitions,
                createExhibitions, mergeExhibitions, substituteThesis, setPersonFieldVisibility,
                enrichDocumentMetadata, enrichInstitutionMetadata, scheduleMetadataEnrichment,
                editFundingPrograms, readFundingPrograms, readFundingCalls, editFundingCalls,
                editFundingParts, editEmploymentPositions, createCourses, editCourses,
                createOtherEvents, editOtherEvents, mergeCourses, mergeOtherEvents,
                editIdentifiers, editEntityIdentifier, editEventIdentifiers, editPersonIdentifiers,
                editDocumentIdentifiers, editOrganisationUnitIdentifiers,
                editPublicationSeriesIdentifiers, readFundingApplications,
                editFundingApplications, readFunding, editFunding, readProjects, editProjects,
                deleteProjects,
                unbindYourselfFromProject, unbindEmployeesFromProject,
                restoreEntityRevision, backfillEntityRevision, assessDataQuality,
                saveModuleAccessConfiguration, manageDataQualityFixators
            ));

        return new Privileges(
            allowAccountTakeover, takeRoleOfUser, deactivateUser, updateProfile,
            createUserBasic, editPersonalInfo, approvePerson, editDocumentFiles,
            approvePublication, editResearchAreas, editOrganisationUnit,
            editEmploymentInstitution, editOURelations, editPublishers,
            createPublisher, editPublicationSeries, editConferences, createConference,
            editEventRelations, mergeJournalPublications, mergePersonPublications,
            mergePersonMetadata, mergeOUEmployments, mergeOUMetadata,
            mergeConferenceProceedings, mergeProceedingsPublications,
            startDeduplicationProcess, performDeduplication, mergeDocumentsMetadata,
            mergeEventMetadata, mergePublicationSeriesMetadata,
            mergeMonographPublications, prepareExportData, editIndicators,
            editAssessmentClassifications, editAssessmentMeasures,
            editAssessmentRulebooks, editDocumentIndicators, editCommissions,
            updateCommission, editEntityIndicatorProofs, editEntityIndicators,
            listMyJournalPublications, deletePerson, registerEmployee,
            reindexPrivilege, mergeBookSeriesPublications, editCountries, forceDelete,
            switchEntityToUnmanaged, claimDocument, mergePublisherPublications,
            mergePublishersMetadata, unbindYourselfFromPublication,
            unbindEmployeesFromPublication, editEntityAssessmentClassifications,
            editEventAssessmentClassification, editLanguageTags, editEventIndicators,
            scheduleTask, editPublicationSeriesAssessmentClassifications,
            editPubSeriesIndicators, assessDocument, editDocumentAssessment,
            editAssessmentResearchArea, scheduleReportGeneration, downloadReports,
            listAssessmentClassifications, updateBrandingInformation, manageApiKeys,
            manageThesisAttachments, deleteThesisAttachments, putThesisOnPublicReview,
            removeThesisFromPublicReview, archiveThesis, unarchiveThesis,
            performThesisReport, addToPromotion, removeFromPromotion,
            addToRegistryBook, updateRegistryBook, removeFromRegistryBook,
            managePromotions, generatePromotionReport, allowRegEntrySingleUpdate,
            generateRegBookReport, performMigration, createJournal,
            generateThesisLibraryBackup, generateOutputBackup, performHealthCheck,
            deleteUserAccount, generateNewEmployeePassword, saveLoadingConfiguration,
            performLoading, editExternalIndicatorConfiguration, harvestIdfMetadata,
            addSubUnit, deleteOrganisationUnit, saveOUPageConfiguration,
            migrateAllEntities, migrateInstitutionEntities, performOaiMigration,
            saveOUTrustConfiguration, validateMetadata, validateUploadedFiles,
            archiveDocument, promotePreliminaryAttachments, scheduleDocumentHarvest,
            configureHarvestSources, performOAIPMHHarvest, setDefaultContent,
            saveOUOutputConfiguration, createBookSeries, getTopCollaborators,
            performExtraMigration, readRegistryBook, saveChartDisplayConfiguration,
            readDigitalLibraryAnalytics, forceEmailChange, performSKGIFHarvest,
            readPromotions, configureAppSettings, registerResearcher,
            unpromoteRbEntries, changePublicationType, assessPrizes, editExhibitions,
            createExhibitions, mergeExhibitions, substituteThesis,
            setPersonFieldVisibility, enrichDocumentMetadata,
            enrichInstitutionMetadata, scheduleMetadataEnrichment,
            readFundingPrograms, editFundingPrograms, readFundingCalls,
            editFundingCalls, editFundingParts, editEmploymentPositions, editCourses,
            createCourses, createOtherEvents, editOtherEvents, mergeCourses,
            mergeOtherEvents, editIdentifiers, editEntityIdentifier,
            editEventIdentifiers, editPersonIdentifiers, editDocumentIdentifiers,
            editOrganisationUnitIdentifiers, editPublicationSeriesIdentifiers,
            readFundingApplications, editFundingApplications, readFunding,
            editFunding, readProjects, editProjects, deleteProjects,
            unbindYourselfFromProject, unbindEmployeesFromProject,
            restoreEntityRevision, backfillEntityRevision, assessDataQuality,
            saveModuleAccessConfiguration, manageDataQualityFixators);
    }

    private Authority adminAuthority(Privileges p) {
        return new Authority(UserRole.ADMIN.toString(), new HashSet<>(
            List.of(p.takeRoleOfUser(), p.deactivateUser(), p.updateProfile(), p.editPersonalInfo(),
                p.createUserBasic(), p.approvePerson(), p.editDocumentFiles(),
                p.editOrganisationUnit(),
                p.editResearchAreas(), p.editOURelations(), p.approvePublication(),
                p.editPublishers(),
                p.editPublicationSeries(), p.editConferences(), p.editEventRelations(),
                p.editCommissions(),
                p.mergeJournalPublications(), p.mergePersonPublications(), p.mergePersonMetadata(),
                p.mergeOUEmployments(), p.mergeConferenceProceedings(),
                p.mergeProceedingsPublications(),
                p.prepareExportData(), p.editIndicators(), p.editAssessmentClassifications(),
                p.scheduleTask(),
                p.editAssessmentMeasures(), p.editAssessmentRulebooks(), p.editDocumentIndicators(),
                p.editEntityIndicatorProofs(), p.deletePerson(), p.registerEmployee(),
                p.reindexPrivilege(),
                p.startDeduplicationProcess(), p.performDeduplication(), p.mergeDocumentsMetadata(),
                p.mergeEventMetadata(), p.mergePublicationSeriesMetadata(),
                p.mergeMonographPublications(),
                p.prepareExportData(), p.mergeBookSeriesPublications(), p.mergeOUMetadata(),
                p.editCountries(),
                p.forceDelete(), p.switchEntityToUnmanaged(), p.mergePublisherPublications(),
                p.editLanguageTags(),
                p.mergePublishersMetadata(), p.editEntityIndicators(),
                p.editEntityAssessmentClassifications(),
                p.editEventIndicators(), p.editEventAssessmentClassification(),
                p.editPubSeriesIndicators(),
                p.editPublicationSeriesAssessmentClassifications(), p.assessDocument(),
                p.updateCommission(),
                p.editDocumentAssessment(), p.editAssessmentResearchArea(),
                p.scheduleReportGeneration(),
                p.downloadReports(), p.listAssessmentClassifications(),
                p.updateBrandingInformation(),
                p.manageApiKeys(), p.manageThesisAttachments(), p.putThesisOnPublicReview(),
                p.managePromotions(),
                p.deleteThesisAttachments(), p.removeThesisFromPublicReview(), p.archiveThesis(),
                p.unarchiveThesis(), p.performThesisReport(), p.addToPromotion(),
                p.generateNewEmployeePassword(),
                p.removeFromPromotion(), p.addToRegistryBook(), p.updateRegistryBook(),
                p.removeFromRegistryBook(),
                p.generatePromotionReport(), p.allowRegEntrySingleUpdate(),
                p.generateRegBookReport(),
                p.performMigration(), p.createJournal(), p.generateThesisLibraryBackup(),
                p.generateOutputBackup(),
                p.performHealthCheck(), p.deleteUserAccount(), p.createConference(),
                p.createPublisher(),
                p.saveLoadingConfiguration(), p.performLoading(),
                p.editExternalIndicatorConfiguration(),
                p.harvestIdfMetadata(), p.addSubUnit(), p.deleteOrganisationUnit(),
                p.saveOUPageConfiguration(),
                p.migrateAllEntities(), p.performOaiMigration(), p.saveOUTrustConfiguration(),
                p.validateMetadata(),
                p.validateUploadedFiles(), p.archiveDocument(), p.promotePreliminaryAttachments(),
                p.scheduleDocumentHarvest(), p.configureHarvestSources(), p.performOAIPMHHarvest(),
                p.setDefaultContent(), p.saveOUOutputConfiguration(), p.createBookSeries(),
                p.readRegistryBook(),
                p.performExtraMigration(), p.saveChartDisplayConfiguration(),
                p.readDigitalLibraryAnalytics(),
                p.forceEmailChange(), p.performSKGIFHarvest(), p.configureAppSettings(),
                p.getTopCollaborators(),
                p.registerResearcher(), p.unpromoteRbEntries(), p.changePublicationType(),
                p.assessPrizes(),
                p.editExhibitions(), p.createExhibitions(), p.mergeExhibitions(),
                p.substituteThesis(),
                p.setPersonFieldVisibility(), p.enrichDocumentMetadata(),
                p.enrichInstitutionMetadata(),
                p.scheduleMetadataEnrichment(), p.editFundingPrograms(), p.readFundingPrograms(),
                p.readFundingCalls(), p.editFundingCalls(), p.editFundingParts(),
                p.editEmploymentPositions(),
                p.createCourses(), p.editCourses(), p.createOtherEvents(), p.editOtherEvents(),
                p.mergeCourses(),
                p.mergeOtherEvents(), p.editIdentifiers(), p.editEntityIdentifier(),
                p.editEventIdentifiers(),
                p.editPersonIdentifiers(), p.editDocumentIdentifiers(),
                p.editOrganisationUnitIdentifiers(),
                p.editPublicationSeriesIdentifiers(), p.readFundingApplications(),
                p.editFundingApplications(), p.readFunding(), p.editFunding(), p.readProjects(),
                p.editProjects(),
                p.deleteProjects(),
                p.restoreEntityRevision(), p.backfillEntityRevision(), p.assessDataQuality(),
                p.saveModuleAccessConfiguration(), p.manageDataQualityFixators()
            )));
    }

    private Authority researcherAuthority(Privileges p) {
        return new Authority(UserRole.RESEARCHER.toString(), new HashSet<>(
            List.of(p.allowAccountTakeover(), p.updateProfile(), p.editPersonalInfo(),
                p.assessDocument(),
                p.editDocumentFiles(), p.editDocumentIndicators(), p.claimDocument(),
                p.createConference(),
                p.editEntityIndicatorProofs(), p.listMyJournalPublications(),
                p.editAssessmentResearchArea(),
                p.unbindYourselfFromPublication(), p.editEntityIndicators(), p.createJournal(),
                p.createBookSeries(), p.createPublisher(), p.performLoading(),
                p.harvestIdfMetadata(),
                p.getTopCollaborators(), p.changePublicationType(), p.createExhibitions(),
                p.setPersonFieldVisibility(), p.enrichDocumentMetadata(), p.readProjects(),
                p.editProjects(),
                p.unbindYourselfFromProject())));
    }

    private Authority institutionalEditorAuthority(Privileges p) {
        return new Authority(UserRole.INSTITUTIONAL_EDITOR.toString(), new HashSet<>(
            List.of(
                p.updateProfile(), p.allowAccountTakeover(), p.manageThesisAttachments(),
                p.putThesisOnPublicReview(), p.createUserBasic(), p.editPersonalInfo(),
                p.createJournal(),
                p.editDocumentFiles(), p.editEmploymentInstitution(), p.generateOutputBackup(),
                p.createConference(), p.saveLoadingConfiguration(), p.performLoading(),
                p.editExternalIndicatorConfiguration(), p.harvestIdfMetadata(), p.addSubUnit(),
                p.mergePersonPublications(), p.mergePersonMetadata(), p.mergeOUMetadata(),
                p.mergeOUEmployments(), p.mergeDocumentsMetadata(), p.deletePerson(),
                p.validateMetadata(),
                p.deleteOrganisationUnit(), p.saveOUPageConfiguration(),
                p.migrateInstitutionEntities(),
                p.saveOUTrustConfiguration(), p.validateUploadedFiles(), p.archiveDocument(),
                p.scheduleDocumentHarvest(), p.configureHarvestSources(), p.setDefaultContent(),
                p.saveOUOutputConfiguration(), p.createBookSeries(),
                p.unbindEmployeesFromPublication(),
                p.saveChartDisplayConfiguration(), p.getTopCollaborators(),
                p.changePublicationType(),
                p.createExhibitions(), p.enrichDocumentMetadata(), p.enrichInstitutionMetadata(),
                p.assessDataQuality(), p.readProjects(), p.editProjects(),
                p.unbindEmployeesFromProject())));
    }

    private Authority commissionAuthority(Privileges p) {
        return new Authority(UserRole.COMMISSION.toString(), new HashSet<>(List.of(
            p.editEventAssessmentClassification(), p.updateProfile(), p.editEventIndicators(),
            p.editPublicationSeriesAssessmentClassifications(), p.editPubSeriesIndicators(),
            p.allowAccountTakeover(), p.editEntityIndicatorProofs(), p.updateCommission(),
            p.editDocumentAssessment(), p.listAssessmentClassifications(), p.assessPrizes()
        )));
    }

    private Authority viceDeanForScienceAuthority(Privileges p) {
        return new Authority(UserRole.VICE_DEAN_FOR_SCIENCE.toString(), new HashSet<>(List.of(
            p.updateProfile(), p.allowAccountTakeover(), p.scheduleReportGeneration(),
            p.downloadReports(),
            p.assessDataQuality()
        )));
    }

    private Authority institutionalLibrarianAuthority(Privileges p) {
        return new Authority(UserRole.INSTITUTIONAL_LIBRARIAN.toString(), new HashSet<>(List.of(
            p.updateProfile(), p.allowAccountTakeover(), p.manageThesisAttachments(),
            p.putThesisOnPublicReview(), p.editDocumentFiles(), p.archiveThesis(),
            p.addToRegistryBook(), p.generateThesisLibraryBackup(), p.harvestIdfMetadata(),
            p.validateMetadata(), p.validateUploadedFiles(), p.promotePreliminaryAttachments(),
            p.setDefaultContent(), p.createUserBasic(), p.deleteThesisAttachments(),
            p.readRegistryBook(),
            p.readDigitalLibraryAnalytics(), p.readPromotions(), p.substituteThesis()
        )));
    }

    private Authority headOfLibraryAuthority(Privileges p) {
        return new Authority(UserRole.HEAD_OF_LIBRARY.toString(), new HashSet<>(List.of(
            p.updateProfile(), p.allowAccountTakeover(), p.deleteThesisAttachments(),
            p.editDocumentFiles(),
            p.removeThesisFromPublicReview(), p.putThesisOnPublicReview(),
            p.manageThesisAttachments(),
            p.unarchiveThesis(), p.performThesisReport(), p.generateThesisLibraryBackup(),
            p.readRegistryBook(),
            p.readDigitalLibraryAnalytics(), p.readPromotions(), p.archiveThesis(),
            p.substituteThesis(),
            p.createUserBasic()
        )));
    }

    private Authority promotionRegistryAdministratorAuthority(Privileges p) {
        return new Authority(UserRole.PROMOTION_REGISTRY_ADMINISTRATOR.toString(),
            new HashSet<>(List.of(
                p.updateProfile(), p.allowAccountTakeover(), p.addToPromotion(),
                p.removeFromPromotion(),
                p.updateRegistryBook(), p.managePromotions(), p.generatePromotionReport(),
                p.generateRegBookReport(), p.readRegistryBook(), p.allowRegEntrySingleUpdate()
            )));
    }

    public record SeededAuthorities(
        Authority admin,
        Authority researcher,
        Authority institutionalEditor,
        Authority commission,
        Authority viceDeanForScience,
        Authority institutionalLibrarian,
        Authority headOfLibrary,
        Authority promotionRegistryAdministrator
    ) {
    }

    record Privileges(
        Privilege allowAccountTakeover,
        Privilege takeRoleOfUser,
        Privilege deactivateUser,
        Privilege updateProfile,
        Privilege createUserBasic,
        Privilege editPersonalInfo,
        Privilege approvePerson,
        Privilege editDocumentFiles,
        Privilege approvePublication,
        Privilege editResearchAreas,
        Privilege editOrganisationUnit,
        Privilege editEmploymentInstitution,
        Privilege editOURelations,
        Privilege editPublishers,
        Privilege createPublisher,
        Privilege editPublicationSeries,
        Privilege editConferences,
        Privilege createConference,
        Privilege editEventRelations,
        Privilege mergeJournalPublications,
        Privilege mergePersonPublications,
        Privilege mergePersonMetadata,
        Privilege mergeOUEmployments,
        Privilege mergeOUMetadata,
        Privilege mergeConferenceProceedings,
        Privilege mergeProceedingsPublications,
        Privilege startDeduplicationProcess,
        Privilege performDeduplication,
        Privilege mergeDocumentsMetadata,
        Privilege mergeEventMetadata,
        Privilege mergePublicationSeriesMetadata,
        Privilege mergeMonographPublications,
        Privilege prepareExportData,
        Privilege editIndicators,
        Privilege editAssessmentClassifications,
        Privilege editAssessmentMeasures,
        Privilege editAssessmentRulebooks,
        Privilege editDocumentIndicators,
        Privilege editCommissions,
        Privilege updateCommission,
        Privilege editEntityIndicatorProofs,
        Privilege editEntityIndicators,
        Privilege listMyJournalPublications,
        Privilege deletePerson,
        Privilege registerEmployee,
        Privilege reindexPrivilege,
        Privilege mergeBookSeriesPublications,
        Privilege editCountries,
        Privilege forceDelete,
        Privilege switchEntityToUnmanaged,
        Privilege claimDocument,
        Privilege mergePublisherPublications,
        Privilege mergePublishersMetadata,
        Privilege unbindYourselfFromPublication,
        Privilege unbindEmployeesFromPublication,
        Privilege editEntityAssessmentClassifications,
        Privilege editEventAssessmentClassification,
        Privilege editLanguageTags,
        Privilege editEventIndicators,
        Privilege scheduleTask,
        Privilege editPublicationSeriesAssessmentClassifications,
        Privilege editPubSeriesIndicators,
        Privilege assessDocument,
        Privilege editDocumentAssessment,
        Privilege editAssessmentResearchArea,
        Privilege scheduleReportGeneration,
        Privilege downloadReports,
        Privilege listAssessmentClassifications,
        Privilege updateBrandingInformation,
        Privilege manageApiKeys,
        Privilege manageThesisAttachments,
        Privilege deleteThesisAttachments,
        Privilege putThesisOnPublicReview,
        Privilege removeThesisFromPublicReview,
        Privilege archiveThesis,
        Privilege unarchiveThesis,
        Privilege performThesisReport,
        Privilege addToPromotion,
        Privilege removeFromPromotion,
        Privilege addToRegistryBook,
        Privilege updateRegistryBook,
        Privilege removeFromRegistryBook,
        Privilege managePromotions,
        Privilege generatePromotionReport,
        Privilege allowRegEntrySingleUpdate,
        Privilege generateRegBookReport,
        Privilege performMigration,
        Privilege createJournal,
        Privilege generateThesisLibraryBackup,
        Privilege generateOutputBackup,
        Privilege performHealthCheck,
        Privilege deleteUserAccount,
        Privilege generateNewEmployeePassword,
        Privilege saveLoadingConfiguration,
        Privilege performLoading,
        Privilege editExternalIndicatorConfiguration,
        Privilege harvestIdfMetadata,
        Privilege addSubUnit,
        Privilege deleteOrganisationUnit,
        Privilege saveOUPageConfiguration,
        Privilege migrateAllEntities,
        Privilege migrateInstitutionEntities,
        Privilege performOaiMigration,
        Privilege saveOUTrustConfiguration,
        Privilege validateMetadata,
        Privilege validateUploadedFiles,
        Privilege archiveDocument,
        Privilege promotePreliminaryAttachments,
        Privilege scheduleDocumentHarvest,
        Privilege configureHarvestSources,
        Privilege performOAIPMHHarvest,
        Privilege setDefaultContent,
        Privilege saveOUOutputConfiguration,
        Privilege createBookSeries,
        Privilege getTopCollaborators,
        Privilege performExtraMigration,
        Privilege readRegistryBook,
        Privilege saveChartDisplayConfiguration,
        Privilege readDigitalLibraryAnalytics,
        Privilege forceEmailChange,
        Privilege performSKGIFHarvest,
        Privilege readPromotions,
        Privilege configureAppSettings,
        Privilege registerResearcher,
        Privilege unpromoteRbEntries,
        Privilege changePublicationType,
        Privilege assessPrizes,
        Privilege editExhibitions,
        Privilege createExhibitions,
        Privilege mergeExhibitions,
        Privilege substituteThesis,
        Privilege setPersonFieldVisibility,
        Privilege enrichDocumentMetadata,
        Privilege enrichInstitutionMetadata,
        Privilege scheduleMetadataEnrichment,
        Privilege readFundingPrograms,
        Privilege editFundingPrograms,
        Privilege readFundingCalls,
        Privilege editFundingCalls,
        Privilege editFundingParts,
        Privilege editEmploymentPositions,
        Privilege editCourses,
        Privilege createCourses,
        Privilege createOtherEvents,
        Privilege editOtherEvents,
        Privilege mergeCourses,
        Privilege mergeOtherEvents,
        Privilege editIdentifiers,
        Privilege editEntityIdentifier,
        Privilege editEventIdentifiers,
        Privilege editPersonIdentifiers,
        Privilege editDocumentIdentifiers,
        Privilege editOrganisationUnitIdentifiers,
        Privilege editPublicationSeriesIdentifiers,
        Privilege readFundingApplications,
        Privilege editFundingApplications,
        Privilege readFunding,
        Privilege editFunding,
        Privilege readProjects,
        Privilege editProjects,
        Privilege deleteProjects,
        Privilege unbindYourselfFromProject,
        Privilege unbindEmployeesFromProject,
        Privilege restoreEntityRevision,
        Privilege backfillEntityRevision,
        Privilege assessDataQuality,
        Privilege saveModuleAccessConfiguration,
        Privilege manageDataQualityFixators
    ) {
    }
}
