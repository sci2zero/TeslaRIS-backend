package rs.teslaris;


import java.time.LocalDate;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
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
import rs.teslaris.core.model.commontypes.MultiLingualContent;
import rs.teslaris.core.model.institution.OrganisationUnit;
import rs.teslaris.core.model.person.Contact;
import rs.teslaris.core.model.person.Person;
import rs.teslaris.core.model.person.PersonalInfo;
import rs.teslaris.core.model.person.PostalAddress;
import rs.teslaris.core.model.person.Sex;
import rs.teslaris.core.model.user.Authority;
import rs.teslaris.core.model.user.Privilege;
import rs.teslaris.core.model.user.User;
import rs.teslaris.core.repository.commontypes.CountryRepository;
import rs.teslaris.core.repository.commontypes.LanguageRepository;
import rs.teslaris.core.repository.institution.OrganisationalUnitRepository;
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

    private final PersonRepository personRepository;

    private final OrganisationalUnitRepository organisationalUnitRepository;

    private final PasswordEncoder passwordEncoder;

    private final CountryRepository countryRepository;

    @Override
    @Transactional
    public void run(ApplicationArguments args) throws Exception {
        var allowAccountTakeover = new Privilege("ALLOW_ACCOUNT_TAKEOVER");
        var takeRoleOfUser = new Privilege("TAKE_ROLE");
        var deactivateUser = new Privilege("DEACTIVATE_USER");
        var updateProfile = new Privilege("UPDATE_PROFILE");
        var createUserBasic = new Privilege("CREATE_PERSON_BASIC");
        privilegeRepository.saveAll(
            Arrays.asList(allowAccountTakeover, takeRoleOfUser, deactivateUser, updateProfile,
                createUserBasic));

        var adminAuthority = new Authority("ADMIN",
            new HashSet<>(
                List.of(new Privilege[] {takeRoleOfUser, deactivateUser, updateProfile,
                    createUserBasic})));
        var authorAuthority =
            new Authority("AUTHOR",
                new HashSet<>(List.of(new Privilege[] {allowAccountTakeover, updateProfile})));
        authorityRepository.save(adminAuthority);
        authorityRepository.save(authorAuthority);

        var serbianLanguage = new Language();
        serbianLanguage.setLanguageCode("RS");
        languageRepository.save(serbianLanguage);

        var country = new Country("SRB", new HashSet<MultiLingualContent>());
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
                "Markovic", false, false, serbianLanguage,
                adminAuthority, null, null);
        var authorUser =
            new User("author@author.com", passwordEncoder.encode("author"), "note note note",
                "Janko", "Jankovic", false, false, serbianLanguage,
                authorAuthority, person1, null);
        userRepository.save(adminUser);
        userRepository.save(authorUser);

        var dummyOU = new OrganisationUnit();
        dummyOU.setNameAbbreviation("FTN");
        dummyOU.setApproveStatus(ApproveStatus.APPROVED);
        dummyOU.setLocation(new GeoLocation(100.00, 100.00, 100));
        dummyOU.setContact(new Contact("office@ftn.uns.ac.com", "021555666"));
        organisationalUnitRepository.save(dummyOU);
    }
}
