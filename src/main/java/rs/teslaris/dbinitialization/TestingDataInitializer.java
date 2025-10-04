package rs.teslaris.dbinitialization;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import lombok.RequiredArgsConstructor;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;
import rs.teslaris.assessment.model.AssessmentMeasure;
import rs.teslaris.assessment.model.AssessmentResearchArea;
import rs.teslaris.assessment.model.AssessmentRulebook;
import rs.teslaris.assessment.model.classification.AssessmentClassification;
import rs.teslaris.assessment.model.classification.EventAssessmentClassification;
import rs.teslaris.assessment.model.classification.PublicationSeriesAssessmentClassification;
import rs.teslaris.assessment.model.indicator.ApplicableEntityType;
import rs.teslaris.assessment.model.indicator.DocumentIndicator;
import rs.teslaris.assessment.model.indicator.EventIndicator;
import rs.teslaris.assessment.model.indicator.Indicator;
import rs.teslaris.assessment.repository.AssessmentMeasureRepository;
import rs.teslaris.assessment.repository.AssessmentResearchAreaRepository;
import rs.teslaris.assessment.repository.AssessmentRulebookRepository;
import rs.teslaris.assessment.repository.classification.AssessmentClassificationRepository;
import rs.teslaris.assessment.repository.classification.EventAssessmentClassificationRepository;
import rs.teslaris.assessment.repository.classification.PublicationSeriesAssessmentClassificationRepository;
import rs.teslaris.assessment.repository.indicator.DocumentIndicatorRepository;
import rs.teslaris.assessment.repository.indicator.EventIndicatorRepository;
import rs.teslaris.assessment.repository.indicator.IndicatorRepository;
import rs.teslaris.core.dto.commontypes.ExportFileType;
import rs.teslaris.core.model.commontypes.AccessLevel;
import rs.teslaris.core.model.commontypes.ApproveStatus;
import rs.teslaris.core.model.commontypes.Country;
import rs.teslaris.core.model.commontypes.GeoLocation;
import rs.teslaris.core.model.commontypes.Language;
import rs.teslaris.core.model.commontypes.LanguageTag;
import rs.teslaris.core.model.commontypes.MultiLingualContent;
import rs.teslaris.core.model.commontypes.RecurrenceType;
import rs.teslaris.core.model.commontypes.ResearchArea;
import rs.teslaris.core.model.commontypes.ScheduledTaskMetadata;
import rs.teslaris.core.model.commontypes.ScheduledTaskType;
import rs.teslaris.core.model.document.AccessRights;
import rs.teslaris.core.model.document.AffiliationStatement;
import rs.teslaris.core.model.document.BookSeries;
import rs.teslaris.core.model.document.Conference;
import rs.teslaris.core.model.document.Dataset;
import rs.teslaris.core.model.document.DocumentContributionType;
import rs.teslaris.core.model.document.DocumentFile;
import rs.teslaris.core.model.document.DocumentFileSection;
import rs.teslaris.core.model.document.EventsRelation;
import rs.teslaris.core.model.document.EventsRelationType;
import rs.teslaris.core.model.document.Journal;
import rs.teslaris.core.model.document.License;
import rs.teslaris.core.model.document.Monograph;
import rs.teslaris.core.model.document.MonographPublication;
import rs.teslaris.core.model.document.MonographPublicationType;
import rs.teslaris.core.model.document.MonographType;
import rs.teslaris.core.model.document.Patent;
import rs.teslaris.core.model.document.PersonDocumentContribution;
import rs.teslaris.core.model.document.PersonPublicationSeriesContribution;
import rs.teslaris.core.model.document.Proceedings;
import rs.teslaris.core.model.document.PublicationSeriesContributionType;
import rs.teslaris.core.model.document.Publisher;
import rs.teslaris.core.model.document.ResourceType;
import rs.teslaris.core.model.document.Software;
import rs.teslaris.core.model.document.Thesis;
import rs.teslaris.core.model.document.ThesisType;
import rs.teslaris.core.model.institution.Commission;
import rs.teslaris.core.model.institution.OrganisationUnit;
import rs.teslaris.core.model.person.Contact;
import rs.teslaris.core.model.person.Education;
import rs.teslaris.core.model.person.Employment;
import rs.teslaris.core.model.person.EmploymentPosition;
import rs.teslaris.core.model.person.ExpertiseOrSkill;
import rs.teslaris.core.model.person.InvolvementType;
import rs.teslaris.core.model.person.Membership;
import rs.teslaris.core.model.person.Person;
import rs.teslaris.core.model.person.PersonName;
import rs.teslaris.core.model.person.PersonalInfo;
import rs.teslaris.core.model.person.PostalAddress;
import rs.teslaris.core.model.person.Prize;
import rs.teslaris.core.model.person.Sex;
import rs.teslaris.core.model.user.Authority;
import rs.teslaris.core.model.user.PasswordResetToken;
import rs.teslaris.core.model.user.User;
import rs.teslaris.core.model.user.UserNotificationPeriod;
import rs.teslaris.core.repository.commontypes.CountryRepository;
import rs.teslaris.core.repository.commontypes.ScheduledTaskMetadataRepository;
import rs.teslaris.core.repository.document.BookSeriesRepository;
import rs.teslaris.core.repository.document.ConferenceRepository;
import rs.teslaris.core.repository.document.DatasetRepository;
import rs.teslaris.core.repository.document.EventsRelationRepository;
import rs.teslaris.core.repository.document.JournalRepository;
import rs.teslaris.core.repository.document.MonographPublicationRepository;
import rs.teslaris.core.repository.document.MonographRepository;
import rs.teslaris.core.repository.document.PatentRepository;
import rs.teslaris.core.repository.document.PersonContributionRepository;
import rs.teslaris.core.repository.document.ProceedingsRepository;
import rs.teslaris.core.repository.document.PublisherRepository;
import rs.teslaris.core.repository.document.SoftwareRepository;
import rs.teslaris.core.repository.document.ThesisRepository;
import rs.teslaris.core.repository.institution.OrganisationUnitRepository;
import rs.teslaris.core.repository.person.PersonRepository;
import rs.teslaris.core.repository.user.PasswordResetTokenRepository;
import rs.teslaris.core.repository.user.UserRepository;
import rs.teslaris.thesislibrary.model.PageContentType;
import rs.teslaris.thesislibrary.model.PageType;
import rs.teslaris.thesislibrary.model.Promotion;
import rs.teslaris.thesislibrary.model.PublicReviewPageContent;
import rs.teslaris.thesislibrary.model.ThesisFileSection;
import rs.teslaris.thesislibrary.repository.PromotionRepository;
import rs.teslaris.thesislibrary.repository.PublicReviewPageContentRepository;

@Component
@RequiredArgsConstructor
@Transactional
public class TestingDataInitializer {

    private final UserRepository userRepository;

    private final PasswordEncoder passwordEncoder;

    private final CountryRepository countryRepository;

    private final PersonRepository personRepository;

    private final OrganisationUnitRepository organisationUnitRepository;

    private final JournalRepository journalRepository;

    private final BookSeriesRepository bookSeriesRepository;

    private final ProceedingsRepository proceedingsRepository;

