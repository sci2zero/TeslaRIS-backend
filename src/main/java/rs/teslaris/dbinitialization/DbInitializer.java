package rs.teslaris.dbinitialization;


import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.context.annotation.Profile;
import org.springframework.core.env.Environment;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;
import rs.teslaris.core.model.commontypes.BrandingInformation;
import rs.teslaris.core.model.commontypes.Country;
import rs.teslaris.core.model.commontypes.Language;
import rs.teslaris.core.model.commontypes.LanguageTag;
import rs.teslaris.core.model.commontypes.MultiLingualContent;
import rs.teslaris.core.model.commontypes.ResearchArea;
import rs.teslaris.core.model.user.Authority;
import rs.teslaris.core.model.user.Privilege;
import rs.teslaris.core.model.user.User;
import rs.teslaris.core.model.user.UserNotificationPeriod;
import rs.teslaris.core.model.user.UserRole;
import rs.teslaris.core.repository.commontypes.BrandingInformationRepository;
import rs.teslaris.core.repository.commontypes.CountryRepository;
import rs.teslaris.core.repository.commontypes.LanguageRepository;
import rs.teslaris.core.repository.commontypes.LanguageTagRepository;
import rs.teslaris.core.repository.commontypes.ResearchAreaRepository;
import rs.teslaris.core.repository.user.AuthorityRepository;
import rs.teslaris.core.repository.user.PrivilegeRepository;
import rs.teslaris.core.repository.user.UserRepository;
import rs.teslaris.core.util.language.LanguageAbbreviations;
import rs.teslaris.core.util.search.StringUtil;
import rs.teslaris.core.util.seeding.CsvDataLoader;
import rs.teslaris.core.util.seeding.SKOSLoader;

@Component
@RequiredArgsConstructor
@Slf4j
@Profile("test")
public class DbInitializer implements ApplicationRunner {

    private final AuthorityRepository authorityRepository;

    private final PrivilegeRepository privilegeRepository;

    private final UserRepository userRepository;

    private final LanguageRepository languageRepository;

    private final LanguageTagRepository languageTagRepository;

    private final PasswordEncoder passwordEncoder;

    private final CountryRepository countryRepository;

    private final ResearchAreaRepository researchAreaRepository;

    private final CsvDataLoader csvDataLoader;

    private final SKOSLoader skosLoader;

    private final Environment environment;

    private final TestingDataInitializer testingDataInitializer;

    private final AssessmentDataInitializer assessmentDataInitializer;

    private final BrandingInformationRepository brandingInformationRepository;


