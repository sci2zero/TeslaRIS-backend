package rs.teslaris;


import java.time.LocalDate;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import javax.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;
import rs.teslaris.core.model.commontypes.ApproveStatus;
import rs.teslaris.core.model.commontypes.Country;
import rs.teslaris.core.model.commontypes.GeoLocation;
import rs.teslaris.core.model.commontypes.Language;
import rs.teslaris.core.model.commontypes.LanguageTag;
import rs.teslaris.core.model.commontypes.MultiLingualContent;
import rs.teslaris.core.model.commontypes.ResearchArea;
import rs.teslaris.core.model.document.BookSeries;
import rs.teslaris.core.model.document.Conference;
import rs.teslaris.core.model.document.Journal;
import rs.teslaris.core.model.document.Proceedings;
import rs.teslaris.core.model.document.Publisher;
import rs.teslaris.core.model.institution.OrganisationUnit;
import rs.teslaris.core.model.person.Contact;
import rs.teslaris.core.model.person.Person;
import rs.teslaris.core.model.person.PersonName;
import rs.teslaris.core.model.person.PersonalInfo;
import rs.teslaris.core.model.person.PostalAddress;
import rs.teslaris.core.model.person.Sex;
import rs.teslaris.core.model.user.Authority;
import rs.teslaris.core.model.user.PasswordResetToken;
import rs.teslaris.core.model.user.Privilege;
import rs.teslaris.core.model.user.User;
import rs.teslaris.core.model.user.UserRole;
import rs.teslaris.core.repository.commontypes.CountryRepository;
import rs.teslaris.core.repository.commontypes.LanguageRepository;
import rs.teslaris.core.repository.commontypes.LanguageTagRepository;
import rs.teslaris.core.repository.commontypes.ResearchAreaRepository;
import rs.teslaris.core.repository.document.BookSeriesRepository;
import rs.teslaris.core.repository.document.ConferenceRepository;
import rs.teslaris.core.repository.document.JournalRepository;
import rs.teslaris.core.repository.document.ProceedingsRepository;
import rs.teslaris.core.repository.document.PublisherRepository;
import rs.teslaris.core.repository.person.OrganisationUnitRepository;
import rs.teslaris.core.repository.person.PersonRepository;
import rs.teslaris.core.repository.user.AuthorityRepository;
import rs.teslaris.core.repository.user.PasswordResetTokenRepository;
import rs.teslaris.core.repository.user.PrivilegeRepository;
import rs.teslaris.core.repository.user.UserRepository;
import rs.teslaris.core.util.language.LanguageAbbreviations;

@Component
@RequiredArgsConstructor
public class DbInitializer implements ApplicationRunner {

    private final AuthorityRepository authorityRepository;

    private final PrivilegeRepository privilegeRepository;

    private final UserRepository userRepository;

    private final LanguageRepository languageRepository;

    private final LanguageTagRepository languageTagRepository;

    private final PersonRepository personRepository;

    private final OrganisationUnitRepository organisationUnitRepository;

    private final PasswordEncoder passwordEncoder;

    private final CountryRepository countryRepository;

    private final JournalRepository journalRepository;

    private final BookSeriesRepository bookSeriesRepository;

    private final ResearchAreaRepository researchAreaRepository;

    private final ProceedingsRepository proceedingsRepository;

    private final ConferenceRepository conferenceRepository;

    private final PublisherRepository publisherRepository;

    private final PasswordResetTokenRepository passwordResetTokenRepository;