    private final ConferenceRepository conferenceRepository;

    private final PublisherRepository publisherRepository;

    private final PasswordResetTokenRepository passwordResetTokenRepository;

    private final PatentRepository patentRepository;

    private final SoftwareRepository softwareRepository;

    private final DatasetRepository datasetRepository;

    private final PersonContributionRepository personContributionRepository;

    private final MonographRepository monographRepository;

    private final EventsRelationRepository eventsRelationRepository;

    private final MonographPublicationRepository monographPublicationRepository;

    private final ThesisRepository thesisRepository;

    private final AssessmentMeasureRepository assessmentMeasureRepository;

    private final AssessmentRulebookRepository assessmentRulebookRepository;

    private final DocumentIndicatorRepository documentIndicatorRepository;

    private final EventAssessmentClassificationRepository eventAssessmentClassificationRepository;

    private final PublicationSeriesAssessmentClassificationRepository
        publicationSeriesAssessmentClassificationRepository;

    private final EventIndicatorRepository eventIndicatorRepository;

    private final AssessmentClassificationRepository assessmentClassificationRepository;

    private final IndicatorRepository indicatorRepository;

    private final AssessmentResearchAreaRepository assessmentResearchAreaRepository;

    private final PromotionRepository promotionRepository;

    private final PublicReviewPageContentRepository publicReviewPageContentRepository;

    private final ScheduledTaskMetadataRepository scheduledTaskMetadataRepository;