    @Override
    @Transactional
    public void run(ApplicationArguments args) throws Exception {
        ///////////////////// NECESSARY DATA /////////////////////

        // PRIVILEGES
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
                validateMetadata, validateUploadedFiles, archiveDocument,
                promotePreliminaryAttachments, scheduleDocumentHarvest));

        // AUTHORITIES
        var adminAuthority = new Authority(UserRole.ADMIN.toString(), new HashSet<>(
            List.of(takeRoleOfUser, deactivateUser, updateProfile, editPersonalInfo,
                createUserBasic, approvePerson, editDocumentFiles, editOrganisationUnit,
                editResearchAreas, editOURelations, approvePublication, editPublishers,
                editPublicationSeries, editConferences, editEventRelations, editCommissions,
                mergeJournalPublications, mergePersonPublications, mergePersonMetadata,
                mergeOUEmployments, mergeConferenceProceedings, mergeProceedingsPublications,
                prepareExportData, editIndicators, editAssessmentClassifications, scheduleTask,
                editAssessmentMeasures, editAssessmentRulebooks, editDocumentIndicators,
                editEntityIndicatorProofs, deletePerson, registerEmployee, reindexPrivilege,
                startDeduplicationProcess, performDeduplication, mergeDocumentsMetadata,
                mergeEventMetadata, mergePublicationSeriesMetadata, mergeMonographPublications,
                prepareExportData, mergeBookSeriesPublications, mergeOUMetadata, editCountries,
                forceDelete, switchEntityToUnmanaged, mergePublisherPublications, editLanguageTags,
                mergePublishersMetadata, editEntityIndicators, editEntityAssessmentClassifications,
                editEventIndicators, editEventAssessmentClassification, editPubSeriesIndicators,
                editPublicationSeriesAssessmentClassifications, assessDocument, updateCommission,
                editDocumentAssessment, editAssessmentResearchArea, scheduleReportGeneration,
                downloadReports, listAssessmentClassifications, updateBrandingInformation,
                manageApiKeys, manageThesisAttachments, putThesisOnPublicReview, managePromotions,
                deleteThesisAttachments, removeThesisFromPublicReview, archiveThesis,
                unarchiveThesis, performThesisReport, addToPromotion, generateNewEmployeePassword,
                removeFromPromotion, addToRegistryBook, updateRegistryBook, removeFromRegistryBook,
                generatePromotionReport, allowRegEntrySingleUpdate, generateRegBookReport,
                performMigration, createJournal, generateThesisLibraryBackup, generateOutputBackup,
                performHealthCheck, deleteUserAccount, createConference, createPublisher,
                saveLoadingConfiguration, performLoading, editExternalIndicatorConfiguration,
                harvestIdfMetadata, addSubUnit, deleteOrganisationUnit, saveOUPageConfiguration,
                migrateAllEntities, performOaiMigration, saveOUTrustConfiguration, validateMetadata,
                validateUploadedFiles, archiveDocument, promotePreliminaryAttachments,
                scheduleDocumentHarvest
            )));

        var researcherAuthority = new Authority(UserRole.RESEARCHER.toString(), new HashSet<>(
            List.of(allowAccountTakeover, updateProfile, editPersonalInfo, assessDocument,
                editDocumentFiles, editDocumentIndicators, claimDocument, createConference,
                editEntityIndicatorProofs, listMyJournalPublications, editAssessmentResearchArea,
                unbindYourselfFromPublication, editEntityIndicators, createJournal,
                createPublisher, performLoading, harvestIdfMetadata)));

        var institutionalEditorAuthority =
            new Authority(UserRole.INSTITUTIONAL_EDITOR.toString(), new HashSet<>(
                List.of(
                    updateProfile, allowAccountTakeover, manageThesisAttachments,
                    putThesisOnPublicReview, createUserBasic, editPersonalInfo, createJournal,
                    editDocumentFiles, editEmploymentInstitution, generateOutputBackup,
                    createConference, saveLoadingConfiguration, performLoading,
                    editExternalIndicatorConfiguration, harvestIdfMetadata, addSubUnit,
                    mergePersonPublications, mergePersonMetadata, mergeOUMetadata,
                    mergeOUEmployments, mergeDocumentsMetadata, deletePerson, validateMetadata,
                    deleteOrganisationUnit, saveOUPageConfiguration, migrateInstitutionEntities,
                    saveOUTrustConfiguration, validateUploadedFiles, archiveDocument,
                    scheduleDocumentHarvest)));

        var commissionAuthority =
            new Authority(UserRole.COMMISSION.toString(), new HashSet<>(List.of(
                editEventAssessmentClassification, updateProfile, editEventIndicators,
                editPublicationSeriesAssessmentClassifications, editPubSeriesIndicators,
                allowAccountTakeover, editEntityIndicatorProofs, updateCommission,
                editDocumentAssessment, listAssessmentClassifications
            )));

        var viceDeanForScienceAuthority =
            new Authority(UserRole.VICE_DEAN_FOR_SCIENCE.toString(), new HashSet<>(List.of(
                updateProfile, allowAccountTakeover, scheduleReportGeneration, downloadReports
            )));

        var institutionalLibrarianAuthority =
            new Authority(UserRole.INSTITUTIONAL_LIBRARIAN.toString(), new HashSet<>(List.of(
                updateProfile, allowAccountTakeover, manageThesisAttachments,
                putThesisOnPublicReview, editDocumentFiles, archiveThesis,
                addToRegistryBook, generateThesisLibraryBackup, harvestIdfMetadata,
                validateMetadata, validateUploadedFiles, promotePreliminaryAttachments
            )));

        var headOfLibraryAuthority =
            new Authority(UserRole.HEAD_OF_LIBRARY.toString(), new HashSet<>(List.of(
                updateProfile, allowAccountTakeover, deleteThesisAttachments, editDocumentFiles,
                removeThesisFromPublicReview, putThesisOnPublicReview, manageThesisAttachments,
                unarchiveThesis, performThesisReport, generateThesisLibraryBackup
            )));

        var promotionRegistryAdministratorAuthority =
            new Authority(UserRole.PROMOTION_REGISTRY_ADMINISTRATOR.toString(),
                new HashSet<>(List.of(
                    updateProfile, allowAccountTakeover, addToPromotion, removeFromPromotion,
                    updateRegistryBook, managePromotions, generatePromotionReport,
                    generateRegBookReport
                )));

        authorityRepository.saveAll(
            List.of(adminAuthority, researcherAuthority, institutionalEditorAuthority,
                commissionAuthority, viceDeanForScienceAuthority, institutionalLibrarianAuthority,
                headOfLibraryAuthority, promotionRegistryAdministratorAuthority));

        // LANGUAGE TAGS
        var englishTag = new LanguageTag(LanguageAbbreviations.ENGLISH, "English");
        var serbianTag = new LanguageTag(LanguageAbbreviations.SERBIAN, "Srpski");
        var serbianCyrillicTag = new LanguageTag(LanguageAbbreviations.SERBIAN_CYRILLIC, "Српски");
        var hungarianTag = new LanguageTag(LanguageAbbreviations.HUNGARIAN, "Magyar");
        var germanTag = new LanguageTag(LanguageAbbreviations.GERMAN, "Deutsch");
        var frenchTag = new LanguageTag(LanguageAbbreviations.FRENCH, "Français");
        var spanishTag = new LanguageTag(LanguageAbbreviations.SPANISH, "Español");
        var russianTag = new LanguageTag(LanguageAbbreviations.RUSSIAN, "Русский");
        var croatianTag = new LanguageTag(LanguageAbbreviations.CROATIAN, "Croatian");
        var italianTag = new LanguageTag(LanguageAbbreviations.ITALIAN, "Italian");
        var slovenianTag = new LanguageTag(LanguageAbbreviations.SLOVENIAN, "Slovenian");
        languageTagRepository.saveAll(
            List.of(englishTag, serbianTag, hungarianTag, germanTag, frenchTag, spanishTag,
                russianTag, croatianTag, italianTag, slovenianTag, serbianCyrillicTag));

        // LANGUAGES
        var serbianLanguage = new Language();
        serbianLanguage.setLanguageCode(LanguageAbbreviations.SERBIAN);
        serbianLanguage.setName(
            new HashSet<>(List.of(new MultiLingualContent(serbianTag, "Srpski", 1))));
        var englishLanguage = new Language();
        englishLanguage.setLanguageCode(LanguageAbbreviations.ENGLISH);
        englishLanguage.setName(
            new HashSet<>(List.of(new MultiLingualContent(serbianTag, "Engleski", 1))));
        // We will maybe need YU, I don't know
        var yuLanguage = new Language();
        yuLanguage.setLanguageCode(LanguageAbbreviations.YUGOSLAV);
        yuLanguage.setDeleted(true);
        var germanLanguage = new Language();
        germanLanguage.setLanguageCode(LanguageAbbreviations.GERMAN);
        germanLanguage.setName(
            new HashSet<>(List.of(new MultiLingualContent(serbianTag, "Nemački", 1))));
        var frenchLanguage = new Language();
        frenchLanguage.setLanguageCode(LanguageAbbreviations.FRENCH);
        frenchLanguage.setName(
            new HashSet<>(List.of(new MultiLingualContent(serbianTag, "Francuski", 1))));
        var spanishLanguage = new Language();
        spanishLanguage.setLanguageCode(LanguageAbbreviations.SPANISH);
        spanishLanguage.setName(
            new HashSet<>(List.of(new MultiLingualContent(serbianTag, "Španski", 1))));
        var russianLanguage = new Language();
        russianLanguage.setLanguageCode(LanguageAbbreviations.RUSSIAN);
        russianLanguage.setName(
            new HashSet<>(List.of(new MultiLingualContent(serbianTag, "Ruski", 1))));
        var croatianLanguage = new Language();
        croatianLanguage.setLanguageCode(LanguageAbbreviations.CROATIAN);
        croatianLanguage.setName(
            new HashSet<>(List.of(new MultiLingualContent(serbianTag, "Hrvatski", 1))));
        var italianLanguage = new Language();
        italianLanguage.setLanguageCode(LanguageAbbreviations.ITALIAN);
        italianLanguage.setName(
            new HashSet<>(List.of(new MultiLingualContent(serbianTag, "Italijanski", 1))));
        var slovenianLanguage = new Language();
        slovenianLanguage.setLanguageCode(LanguageAbbreviations.SLOVENIAN);
        slovenianLanguage.setName(
            new HashSet<>(List.of(new MultiLingualContent(serbianTag, "Slovenački", 1))));

        languageRepository.saveAll(
            List.of(serbianLanguage, englishLanguage, yuLanguage, germanLanguage, frenchLanguage,
                spanishLanguage, russianLanguage, croatianLanguage, italianLanguage,
                slovenianLanguage));

        // ADMIN USER
        var adminUser =
            new User("admin@admin.com", passwordEncoder.encode("admin"), "note", "Marko",
                "Markovic", false, false, serbianTag, englishTag, adminAuthority, null,
                null, null,
                UserNotificationPeriod.DAILY);
        userRepository.save(adminUser);

        // RESEARCH AREAS - NOT COMPLETE
        var researchArea1 = new ResearchArea(new HashSet<>(Set.of(
            new MultiLingualContent(serbianTag, "Elektrotehnicko i racunarsko inzenjerstvo", 2))),
            new HashSet<>(), null, "elektrotehnicko i racunarsko inzenjerstvo");
        var researchArea2 = new ResearchArea(new HashSet<>(Set.of(
            new MultiLingualContent(serbianTag, "Softversko inzenjerstvo", 2))),
            new HashSet<>(), researchArea1, "softversko inzenjerstvo");
        var researchArea3 = new ResearchArea(new HashSet<>(Set.of(
            new MultiLingualContent(serbianTag, "Cybersecurity", 2))),
            new HashSet<>(), researchArea2, "cybersecurity");

        researchAreaRepository.saveAll(List.of(researchArea1, researchArea2, researchArea3));

        // DEFAULT BRANDING
        setupDefaultBranding(serbianTag, englishTag);

        // COUNTRIES
        csvDataLoader.loadData("countries.csv", this::processCountryLine, ',');
        var yugoslavia = new Country();
        yugoslavia.setCode("YU");
        yugoslavia.setName(Set.of(new MultiLingualContent(serbianTag, "Jugoslavija", 1),
            new MultiLingualContent(englishTag, "Yugoslavia", 2)));
        countryRepository.save(yugoslavia);

        // RESEARCH AREAS
        TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronization() {
            @Override
            public void afterCommit() {
                skosLoader.loadResearchAreas();
            }
        });

        ///////////////////// ASSESSMENT DATA /////////////////////
        assessmentDataInitializer.initializeIndicators(englishTag, serbianTag);
        assessmentDataInitializer.initializeAssessmentClassifications(englishTag, serbianTag);
        var commissions = assessmentDataInitializer.initializeCommissions(englishTag, serbianTag);
        assessmentDataInitializer.initializeRulebooks(englishTag, serbianTag);

        ///////////////////// TESTING DATA /////////////////////
        if (Arrays.stream(environment.getActiveProfiles())
            .anyMatch(profile -> profile.equalsIgnoreCase("test"))) {
            testingDataInitializer.initializeIntegrationTestingData(serbianTag, serbianLanguage,
                englishTag, germanTag, researchArea3, researcherAuthority, commissionAuthority,
                viceDeanForScienceAuthority, institutionalEditorAuthority,
                institutionalLibrarianAuthority, headOfLibraryAuthority,
                promotionRegistryAdministratorAuthority, commissions.a, commissions.b);
        }
    }

    private void processCountryLine(String[] line) {
        var country = new Country();
        country.setCode(line[0]);

        var names = new HashSet<MultiLingualContent>();
        var nameList = line[1].split(";");
        for (int i = 0; i < nameList.length; i++) {
            var nameParts = nameList[i].split("@");
            if (nameParts.length == 2) {
                var content = nameParts[0].trim();
                var languageTagCode = nameParts[1].trim().toUpperCase();

                var languageTag = languageTagRepository
                    .findLanguageTagByLanguageTag(languageTagCode)
                    .orElseGet(() -> {
                        var newLanguageTag = new LanguageTag();
                        newLanguageTag.setLanguageTag(languageTagCode);
                        newLanguageTag.setDisplay(languageTagCode);
                        log.info("Created new language tag: {}", languageTagCode);
                        return languageTagRepository.save(newLanguageTag);
                    });

                var multilingualContent =
                    new MultiLingualContent(languageTag, content, i + 1);
                names.add(multilingualContent);
            }
        }
        country.setName(names);
        country.setProcessedName("");
        country.getName().forEach(name -> {
            country.setProcessedName(country.getProcessedName() + " " +
                StringUtil.performSimpleLatinPreprocessing(name.getContent()));
        });
        countryRepository.save(country);
    }

    private void setupDefaultBranding(LanguageTag serbianTag, LanguageTag englishTag) {
        var brandingInformation = new BrandingInformation();
        brandingInformation.setTitle(new HashSet<>(Set.of(
            new MultiLingualContent(serbianTag, "CRIS UNS", 1),
            new MultiLingualContent(englishTag, "CRIS UNS", 2)
        )));
        brandingInformation.setDescription(new HashSet<>(Set.of(
            new MultiLingualContent(serbianTag,
                "CRIS UNS je informacioni sistem naučno-istraživačke delatnosti Univerziteta u Novom Sadu. U ovom sistemu možete pronaći informacije o istraživačima, organizacionim jedinicama i objavljenim rezultatima ovog univerziteta. Informacioni sistem je barizan na TeslaRIS open-source platformi.",
                1),
            new MultiLingualContent(englishTag,
                "CRIS UNS is the information system of scientific research activities at the University of Novi Sad. In this system, you can find information about researchers, organisation units, and scientific results of this university. The information system is based on the TeslaRIS open-source platform.",
                2)
        )));
        brandingInformationRepository.save(brandingInformation);
    }
}
