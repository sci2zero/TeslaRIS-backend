package rs.teslaris;


import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import javax.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;
import rs.teslaris.core.model.Authority;
import rs.teslaris.core.model.Language;
import rs.teslaris.core.model.OrganisationalUnit;
import rs.teslaris.core.model.Person;
import rs.teslaris.core.model.Privilege;
import rs.teslaris.core.model.User;
import rs.teslaris.core.repository.AuthorityRepository;
import rs.teslaris.core.repository.LanguageRepository;
import rs.teslaris.core.repository.OrganisationalUnitRepository;
import rs.teslaris.core.repository.PersonRepository;
import rs.teslaris.core.repository.PrivilegeRepository;
import rs.teslaris.core.repository.UserRepository;

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

    @Override
    @Transactional
    public void run(ApplicationArguments args) throws Exception {
        var allowAccountTakeover = new Privilege("ALLOW_ACCOUNT_TAKEOVER");
        var takeRoleOfUser = new Privilege("TAKE_ROLE");
        var deactivateUser = new Privilege("DEACTIVATE_USER");
        var updateProfile = new Privilege("UPDATE_PROFILE");
        privilegeRepository.saveAll(
            Arrays.asList(allowAccountTakeover, takeRoleOfUser, deactivateUser, updateProfile));

        var adminAuthority = new Authority("ADMIN",
            new HashSet<>(
                List.of(new Privilege[] {takeRoleOfUser, deactivateUser, updateProfile})));
        var authorAuthority =
            new Authority("AUTHOR",
                new HashSet<>(List.of(new Privilege[] {allowAccountTakeover, updateProfile})));
        authorityRepository.save(adminAuthority);
        authorityRepository.save(authorAuthority);

        var serbianLanguage = new Language("Serbian");
        languageRepository.save(serbianLanguage);

        var person1 = new Person("dummy");
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

        var dummyOU = new OrganisationalUnit("dummy");
        organisationalUnitRepository.save(dummyOU);
    }
}