    public void initializeIntegrationTestingData(LanguageTag serbianTag, Language serbianLanguage,
                                                 LanguageTag englishTag, LanguageTag germanTag,
                                                 ResearchArea researchArea3,
                                                 Authority researcherAuthority,
                                                 Authority commissionAuthority,
                                                 Authority viceDeanForScienceAuthority,
                                                 Authority institutionalEditorAuthority,
                                                 Authority institutionalLibrarianAuthority,
                                                 Authority headOfLibraryAuthority,
                                                 Authority promotionRegistryAdminAuthority,
                                                 Commission commission5, Commission commission6) {
        var country = new Country("SRB", new HashSet<>(), "");
        countryRepository.save(country);

        var postalAddress = new PostalAddress(country, new HashSet<>(),
            new HashSet<>());
        var personalInfo =
            new PersonalInfo(LocalDate.of(2000, 1, 25), "Serbia", Sex.MALE, postalAddress,
                new Contact("john@ftn.uns.ac.com", "021555666"), new HashSet<>(), new HashSet<>());
        var person1 = new Person();
        person1.getOldIds().add(3);
        person1.setName(
            new PersonName("Ivan", "Radomir", "Mrsulja", LocalDate.of(2000, 1, 25), null));
        person1.setApproveStatus(ApproveStatus.APPROVED);
        person1.setPersonalInfo(personalInfo);
        person1.setOrcid("0009-0008-0599-0599");
        person1.setOpenAlexId("A5070362523");
        person1.setScopusAuthorId("35795419600");
        person1.setWebOfScienceResearcherId("J-4074-2012");
        person1.setDateOfLastIndicatorHarvest(LocalDate.of(2025, 1, 1));
        personRepository.save(person1);

        var researcherUser =
            new User("author@author.com", passwordEncoder.encode("author"), "note note note",
                "Dragan", "Ivanovic", false, false, serbianTag, serbianTag,
                researcherAuthority, person1,
                null, null, UserNotificationPeriod.DAILY);
        userRepository.save(researcherUser);

        var dummyOU = new OrganisationUnit();
        dummyOU.setNameAbbreviation("FTN");
        dummyOU.getOldIds().add(2);
        dummyOU.setName(new HashSet<>(List.of(new MultiLingualContent[] {
            new MultiLingualContent(englishTag, "Faculty of Technical Sciences", 1),
            new MultiLingualContent(serbianTag, "Fakultet Tehničkih Nauka", 2)})));
        dummyOU.setApproveStatus(ApproveStatus.APPROVED);
        dummyOU.setScopusAfid("60068801");
        dummyOU.setRor("00xa57a59");
        dummyOU.setOpenAlexId("I4401727005");
        dummyOU.setLocation(new GeoLocation(19.850885, 45.245688, "NOWHERE"));
        dummyOU.setContact(new Contact("office@ftn.uns.ac.com", "021555666"));
        dummyOU.getAllowedThesisTypes().addAll(
            List.of(ThesisType.PHD.name(), ThesisType.PHD_ART_PROJECT.name(),
                ThesisType.MASTER.name(), ThesisType.BACHELOR.name(),
                ThesisType.BACHELOR_WITH_HONORS.name()));
        dummyOU.setIsClientInstitution(true);
        organisationUnitRepository.save(dummyOU);
        researcherUser.setOrganisationUnit(dummyOU);
        userRepository.save(researcherUser);

        var dummyJournal = new Journal();
        dummyJournal.setTitle(Set.of(new MultiLingualContent(englishTag, "Title1", 1)));
        dummyJournal.setNameAbbreviation(Set.of(new MultiLingualContent(englishTag, "ABR1", 1)));
        journalRepository.save(dummyJournal);

        var dummyOU2 = new OrganisationUnit();
        dummyOU2.setNameAbbreviation("PMF");
        dummyOU2.setName(new HashSet<>(List.of(new MultiLingualContent[] {
            new MultiLingualContent(englishTag, "Faculty of Sciences", 1),
            new MultiLingualContent(serbianTag, "Prirodno matematicki fakultet", 2)})));
        dummyOU2.setApproveStatus(ApproveStatus.APPROVED);
        dummyOU2.setLocation(new GeoLocation(19.8502021, 45.2454147, "NOWHERE"));
        dummyOU2.setContact(new Contact("office@pmf.uns.ac.com", "021555667"));
        dummyOU2.setScopusAfid("60068802");
        organisationUnitRepository.save(dummyOU2);

        var conferenceEvent1 = new Conference();
        conferenceEvent1.setName(Set.of(new MultiLingualContent(serbianTag, "Konferencija", 1)));
        conferenceEvent1.setDateFrom(LocalDate.of(2021, 3, 6));
        conferenceEvent1.setDateTo(LocalDate.of(2021, 3, 10));
        conferenceEvent1.setSerialEvent(false);
        conferenceRepository.save(conferenceEvent1);

        var proceedings1 = new Proceedings();
        proceedings1.getTitle().add(new MultiLingualContent(englishTag, "Proceedings 1", 1));
        proceedings1.setApproveStatus(ApproveStatus.APPROVED);
        proceedings1.setEISBN("978-3-16-148410-0");
        proceedings1.setDocumentDate("2021");
        proceedings1.setEvent(conferenceEvent1);
        proceedingsRepository.save(proceedings1);

        var proceedings2 = new Proceedings();
        proceedings2.getTitle().add(new MultiLingualContent(englishTag, "Proceedings 2", 1));
        proceedings2.setApproveStatus(ApproveStatus.REQUESTED);
        proceedings2.setEISBN("978-3-16-145410-0");
        proceedings2.setEvent(conferenceEvent1);
        proceedingsRepository.save(proceedings2);

        var publisher1 = new Publisher();
        publisher1.setName(Set.of(new MultiLingualContent(englishTag, "Name1", 1)));
        publisher1.setPlace(Set.of(new MultiLingualContent(englishTag, "Place1", 1)));
        publisher1.setCountry(country);
        publisherRepository.save(publisher1);

        var conferenceEvent2 = new Conference();
        conferenceEvent2.setName(Set.of(new MultiLingualContent(serbianTag, "Konferencija2", 1)));
        conferenceEvent2.setDateFrom(LocalDate.of(2020, 6, 13));
        conferenceEvent2.setDateTo(LocalDate.of(2020, 6, 19));
        conferenceEvent2.setSerialEvent(false);
        conferenceRepository.save(conferenceEvent2);

        var journal2 = new Journal();
        journal2.setTitle(Set.of(new MultiLingualContent(englishTag, "Title2", 1)));
        journal2.setNameAbbreviation(Set.of(new MultiLingualContent(englishTag, "ABR2", 1)));
        journalRepository.save(journal2);

        var bookSeries1 = new BookSeries();
        bookSeries1.setTitle(Set.of(new MultiLingualContent(englishTag, "BookSeries1", 1)));
        bookSeries1.setNameAbbreviation(Set.of(new MultiLingualContent(englishTag, "ABR1", 1)));
        bookSeriesRepository.save(bookSeries1);

        var bookSeries2 = new BookSeries();
        bookSeries2.setTitle(Set.of(new MultiLingualContent(englishTag, "BookSeries2", 1)));
        bookSeries2.setNameAbbreviation(Set.of(new MultiLingualContent(englishTag, "ABR2", 1)));
        bookSeriesRepository.save(bookSeries2);

        var passwordResetRequest = new PasswordResetToken("TOKEN", researcherUser);
        passwordResetTokenRepository.save(passwordResetRequest);

        var person2 = new Person();
        var postalAddress2 = new PostalAddress(country, new HashSet<>(),
            new HashSet<>());
        var personalInfo2 =
            new PersonalInfo(LocalDate.of(2000, 1, 31), "Germany", Sex.MALE, postalAddress2,
                new Contact("joakim@email.com", "021555769"), new HashSet<>(), new HashSet<>());
        person2.setApproveStatus(ApproveStatus.APPROVED);
        person2.setPersonalInfo(personalInfo2);
        person2.setName(
            new PersonName("Schöpfel", "", "Joachim", LocalDate.of(2000, 1, 31), null));
        person2.setScopusAuthorId("14619562900");
        person2.setOrcid("0000-0002-8096-5149");
        person2.setDateOfLastIndicatorHarvest(LocalDate.of(2025, 2, 1));
        personRepository.save(person2);

        var software = new Software();
        software.setTitle(Set.of(new MultiLingualContent(englishTag,
            "TeslaRIS - Reengineering of CRIS at the University of Novi Sad", 1)));
        software.setInternalNumber("123456");
        software.setDoi("10.1109/tsmc.2014.2347265");
        software.setApproveStatus(ApproveStatus.APPROVED);

        var patent = new Patent();
        patent.setTitle(Set.of(new MultiLingualContent(englishTag, "Dummy Patent", 1)));
        patent.setApproveStatus(ApproveStatus.APPROVED);
        patentRepository.save(patent);

        var dataset = new Dataset();
        dataset.setTitle(Set.of(new MultiLingualContent(englishTag, "Dummy Dataset", 1)));
        dataset.setApproveStatus(ApproveStatus.APPROVED);
        dataset.setDoi("10.1007/s11192-024-05076-2");
        dataset.setDocumentDate("2020-01-01");

        var datasetContribution = new PersonDocumentContribution();
        datasetContribution.setPerson(person1);
        datasetContribution.setContributionType(DocumentContributionType.AUTHOR);
        datasetContribution.setIsMainContributor(true);
        datasetContribution.setIsCorrespondingContributor(false);
        datasetContribution.setOrderNumber(1);
        datasetContribution.setDocument(dataset);
        datasetContribution.setApproveStatus(ApproveStatus.APPROVED);
        datasetContribution.setAffiliationStatement(
            new AffiliationStatement(new HashSet<>(), new PersonName(),
                new PostalAddress(country, new HashSet<>(), new HashSet<>()), new Contact("", "")));

        dataset.setContributors(Set.of(datasetContribution));
        datasetRepository.save(dataset);

        var sci2zero = new OrganisationUnit();
        sci2zero.setNameAbbreviation("Sci2zero");
        sci2zero.setName(new HashSet<>(List.of(new MultiLingualContent[] {
            new MultiLingualContent(serbianTag, "Science 2.0 Alliance", 1)})));
        sci2zero.setApproveStatus(ApproveStatus.APPROVED);
        sci2zero.setContact(new Contact("info@sci2zero.com", "021555666"));
        organisationUnitRepository.save(sci2zero);

        person1.getBiography()
            .add(new MultiLingualContent(englishTag, "Lorem ipsum dolor sit amet.", 1));
        person1.getBiography().add(new MultiLingualContent(serbianTag, "Srpska biografija.", 2));
        person1.getKeyword().add(new MultiLingualContent(englishTag,
            "Machine Learning\nCybersecurity\nReverse Engineering\nWeb Security", 1));
        person1.addInvolvement(
            new Employment(LocalDate.of(2021, 10, 3), null, ApproveStatus.APPROVED,
                new HashSet<>(), InvolvementType.EMPLOYED_AT, new HashSet<>(), null, dummyOU,
                EmploymentPosition.TEACHING_ASSISTANT, Set.of(new MultiLingualContent(englishTag,
                "Courses: Digital Documents Management, Secure Software Development", 1))));
        person1.addInvolvement(
            new Employment(LocalDate.of(2021, 10, 3), null, ApproveStatus.APPROVED,
                new HashSet<>(), InvolvementType.HIRED_BY, new HashSet<>(), null, sci2zero,
                EmploymentPosition.COLLABORATOR, Set.of(new MultiLingualContent(englishTag,
                "TeslaRIS - reingeneering of CRIS at the university of Novi Sad.", 1))));
        person1.addInvolvement(
            new Membership(LocalDate.of(2021, 10, 3), null, ApproveStatus.APPROVED,
                new HashSet<>(), InvolvementType.MEMBER_OF, new HashSet<>(), null, sci2zero,
                Set.of(new MultiLingualContent(englishTag,
                    "I just wanted to be around cool kids...", 1)),
                Set.of(new MultiLingualContent(englishTag,
                    "Gold member", 1))));
        person1.addInvolvement(
            new Education(LocalDate.of(2018, 10, 1), LocalDate.of(2023, 9, 1),
                ApproveStatus.APPROVED,
                new HashSet<>(), InvolvementType.STUDIED_AT, new HashSet<>(), null, dummyOU,
                Set.of(new MultiLingualContent(englishTag, "Reverse Image Search System", 1),
                    new MultiLingualContent(englishTag, "Sistem za reverznu pretragu slika", 1)),
                Set.of(new MultiLingualContent(englishTag, "Master in Software", 1),
                    new MultiLingualContent(englishTag, "Master inženjer softvera", 1)),
                Set.of(new MultiLingualContent(englishTag, "Msc", 1))));
        person1.getExpertisesAndSkills().add(new ExpertiseOrSkill(
            Set.of(new MultiLingualContent(englishTag, "Cybersecurity", 1)),
            Set.of(new MultiLingualContent(englishTag,
                "Proficiency in web exploitation and reverse engineering.", 1)),
            Set.of(
                new DocumentFile("ISACA Cybersecurity Fundamentals - Certificate.pdf", "1111.pdf",
                    new HashSet<>(), "application/pdf", 200L, ResourceType.SUPPLEMENT,
                    AccessRights.RESTRICTED_ACCESS, License.BY_NC, ApproveStatus.APPROVED, true,
                    LocalDateTime.now(),
                    false, false, null, null, person1)), person1));
        person1.getExpertisesAndSkills().add(new ExpertiseOrSkill(
            Set.of(new MultiLingualContent(englishTag, "CERIF-based systems", 1)),
            Set.of(new MultiLingualContent(englishTag,
                "Contributing to VIVO, Vitro and TeslaRIS current research information systems.",
                1)),
            new HashSet<>(), person1));
        person1.getPrizes().add(new Prize(
            Set.of(
                new MultiLingualContent(englishTag, "Serbian Cybersecurity Challenge - 1st place",
                    1)),
            Set.of(new MultiLingualContent(englishTag,
                "1st place on a national cybersecurity competition finals. The competition is conducted in 5-man teams.",
                1)),
            Set.of(new DocumentFile("1st place certificate.pdf", "2222.pdf",
                new HashSet<>(), "application/pdf", 127L, ResourceType.SUPPLEMENT,
                AccessRights.OPEN_ACCESS, License.BY_NC, ApproveStatus.APPROVED, true,
                LocalDateTime.now(), false, false, null, null, person1)),
            LocalDate.of(2023, 4, 17), person1));
        personRepository.save(person1);

        country.getName().add(new MultiLingualContent(serbianTag, "Srbija", 1));
        var country2 = new Country("MNE",
            new HashSet<>(List.of(new MultiLingualContent(serbianTag, "Crna Gora", 1))),
            "crna gora");
        countryRepository.saveAll(List.of(country, country2));

        datasetContribution.getAffiliationStatement().setDisplayPersonName(
            new PersonName("Ivan", "R.", "M.", LocalDate.of(2000, 1, 31), null));
        personContributionRepository.save(datasetContribution);

        var softwareContribution = new PersonDocumentContribution();
        softwareContribution.setPerson(person1);
        softwareContribution.setContributionType(DocumentContributionType.AUTHOR);
        softwareContribution.setIsMainContributor(true);
        softwareContribution.setIsCorrespondingContributor(false);
        softwareContribution.setOrderNumber(1);
        softwareContribution.setDocument(dataset);
        softwareContribution.setApproveStatus(ApproveStatus.APPROVED);
        softwareContribution.getInstitutions().add(dummyOU);
        softwareContribution.setAffiliationStatement(
            new AffiliationStatement(new HashSet<>(), new PersonName(),
                new PostalAddress(country, new HashSet<>(), new HashSet<>()), new Contact("", "")));
        softwareContribution.getAffiliationStatement().setDisplayPersonName(
            new PersonName("Ivan", "", "M.", LocalDate.of(2000, 1, 31), null));
        personContributionRepository.save(softwareContribution);

        dummyOU.getResearchAreas().add(researchArea3);
        organisationUnitRepository.save(dummyOU);

        var monograph1 = new Monograph();
        monograph1.setApproveStatus(ApproveStatus.APPROVED);
        monograph1.setTitle(
            Set.of(new MultiLingualContent(serbianTag, "Monografija 1", 1)));
        monograph1.setMonographType(MonographType.BOOK);
        var monographContribution = new PersonDocumentContribution();
        monographContribution.setPerson(person2);
        monographContribution.setContributionType(DocumentContributionType.AUTHOR);
        monographContribution.setIsMainContributor(true);
        monographContribution.setIsCorrespondingContributor(false);
        monographContribution.setOrderNumber(1);
        monographContribution.setDocument(monograph1);
        monographContribution.setApproveStatus(ApproveStatus.APPROVED);
        monographContribution.setAffiliationStatement(
            new AffiliationStatement(new HashSet<>(), new PersonName(),
                new PostalAddress(country, new HashSet<>(), new HashSet<>()), new Contact("", "")));
        monographContribution.getAffiliationStatement().setDisplayPersonName(
            new PersonName("Joachim", "N.", "S.", null, null));

        monograph1.getContributors().add(monographContribution);
        monographRepository.save(monograph1);

        var monograph2 = new Monograph();
        monograph2.setApproveStatus(ApproveStatus.APPROVED);
        monograph2.setTitle(
            Set.of(new MultiLingualContent(serbianTag, "Monografija 2", 1)));
        monograph2.setMonographType(MonographType.BOOK);
        monograph2.setDocumentDate("2024");
        monographRepository.save(monograph2);

        var researcherUser2 =
            new User("author2@author.com", passwordEncoder.encode("author2"), "note note note",
                "Schöpfel", "Joachim", false, false, englishTag, germanTag,
                researcherAuthority, person2,
                null, null, UserNotificationPeriod.WEEKLY);
        userRepository.save(researcherUser2);

        var person3 = new Person();
        var postalAddress3 = new PostalAddress(country, new HashSet<>(),
            new HashSet<>());
        var personalInfo3 =
            new PersonalInfo(LocalDate.of(2000, 1, 31), "Serbia", Sex.MALE, postalAddress3,
                new Contact("test@email.com", "021555769"), new HashSet<>(), new HashSet<>());
        person3.setApproveStatus(ApproveStatus.APPROVED);
        person3.setPersonalInfo(personalInfo3);
        person3.setName(
            new PersonName("Dusan", "", "Nikolic", LocalDate.of(1976, 7, 16), null));
        person3.setScopusAuthorId("14419566900");
        personRepository.save(person3);

        var softwareContribution2 = new PersonDocumentContribution();
//        softwareContribution2.setPerson(person3);
        softwareContribution2.setContributionType(DocumentContributionType.AUTHOR);
        softwareContribution2.setIsMainContributor(false);
        softwareContribution2.setIsCorrespondingContributor(false);
        softwareContribution2.setOrderNumber(2);
        softwareContribution2.setDocument(dataset);
        softwareContribution2.setApproveStatus(ApproveStatus.APPROVED);
        softwareContribution2.setAffiliationStatement(
            new AffiliationStatement(new HashSet<>(), new PersonName(),
                new PostalAddress(country, new HashSet<>(), new HashSet<>()), new Contact("", "")));
        softwareContribution2.getAffiliationStatement().setDisplayPersonName(
            new PersonName("Dušan", "", "N.", null, null));
        personContributionRepository.save(softwareContribution2);

        software.addDocumentContribution(softwareContribution);
        software.addDocumentContribution(softwareContribution2);
        softwareRepository.save(software);

        var person4 = new Person();
        var postalAddress4 = new PostalAddress(country, new HashSet<>(),
            new HashSet<>());
        var personalInfo4 =
            new PersonalInfo(LocalDate.of(1976, 6, 24), "Serbia", Sex.FEMALE, postalAddress4,
                new Contact("test1@email.com", "021555769"), new HashSet<>(), new HashSet<>());
        person4.setApproveStatus(ApproveStatus.APPROVED);
        person4.setPersonalInfo(personalInfo4);
        person4.setName(
            new PersonName("Jovana", "", "Stankovic", LocalDate.of(1976, 7, 16), null));
        person4.setScopusAuthorId("14419566900");
//        person4.setOrcid("0009-0008-0599-0599");
        personRepository.save(person4);

        var conferenceEvent3 = new Conference();
        conferenceEvent3.setName(Set.of(new MultiLingualContent(serbianTag, "Konferencija3", 1)));
        conferenceEvent3.setDateFrom(LocalDate.of(2024, 6, 29));
        conferenceEvent3.setDateTo(LocalDate.of(2024, 7, 3));
        conferenceEvent3.setSerialEvent(true);
        conferenceRepository.save(conferenceEvent3);

        var eventsRelation1 = new EventsRelation();
        eventsRelation1.setSource(conferenceEvent1);
        eventsRelation1.setTarget(conferenceEvent2);
        eventsRelation1.setEventsRelationType(EventsRelationType.PART_OF);
        eventsRelationRepository.save(eventsRelation1);

        var eventsRelation2 = new EventsRelation();
        eventsRelation2.setSource(conferenceEvent1);
        eventsRelation2.setTarget(conferenceEvent3);
        eventsRelation2.setEventsRelationType(EventsRelationType.BELONGS_TO_SERIES);
        eventsRelationRepository.save(eventsRelation2);

        var monographPublication1 = new MonographPublication();
        monographPublication1.setApproveStatus(ApproveStatus.APPROVED);
        monographPublication1.setDocumentDate("2024");
        monographPublication1.setMonograph(monograph1);
        monographPublication1.setMonographPublicationType(MonographPublicationType.PREFACE);
        monographPublication1.setTitle(
            Set.of(new MultiLingualContent(englishTag, "Monograph Publication 1", 1)));
        monographPublicationRepository.save(monographPublication1);

        var monographPublication2 = new MonographPublication();
        monographPublication2.setApproveStatus(ApproveStatus.APPROVED);
        monographPublication2.setDocumentDate("2024");
        monographPublication2.setMonograph(monograph1);
        monographPublication2.setMonographPublicationType(MonographPublicationType.CHAPTER);
        monographPublication2.setTitle(
            Set.of(new MultiLingualContent(serbianTag, "Rad u monografiji 2", 1)));
        monographPublicationRepository.save(monographPublication2);

        var thesis1 = new Thesis();
        thesis1.setApproveStatus(ApproveStatus.APPROVED);
        thesis1.setThesisType(ThesisType.PHD);
        thesis1.setDocumentDate("2021");
        thesis1.setOrganisationUnit(dummyOU);
        thesis1.setTitle(
            Set.of(new MultiLingualContent(serbianTag, "Doktorska disertacija", 1)));
        thesis1.setLanguage(serbianLanguage);
        thesis1.setThesisDefenceDate(LocalDate.of(2024, 1, 31));

        var thesisContribution = new PersonDocumentContribution();
        thesisContribution.setPerson(person1);
        thesisContribution.setContributionType(DocumentContributionType.AUTHOR);
        thesisContribution.setIsMainContributor(true);
        thesisContribution.setIsCorrespondingContributor(true);
        thesisContribution.setOrderNumber(1);
        thesisContribution.setDocument(thesis1);
        thesisContribution.setApproveStatus(ApproveStatus.APPROVED);
        thesisContribution.setInstitutions(Set.of(dummyOU));
        thesisContribution.setAffiliationStatement(
            new AffiliationStatement(new HashSet<>(), person1.getName(),
                new PostalAddress(country, new HashSet<>(), new HashSet<>()), new Contact("", "")));

        thesis1.setContributors(Set.of(thesisContribution));
        thesisRepository.save(thesis1);

        var thesis2 = new Thesis();
        thesis2.setApproveStatus(ApproveStatus.APPROVED);
        thesis2.setThesisType(ThesisType.MASTER);
        thesis2.setDocumentDate("2023");
        thesis2.setOrganisationUnit(dummyOU2);
        thesis2.setTitle(
            Set.of(new MultiLingualContent(serbianTag, "Master rad", 1)));
        thesisRepository.save(thesis2);

        var conferenceEvent4 = new Conference();
        conferenceEvent4.setName(Set.of(new MultiLingualContent(englishTag, "EURO CRIS", 1)));
        conferenceEvent4.setDateFrom(LocalDate.of(2023, 5, 12));
        conferenceEvent4.setDateTo(LocalDate.of(2023, 5, 17));
        conferenceEvent4.setSerialEvent(false);
        conferenceRepository.save(conferenceEvent4);

        var monograph3 = new Monograph();
        monograph3.setApproveStatus(ApproveStatus.APPROVED);
        monograph3.setTitle(
            Set.of(new MultiLingualContent(englishTag, "Dummy Translation", 1)));
        monograph3.setMonographType(MonographType.TRANSLATION);
        monographRepository.save(monograph3);

        var indicator1 = new Indicator();
        indicator1.setCode("Code 1");
        indicator1.setTitle(Set.of(new MultiLingualContent(englishTag, "Indicator 1", 1)));
        indicator1.setDescription(
            Set.of(new MultiLingualContent(englishTag, "Indicator 1 description", 1)));
        indicator1.setAccessLevel(AccessLevel.OPEN);
        indicator1.setApplicableTypes(Set.of(ApplicableEntityType.ALL));

        var indicator2 = new Indicator();
        indicator2.setCode("Code 2");
        indicator2.setTitle(Set.of(new MultiLingualContent(englishTag, "Indicator 2", 1)));
        indicator2.setDescription(
            Set.of(new MultiLingualContent(englishTag, "Indicator 2 description", 1)));

        var indicator3 = new Indicator();
        indicator3.setCode("Code 3");
        indicator3.setTitle(Set.of(new MultiLingualContent(englishTag, "Indicator 3", 1)));
        indicator3.setDescription(
            Set.of(new MultiLingualContent(englishTag, "Indicator 3 description", 1)));
        indicator3.setAccessLevel(AccessLevel.CLOSED);

        var indicator4 = new Indicator();
        indicator4.setCode("Code 4");
        indicator4.setTitle(Set.of(new MultiLingualContent(englishTag, "Indicator 4", 1)));
        indicator4.setDescription(
            Set.of(new MultiLingualContent(englishTag, "Indicator 4 description", 1)));
        indicator4.setAccessLevel(AccessLevel.ADMIN_ONLY);

        indicatorRepository.saveAll(List.of(indicator1, indicator2, indicator3, indicator4));

        var assessmentClassification1 = new AssessmentClassification();
        assessmentClassification1.setFormalDescriptionOfRule("Rule 1");
        assessmentClassification1.setCode("Code 1");
        assessmentClassification1.setTitle(
            Set.of(new MultiLingualContent(englishTag, "Assessment Classification 1", 1)));

        var assessmentClassification2 = new AssessmentClassification();
        assessmentClassification2.setFormalDescriptionOfRule("Rule 2");
        assessmentClassification2.setCode("Code 2");
        assessmentClassification2.setTitle(
            Set.of(new MultiLingualContent(englishTag, "Assessment Classification 2", 1)));

        assessmentClassificationRepository.saveAll(
            List.of(assessmentClassification1, assessmentClassification2));

        var assessmentMeasure1 = new AssessmentMeasure();
        assessmentMeasure1.setCode("Code 1");
        assessmentMeasure1.setPointRule("serbianPointsRulebook2025");
        assessmentMeasure1.setScalingRule("serbianScalingRulebook2025");
        assessmentMeasure1.setTitle(
            Set.of(new MultiLingualContent(englishTag, "Assessment Measure 1", 1)));

        var assessmentMeasure2 = new AssessmentMeasure();
        assessmentMeasure2.setCode("Code 2");
        assessmentMeasure2.setPointRule("serbianPointsRulebook2025");
        assessmentMeasure2.setScalingRule("serbianScalingRulebook2025");
        assessmentMeasure2.setTitle(
            Set.of(new MultiLingualContent(englishTag, "Assessment Measure 2", 1)));

        var assessmentMeasure3 = new AssessmentMeasure();
        assessmentMeasure3.setCode("Code 3");
        assessmentMeasure3.setPointRule("serbianPointsRulebook2025");
        assessmentMeasure3.setScalingRule("serbianScalingRulebook2025");
        assessmentMeasure3.setTitle(
            Set.of(new MultiLingualContent(englishTag, "Assessment Measure 3", 1)));

        assessmentMeasureRepository.saveAll(
            List.of(assessmentMeasure1, assessmentMeasure2, assessmentMeasure3));

        var assessmentRulebook1 = new AssessmentRulebook();
        assessmentRulebook1.setName(
            Set.of(new MultiLingualContent(englishTag, "Assessment Rulebook 1", 1),
                new MultiLingualContent(serbianTag, "Pravilnik 1", 2)));
        assessmentRulebook1.setDescription(
            Set.of(new MultiLingualContent(englishTag, "Description 1", 1),
                new MultiLingualContent(serbianTag, "Opis 1", 2)));
        assessmentRulebook1.setIssueDate(LocalDate.of(2023, 10, 1));

        var assessmentRulebook2 = new AssessmentRulebook();
        assessmentRulebook2.setName(
            Set.of(new MultiLingualContent(englishTag, "Assessment Rulebook 2", 1),
                new MultiLingualContent(serbianTag, "Pravilnik 2", 2)));
        assessmentRulebook2.setDescription(
            Set.of(new MultiLingualContent(englishTag, "Description 2", 1),
                new MultiLingualContent(serbianTag, "Opis 2", 2)));
        assessmentRulebook2.setIssueDate(LocalDate.of(2023, 10, 1));
        assessmentRulebook2.setAssessmentMeasures(List.of(assessmentMeasure3));
        assessmentMeasure3.setRulebook(assessmentRulebook2);

        assessmentRulebookRepository.saveAll(List.of(assessmentRulebook1, assessmentRulebook2));

        var documentIndicator1 = new DocumentIndicator();
        documentIndicator1.setTextualValue("ADMIN ACCESS INDICATOR");
        documentIndicator1.setIndicator(indicator4);
        documentIndicator1.setDocument(dataset);

        var documentIndicator2 = new DocumentIndicator();
        documentIndicator2.setTextualValue("CLOSED ACCESS INDICATOR");
        documentIndicator2.setIndicator(indicator3);
        documentIndicator2.setDocument(dataset);

        var documentIndicator3 = new DocumentIndicator();
        documentIndicator3.setTextualValue("OPEN ACCESS INDICATOR");
        documentIndicator3.setIndicator(indicator1);
        documentIndicator3.setDocument(dataset);

        var documentIndicatorToDelete = new DocumentIndicator();
        documentIndicatorToDelete.setTextualValue("OPEN ACCESS INDICATOR");
        documentIndicatorToDelete.setIndicator(indicator1);
        documentIndicatorToDelete.setDocument(dataset);

        documentIndicatorRepository.saveAll(
            List.of(documentIndicator1, documentIndicator2, documentIndicator3,
                documentIndicatorToDelete));

        documentIndicator1.getProofs().add(new DocumentFile("Proof 1", "3333.pdf",
            new HashSet<>(), "application/pdf", 127L, ResourceType.SUPPLEMENT,
            AccessRights.OPEN_ACCESS, License.BY_SA, ApproveStatus.APPROVED, true,
            LocalDateTime.now(),
            false, false, "123.pdf", null, null));
        documentIndicatorRepository.save(documentIndicator1);

        var eventIndicator1 = new EventIndicator();
        eventIndicator1.setTextualValue("OPEN ACCESS INDICATOR");
        eventIndicator1.setIndicator(indicator1);
        eventIndicator1.setEvent(conferenceEvent2);
        eventIndicatorRepository.save(eventIndicator1);

        var eventAssessmentClassification1 = new EventAssessmentClassification();
        eventAssessmentClassification1.setEvent(conferenceEvent1);
        eventAssessmentClassification1.setAssessmentClassification(assessmentClassification1);
        eventAssessmentClassification1.setClassificationYear(2023);

        var eventAssessmentClassification2 = new EventAssessmentClassification();
        eventAssessmentClassification2.setEvent(conferenceEvent1);
        eventAssessmentClassification2.setAssessmentClassification(assessmentClassification1);
        eventAssessmentClassification2.setCommission(commission5);
        eventAssessmentClassification2.setClassificationYear(2020);

        eventAssessmentClassificationRepository.saveAll(
            List.of(eventAssessmentClassification1, eventAssessmentClassification2));

        var publicationSeriesAssessmentClassification =
            new PublicationSeriesAssessmentClassification();
        publicationSeriesAssessmentClassification.setPublicationSeries(dummyJournal);
        publicationSeriesAssessmentClassification.setAssessmentClassification(
            assessmentClassification1);
        publicationSeriesAssessmentClassification.setClassificationYear(2020);

        publicationSeriesAssessmentClassificationRepository.saveAll(
            List.of(publicationSeriesAssessmentClassification));

        var commissionUser =
            new User("commission@commission.com", passwordEncoder.encode("commission"),
                "note note note",
                "FTN", "", false, false, serbianTag, serbianTag, commissionAuthority,
                null,
                dummyOU, commission5, UserNotificationPeriod.WEEKLY);
        userRepository.save(commissionUser);

        var commissionUser2 =
            new User("commission2@commission.com", passwordEncoder.encode("commission2"),
                "note note note",
                "PMF", "", false, false, serbianTag, serbianTag, commissionAuthority,
                null,
                dummyOU2, commission6, UserNotificationPeriod.WEEKLY);
        userRepository.save(commissionUser2);

        var assessmentResearchArea = new AssessmentResearchArea();
        assessmentResearchArea.setPerson(person1);
        assessmentResearchArea.setResearchAreaCode("TECHNICAL");
        assessmentResearchAreaRepository.save(assessmentResearchArea);

        var viceDeanUser =
            new User("vicedean@vicedean.com", passwordEncoder.encode("vicedean"),
                "note note note",
                "Nikola", "Nikolic", false, false, serbianTag, serbianTag,
                viceDeanForScienceAuthority,
                null,
                dummyOU, null, UserNotificationPeriod.WEEKLY);

        var institutionalEditorUser =
            new User("editor@editor.com", passwordEncoder.encode("editor"), "note note note",
                "Nikola", "Markovic", false, false, serbianTag, serbianTag,
                institutionalEditorAuthority,
                null,
                dummyOU, null, UserNotificationPeriod.WEEKLY);

        var institutionalLibrarianUser =
            new User("librarian@librarian.com", passwordEncoder.encode("librarian"),
                "note note note",
                "Mirka", "Maric", false, false, serbianTag, serbianTag,
                institutionalLibrarianAuthority,
                null,
                dummyOU, null, UserNotificationPeriod.WEEKLY);

        var headOfLibraryUser =
            new User("head_of_library@library.com", passwordEncoder.encode("head_of_library"),
                "note note note",
                "Djordje", "Perovic", false, false, serbianTag, serbianTag,
                headOfLibraryAuthority,
                null,
                dummyOU, null, UserNotificationPeriod.WEEKLY);

        var promotionRegistryAdminUser =
            new User("promotion@registry.com", passwordEncoder.encode("promotion_registry"),
                "note note note",
                "Davor", "Kontić", false, false, serbianTag, serbianTag,
                promotionRegistryAdminAuthority,
                null,
                dummyOU, null, UserNotificationPeriod.DAILY);

        var institutionalEditorUser2 =
            new User("editor2@editor.com", passwordEncoder.encode("editor2"), "note note note",
                "Marko", "Nikolic", false, false, serbianTag, serbianTag,
                institutionalEditorAuthority,
                null,
                dummyOU2, null, UserNotificationPeriod.WEEKLY);

        userRepository.saveAll(
            List.of(viceDeanUser, institutionalEditorUser, institutionalLibrarianUser,
                headOfLibraryUser, promotionRegistryAdminUser, institutionalEditorUser2));

        var software2 = new Software();
        software2.setTitle(Set.of(new MultiLingualContent(englishTag,
            "CRIS UNS", 1)));
        software2.setInternalNumber("654321");
        software2.setApproveStatus(ApproveStatus.APPROVED);
        software2.setDoi("10.1038/nature.2012.9872");
        software2.setDocumentDate("2012-3-14");

        var softwareContribution3 = new PersonDocumentContribution();
        softwareContribution3.setPerson(person1);
        softwareContribution3.setContributionType(DocumentContributionType.AUTHOR);
        softwareContribution3.setIsMainContributor(true);
        softwareContribution3.setIsCorrespondingContributor(false);
        softwareContribution3.setOrderNumber(1);
        softwareContribution3.setDocument(dataset);
        softwareContribution3.setApproveStatus(ApproveStatus.APPROVED);
        softwareContribution3.getInstitutions().add(dummyOU);
        softwareContribution3.setAffiliationStatement(
            new AffiliationStatement(new HashSet<>(), new PersonName(),
                new PostalAddress(country, new HashSet<>(), new HashSet<>()), new Contact("", "")));
        softwareContribution3.getAffiliationStatement().setDisplayPersonName(
            new PersonName("Ivan", "", "R. M.", LocalDate.of(2000, 1, 31), null));
        personContributionRepository.save(softwareContribution3);

        software2.addDocumentContribution(softwareContribution3);
        softwareRepository.save(software2);

        var promotion1 = new Promotion();
        promotion1.setPromotionDate(LocalDate.of(2023, 5, 1));
        promotion1.setPromotionTime(LocalTime.NOON);
        promotion1.setPlaceOrVenue("Some place");
        promotion1.setInstitution(dummyOU);
        promotionRepository.save(promotion1);

        var thesis3 = new Thesis();
        thesis3.setApproveStatus(ApproveStatus.APPROVED);
        thesis3.setThesisType(ThesisType.PHD);
        thesis3.setDocumentDate("2022");
        thesis3.setOrganisationUnit(dummyOU);
        thesis3.setTitle(
            Set.of(new MultiLingualContent(serbianTag, "Doktorska disertacija 2", 1)));
        thesis3.setLanguage(serbianLanguage);
        thesis3.setThesisDefenceDate(LocalDate.of(2024, 3, 31));

        var thesisContribution2 = new PersonDocumentContribution();
        thesisContribution2.setPerson(person1);
        thesisContribution2.setContributionType(DocumentContributionType.AUTHOR);
        thesisContribution2.setIsMainContributor(true);
        thesisContribution2.setIsCorrespondingContributor(true);
        thesisContribution2.setOrderNumber(1);
        thesisContribution2.setDocument(thesis3);
        thesisContribution2.setApproveStatus(ApproveStatus.APPROVED);
        thesisContribution2.setInstitutions(Set.of(dummyOU));
        thesisContribution2.setAffiliationStatement(
            new AffiliationStatement(new HashSet<>(), person1.getName(),
                new PostalAddress(country, new HashSet<>(), new HashSet<>()), new Contact("", "")));

        thesis3.setContributors(Set.of(thesisContribution2));
        thesisRepository.save(thesis3);

        var thesis4 = new Thesis();
        thesis4.setApproveStatus(ApproveStatus.APPROVED);
        thesis4.setThesisType(ThesisType.PHD);
        thesis4.setDocumentDate("2024");
        thesis4.setOrganisationUnit(dummyOU);
        thesis4.setTitle(
            Set.of(
                new MultiLingualContent(serbianTag, "Jos neka test disertacija", 1)));
        thesis4.setLanguage(serbianLanguage);
        thesis4.setThesisDefenceDate(LocalDate.of(2025, 3, 31));
        thesisRepository.save(thesis4);

        var yetAnotherJournal = new Journal();
        yetAnotherJournal.setTitle(
            Set.of(new MultiLingualContent(englishTag, "Yet another journal", 1)));
        yetAnotherJournal.setNameAbbreviation(
            Set.of(new MultiLingualContent(englishTag, "ABBBR1", 1)));

        var publicationSeriesContribution = new PersonPublicationSeriesContribution();
        publicationSeriesContribution.setPerson(person1);
        publicationSeriesContribution.setContributionType(
            PublicationSeriesContributionType.SCIENTIFIC_BOARD_MEMBER);
        publicationSeriesContribution.setDateFrom(LocalDate.of(2020, 1, 12));
        publicationSeriesContribution.setOrderNumber(1);
        publicationSeriesContribution.setPublicationSeries(yetAnotherJournal);
        publicationSeriesContribution.setApproveStatus(ApproveStatus.APPROVED);
        publicationSeriesContribution.setInstitutions(Set.of(dummyOU));
        publicationSeriesContribution.setAffiliationStatement(
            new AffiliationStatement(new HashSet<>(), person1.getName(),
                new PostalAddress(country, new HashSet<>(), new HashSet<>()), new Contact("", "")));

        yetAnotherJournal.addContribution(publicationSeriesContribution);

        var publicationSeriesContribution2 = new PersonPublicationSeriesContribution();
        publicationSeriesContribution2.setPerson(person2);
        publicationSeriesContribution2.setContributionType(
            PublicationSeriesContributionType.SCIENTIFIC_BOARD_MEMBER);
        publicationSeriesContribution2.setDateFrom(LocalDate.of(2020, 3, 23));
        publicationSeriesContribution2.setOrderNumber(2);
        publicationSeriesContribution2.setPublicationSeries(yetAnotherJournal);
        publicationSeriesContribution2.setApproveStatus(ApproveStatus.APPROVED);
        publicationSeriesContribution2.setInstitutions(Set.of(dummyOU));
        publicationSeriesContribution2.setAffiliationStatement(
            new AffiliationStatement(new HashSet<>(), person1.getName(),
                new PostalAddress(country, new HashSet<>(), new HashSet<>()), new Contact("", "")));

        yetAnotherJournal.addContribution(publicationSeriesContribution2);
        journalRepository.save(yetAnotherJournal);

        var thesis5 = new Thesis();
        thesis5.setApproveStatus(ApproveStatus.APPROVED);
        thesis5.setThesisType(ThesisType.PHD);
        thesis5.setDocumentDate("2024");
        thesis5.setOrganisationUnit(dummyOU);
        thesis5.setTitle(
            Set.of(new MultiLingualContent(serbianTag,
                "Doktorska disertacija, zavrsen uvid, neodbranjena", 1)));
        thesis5.setLanguage(serbianLanguage);
        thesis5.getPublicReviewStartDates().add(LocalDate.of(2024, 3, 16));
        thesis5.setPublicReviewCompleted(true);

        var thesisContribution3 = new PersonDocumentContribution();
        thesisContribution3.setPerson(person2);
        thesisContribution3.setContributionType(DocumentContributionType.AUTHOR);
        thesisContribution3.setIsMainContributor(true);
        thesisContribution3.setIsCorrespondingContributor(true);
        thesisContribution3.setOrderNumber(1);
        thesisContribution3.setDocument(thesis5);
        thesisContribution3.setApproveStatus(ApproveStatus.APPROVED);
        thesisContribution3.setInstitutions(Set.of(dummyOU));
        thesisContribution3.setAffiliationStatement(
            new AffiliationStatement(new HashSet<>(),
                new PersonName(person2.getName().getFirstname(), "",
                    person2.getName().getLastname(), null, null),
                new PostalAddress(country, new HashSet<>(), new HashSet<>()), new Contact("", "")));

        thesis5.getContributors().add(thesisContribution3);
        thesisRepository.save(thesis5);

        var thesis6 = new Thesis();
        thesis6.setApproveStatus(ApproveStatus.APPROVED);
        thesis6.setThesisType(ThesisType.PHD);
        thesis6.setDocumentDate("2024");
        thesis6.setOrganisationUnit(dummyOU);
        thesis6.setTitle(
            Set.of(new MultiLingualContent(serbianTag,
                "Doktorska disertacija, zavrsen uvid, odbranjena", 1)));
        thesis6.setLanguage(serbianLanguage);
        thesis6.getPublicReviewStartDates().add(LocalDate.of(2024, 12, 20));
        thesis6.setThesisDefenceDate(LocalDate.of(2025, 2, 20));
        thesis6.setPublicReviewCompleted(true);
        thesis6.getOldIds().add(998);

        var thesisContribution4 = new PersonDocumentContribution();
        thesisContribution4.setPerson(person3);
        thesisContribution4.setContributionType(DocumentContributionType.AUTHOR);
        thesisContribution4.setIsMainContributor(true);
        thesisContribution4.setIsCorrespondingContributor(true);
        thesisContribution4.setOrderNumber(1);
        thesisContribution4.setDocument(thesis6);
        thesisContribution4.setApproveStatus(ApproveStatus.APPROVED);
        thesisContribution4.setInstitutions(Set.of(dummyOU));
        thesisContribution4.setAffiliationStatement(
            new AffiliationStatement(new HashSet<>(),
                new PersonName(person3.getName().getFirstname(), "",
                    person3.getName().getLastname(), null, null),
                new PostalAddress(country, new HashSet<>(), new HashSet<>()), new Contact("", "")));

        thesis6.getContributors().add(thesisContribution4);
        thesisRepository.save(thesis6);

        var pageContent1 = new PublicReviewPageContent();
        pageContent1.setContentType(PageContentType.IMPORTANT_NOTE);
        pageContent1.setPageType(PageType.CURRENT);
        pageContent1.setThesisTypes(Set.of(ThesisType.PHD, ThesisType.PHD_ART_PROJECT));
        pageContent1.setInstitution(dummyOU);
        pageContent1.setContent(new HashSet<>(List.of(new MultiLingualContent(serbianTag,
            "Primedbe na doktorsku disertaciju dostaviti u štampanom obliku na adresu Univerzitet u Novom Sadu, Dr Zorana Đinđića 1. Anonimne primedbe se neće uzimati u razmatranje.",
            1))));

        var pageContent2 = new PublicReviewPageContent();
        pageContent2.setContentType(PageContentType.NOTE);
        pageContent2.setPageType(PageType.ARCHIVE);
        pageContent2.setThesisTypes(Set.of(ThesisType.PHD, ThesisType.PHD_ART_PROJECT));
        pageContent2.setInstitution(dummyOU);
        pageContent2.setContent(new HashSet<>(List.of(new MultiLingualContent(serbianTag,
            "Za elektronske verzije doktorskih disertacija obratiti se dr Mirjani Brković, Centralna biblioteka UNS-a, telefon 485-2040.",
            1))));

        var pageContent3 = new PublicReviewPageContent();
        pageContent3.setContentType(PageContentType.TEXT);
        pageContent3.setPageType(PageType.ALL);
        pageContent3.setThesisTypes(Set.of(ThesisType.PHD, ThesisType.PHD_ART_PROJECT));
        pageContent3.setInstitution(dummyOU);
        pageContent3.setContent(new HashSet<>(List.of(new MultiLingualContent(serbianTag,
            "Štampana verzija doktorske disertacije dostupna je u našoj biblioteci.",
            1))));

        publicReviewPageContentRepository.saveAll(
            List.of(pageContent1, pageContent2, pageContent3));

        scheduledTaskMetadataRepository.save(
            new ScheduledTaskMetadata("ID", LocalDateTime.now(),
                ScheduledTaskType.THESIS_LIBRARY_BACKUP, Map.of(
                "institutionId", 1,
                "from", LocalDate.of(2000, 1, 1).toString(),
                "to", LocalDate.now().toString(),
                "types", new ArrayList<>(List.of(ThesisType.PHD)),
                "thesisFileSections", new ArrayList<>(List.of(ThesisFileSection.PRELIMINARY_FILES,
                    DocumentFileSection.FILE_ITEMS)),
                "defended", true,
                "putOnReview", false,
                "userId", 1,
                "language", "SR",
                "metadataFormat", ExportFileType.CSV
            ), RecurrenceType.ONCE));
    }
}
