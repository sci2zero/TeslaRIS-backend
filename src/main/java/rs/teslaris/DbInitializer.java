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
        var adminRead = new Privilege("ADMIN_READ");
        var adminWrite = new Privilege("ADMIN_WRITE");
        var adminDelete = new Privilege("ADMIN_DELETE");
        privilegeRepository.saveAll(Arrays.asList(adminRead, adminWrite, adminDelete));

        var adminAuthority = new Authority("ADMIN",
            new HashSet<>(List.of(new Privilege[] {adminRead, adminWrite, adminDelete})));
        authorityRepository.save(adminAuthority);

        var adminUser =
            new User("admin@admin.com", passwordEncoder.encode("admin"), false, adminAuthority);
        userRepository.save(adminUser);
    }
}
