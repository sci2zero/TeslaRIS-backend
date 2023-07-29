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
import rs.teslaris.core.model.document.Journal;
import rs.teslaris.core.model.institution.OrganisationUnit;
import rs.teslaris.core.model.person.Contact;
import rs.teslaris.core.model.person.Person;
import rs.teslaris.core.model.person.PersonalInfo;
import rs.teslaris.core.model.person.PostalAddress;
import rs.teslaris.core.model.person.Sex;
import rs.teslaris.core.model.user.Authority;
import rs.teslaris.core.model.user.Privilege;
import rs.teslaris.core.model.user.User;
import rs.teslaris.core.model.user.UserRole;
import rs.teslaris.core.repository.commontypes.CountryRepository;
import rs.teslaris.core.repository.commontypes.LanguageRepository;
import rs.teslaris.core.repository.commontypes.LanguageTagRepository;
import rs.teslaris.core.repository.commontypes.ResearchAreaRepository;
import rs.teslaris.core.repository.document.JournalRepository;
import rs.teslaris.core.repository.person.OrganisationUnitRepository;
import rs.teslaris.core.repository.person.PersonRepository;
import rs.teslaris.core.repository.user.AuthorityRepository;
import rs.teslaris.core.repository.user.PrivilegeRepository;
import rs.teslaris.core.repository.user.UserRepository;

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

    private final ResearchAreaRepository researchAreaRepository;


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

        privilegeRepository.saveAll(
            Arrays.asList(allowAccountTakeover, takeRoleOfUser, deactivateUser, updateProfile,
                createUserBasic, editPersonalInfo, approvePerson, editProofs, editOrganisationUnit,
                editResearchAreas, approvePublication, editOURelations));

        var adminAuthority = new Authority(UserRole.ADMIN.toString(), new HashSet<>(List.of(
            takeRoleOfUser, deactivateUser, updateProfile, editPersonalInfo,
            createUserBasic, approvePerson, editProofs, editOrganisationUnit, editResearchAreas,
            editOURelations, approvePublication)));
        var researcherAuthority = new Authority(UserRole.RESEARCHER.toString(), new HashSet<>(
            List.of(new Privilege[] {allowAccountTakeover, updateProfile, editPersonalInfo,
                createUserBasic, editProofs})));
        authorityRepository.save(adminAuthority);
        authorityRepository.save(researcherAuthority);

        var serbianLanguage = new Language();
        serbianLanguage.setLanguageCode("RS");
        languageRepository.save(serbianLanguage);

        var englishLanguage = new Language();
        englishLanguage.setLanguageCode("EN");
        languageRepository.save(englishLanguage);


        var yuLanguage = new Language();
        yuLanguage.setLanguageCode("YU");
        yuLanguage.setDeleted(true);
        languageRepository.save(yuLanguage);

        var country = new Country("RS", new HashSet<MultiLingualContent>());
        country = countryRepository.save(country);

        var postalAddress = new PostalAddress(country, new HashSet<MultiLingualContent>(),
            new HashSet<MultiLingualContent>());
        var personalInfo =
            new PersonalInfo(LocalDate.of(2000, 1, 25), "Sebia", Sex.MALE, postalAddress,
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

        var englishTag = new LanguageTag("EN", "English");
        languageTagRepository.save(englishTag);
        var serbianTag = new LanguageTag("SR", "Srpski");
        languageTagRepository.save(serbianTag);

        var dummyOU = new OrganisationUnit();
        dummyOU.setNameAbbreviation("FTN");
        dummyOU.setName(new HashSet<>(List.of(new MultiLingualContent[] {
            new MultiLingualContent(englishTag, "Faculty of Technical Sciences", 1),
            new MultiLingualContent(serbianTag, "Fakultet Tehnickih Nauka", 2)})));
        dummyOU.setApproveStatus(ApproveStatus.APPROVED);
        dummyOU.setLocation(new GeoLocation(100.00, 100.00, 100));
        dummyOU.setContact(new Contact("office@ftn.uns.ac.com", "021555666"));
        organisationUnitRepository.save(dummyOU);

        var dummyJournal = new Journal();
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
            null, null);
        researchAreaRepository.save(researchArea1);
    }
}
