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
import rs.teslaris.core.model.Privilege;
import rs.teslaris.core.model.User;
import rs.teslaris.core.repository.AuthorityRepository;
import rs.teslaris.core.repository.PrivilegeRepository;
import rs.teslaris.core.repository.UserRepository;

@Component
@RequiredArgsConstructor
public class DbInitializer implements ApplicationRunner {

    private final AuthorityRepository authorityRepository;

    private final PrivilegeRepository privilegeRepository;

    private final UserRepository userRepository;

    private final PasswordEncoder passwordEncoder;

    @Override
    @Transactional
    public void run(ApplicationArguments args) throws Exception {
        var allowAccountTakeover = new Privilege("ALLOW_ACCOUNT_TAKEOVER");
        var takeRoleOfUser = new Privilege("TAKE_ROLE");
        privilegeRepository.saveAll(
            Arrays.asList(allowAccountTakeover, takeRoleOfUser));

        var adminAuthority = new Authority("ADMIN",
            new HashSet<>(
                List.of(new Privilege[] {takeRoleOfUser})));
        var authorAuthority =
            new Authority("AUTHOR", new HashSet<>(List.of(new Privilege[] {allowAccountTakeover})));
        authorityRepository.save(adminAuthority);
        authorityRepository.save(authorAuthority);

        var adminUser =
            new User("admin@admin.com", passwordEncoder.encode("admin"), false, false,
                adminAuthority);
        var authorUser =
            new User("author@author.com", passwordEncoder.encode("author"), false, false,
                authorAuthority);
        userRepository.save(adminUser);
        userRepository.save(authorUser);
    }
}