    @Override
    @Transactional
    public void run(ApplicationArguments args) throws Exception {
        var allowAccountTakeover = new Privilege("ALLOW_ACCOUNT_TAKEOVER");
        var takeRoleOfUser = new Privilege("TAKE_ROLE");
        var deactivateUser = new Privilege("DEACTIVATE_USER");
        var updateProfile = new Privilege("UPDATE_PROFILE");
        var createUserBasic = new Privilege("REGISTER_PERSON");
        var editPersonalInfo = new Privilege("EDIT_PERSON_INFORMATION");
        var approvePerson = new Privilege("APPROVE_PERSON");
        var editProofs = new Privilege("EDIT_DOCUMENT_PROOFS");
        var approvePublication = new Privilege("APPROVE_PUBLICATION");
        var editResearchAreas = new Privilege("EDIT_RESEARCH_AREAS");
        var editOrganisationUnit = new Privilege("EDIT_ORGANISATION_UNITS");
        var editOURelations = new Privilege("EDIT_OU_RELATIONS");
        var editPublishers = new Privilege("EDIT_PUBLISHERS");
        var editPublicationSeries = new Privilege("EDIT_PUBLICATION_SERIES");
        var editConferences = new Privilege("EDIT_CONFERENCES");

        privilegeRepository.saveAll(
            Arrays.asList(allowAccountTakeover, takeRoleOfUser, deactivateUser, updateProfile,
                createUserBasic, editPersonalInfo, approvePerson, editProofs, editOrganisationUnit,
                editResearchAreas, approvePublication, editOURelations, editPublishers,
                editPublicationSeries, editConferences));

        var adminAuthority = new Authority(UserRole.ADMIN.toString(), new HashSet<>(
            List.of(takeRoleOfUser, deactivateUser, updateProfile, editPersonalInfo,
                createUserBasic, approvePerson, editProofs, editOrganisationUnit, editResearchAreas,
                editOURelations, approvePublication, editPublishers, editPublicationSeries,
                editConferences)));

        var researcherAuthority = new Authority(UserRole.RESEARCHER.toString(), new HashSet<>(
            List.of(new Privilege[] {allowAccountTakeover, updateProfile, editPersonalInfo,
                createUserBasic, editProofs})));
        authorityRepository.save(adminAuthority);
        authorityRepository.save(researcherAuthority);

        var serbianLanguage = new Language();
        serbianLanguage.setLanguageCode(LanguageAbbreviations.SERBIAN);
        languageRepository.save(serbianLanguage);

        var englishLanguage = new Language();
        englishLanguage.setLanguageCode(LanguageAbbreviations.ENGLISH);
        languageRepository.save(englishLanguage);

        var yuLanguage = new Language();
        yuLanguage.setLanguageCode(LanguageAbbreviations.CROATIAN);
        yuLanguage.setDeleted(true);
        languageRepository.save(yuLanguage);

        var country = new Country("RS", new HashSet<>());
        country = countryRepository.save(country);

        var postalAddress = new PostalAddress(country, new HashSet<>(),
            new HashSet<>());
        var personalInfo =
            new PersonalInfo(LocalDate.of(2000, 1, 25), "Serbia", Sex.MALE, postalAddress,
                new Contact("john@ftn.uns.ac.com", "021555666"));
        var person1 = new Person();
        person1.setApproveStatus(ApproveStatus.APPROVED);
        person1.setPersonalInfo(personalInfo);
        personRepository.save(person1);

        var adminUser =
            new User("admin@admin.com", passwordEncoder.encode("admin"), "note", "Marko",
                "Markovic", false, false, serbianLanguage, adminAuthority, null, null);
        var researcherUser =
            new User("author@author.com", passwordEncoder.encode("author"), "note note note",
                "Janko", "Jankovic", false, false, serbianLanguage, researcherAuthority, person1,
                null);
        userRepository.save(adminUser);
        userRepository.save(researcherUser);

        var englishTag = new LanguageTag(LanguageAbbreviations.ENGLISH, "English");
        languageTagRepository.save(englishTag);
        var serbianTag = new LanguageTag(LanguageAbbreviations.SERBIAN, "Srpski");
        languageTagRepository.save(serbianTag);

        var dummyOU = new OrganisationUnit();
        dummyOU.setNameAbbreviation("FTN");
        dummyOU.setName(new HashSet<>(List.of(new MultiLingualContent[] {
            new MultiLingualContent(englishTag, "Faculty of Technical Sciences", 1),
            new MultiLingualContent(serbianTag, "Fakultet Tehničkih Nauka", 2)})));
        dummyOU.setApproveStatus(ApproveStatus.APPROVED);
        dummyOU.setLocation(new GeoLocation(100.00, 100.00, 100));
        dummyOU.setContact(new Contact("office@ftn.uns.ac.com", "021555666"));
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
        dummyOU2.setLocation(new GeoLocation(120.00, 120.00, 100));
        dummyOU2.setContact(new Contact("office@pmf.uns.ac.com", "021555667"));
        organisationUnitRepository.save(dummyOU2);

        var researchArea1 = new ResearchArea(new HashSet<>(Set.of(
            new MultiLingualContent(serbianTag, "Elektrotehnicko i racunarsko inzenjerstvo", 2))),
            new HashSet<>(), null);
        researchAreaRepository.save(researchArea1);

        var conferenceEvent1 = new Conference();
        conferenceEvent1.setName(Set.of(new MultiLingualContent(serbianTag, "Konferencija", 1)));
        conferenceRepository.save(conferenceEvent1);

        var proceedings1 = new Proceedings();
        proceedings1.setApproveStatus(ApproveStatus.APPROVED);
        proceedings1.setEISBN("MOCK_eISBN");
        proceedings1.setEvent(conferenceEvent1);
        proceedingsRepository.save(proceedings1);

        var proceedings2 = new Proceedings();
        proceedings2.setApproveStatus(ApproveStatus.REQUESTED);
        proceedings2.setEISBN("MOCK_eISBN");
        proceedings2.setEvent(conferenceEvent1);
        proceedingsRepository.save(proceedings2);

        var publisher1 = new Publisher();
        publisher1.setName(Set.of(new MultiLingualContent(englishTag, "Name1", 1)));
        publisher1.setPlace(Set.of(new MultiLingualContent(englishTag, "Place1", 1)));
        publisher1.setState(Set.of(new MultiLingualContent(englishTag, "State1", 1)));
        publisherRepository.save(publisher1);

        var conferenceEvent2 = new Conference();
        conferenceEvent2.setName(Set.of(new MultiLingualContent(serbianTag, "Konferencija2", 1)));
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

        person1.setName(
            new PersonName("Ivan", "Radomir", "Mrsulja", LocalDate.of(2000, 1, 25), null));
        personRepository.save(person1);

        var listMyJournalPublications = new Privilege("LIST_MY_JOURNAL_PUBLICATIONS");
        var deletePerson = new Privilege("DELETE_PERSON");
        privilegeRepository.saveAll(List.of(listMyJournalPublications, deletePerson));

        researcherAuthority.addPrivilege(listMyJournalPublications);
        authorityRepository.save(researcherAuthority);
        adminAuthority.addPrivilege(deletePerson);
        authorityRepository.save(adminAuthority);

        var person2 = new Person();
        var postalAddress2 = new PostalAddress(country, new HashSet<>(),
            new HashSet<>());
        var personalInfo2 =
            new PersonalInfo(LocalDate.of(2000, 1, 31), "Sebia", Sex.MALE, postalAddress2,
                new Contact("mark@ftn.uns.ac.com", "021555769"));
        person2.setApproveStatus(ApproveStatus.APPROVED);
        person2.setPersonalInfo(personalInfo2);
        person1.setName(
            new PersonName("Marko", "Janko", "Markovic", LocalDate.of(2000, 1, 31), null));
        personRepository.save(person2);

        var registerEmployee = new Privilege("REGISTER_EMPLOYEE");
        privilegeRepository.save(registerEmployee);
        adminAuthority.addPrivilege(registerEmployee);
        authorityRepository.save(adminAuthority);

        var institutionalEditorAuthority =
            new Authority(UserRole.INSTITUTIONAL_EDITOR.toString(), new HashSet<>(
                List.of(new Privilege[] {updateProfile})));
        authorityRepository.save(institutionalEditorAuthority);
    }
}
