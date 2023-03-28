package rs.teslaris.core.service.impl;

import lombok.RequiredArgsConstructor;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;
import rs.teslaris.core.dto.AuthenticationRequestDTO;
import rs.teslaris.core.dto.TakeRoleOfUserRequestDTO;
import rs.teslaris.core.exception.TakeOfRoleNotPermittedException;
import rs.teslaris.core.model.User;
import rs.teslaris.core.repository.UserRepository;
import rs.teslaris.core.service.UserService;
import rs.teslaris.core.util.jwt.JwtUtil;

@Service
@RequiredArgsConstructor
public class UserServiceImpl implements UserService {

    private final JwtUtil tokenUtil;

    private final UserRepository userRepository;

    @Override
    public UserDetails loadUserByUsername(String username) throws UsernameNotFoundException {
        return userRepository.findByEmail(username).orElseThrow(
            () -> new UsernameNotFoundException("User with this email does not exist."));
    }

    @Override
    public String authenticateUser(AuthenticationManager authenticationManager,
                                   AuthenticationRequestDTO authenticationRequest,
                                   String fingerprint) {
        var authentication = authenticationManager.authenticate(
            new UsernamePasswordAuthenticationToken(authenticationRequest.getEmail(),
                authenticationRequest.getPassword())
        );
        SecurityContextHolder.getContext().setAuthentication(authentication);

        return tokenUtil.generateToken(authentication, fingerprint);
    }

    @Override
    public String takeRoleOfUser(TakeRoleOfUserRequestDTO takeRoleOfUserRequest,
                                 String fingerprint) {
        var user = (User) loadUserByUsername(takeRoleOfUserRequest.getUserEmail());

        if (!user.getCanTakeRole()) {
            throw new TakeOfRoleNotPermittedException(
                "User did not allow taking control of his account.");
        }

        user.setCanTakeRole(false);
        userRepository.save(user);

        return tokenUtil.generateToken(user, fingerprint);
    }

    @Override
    public void allowTakingRoleOfAccount(String bearerToken) {
        var email = tokenUtil.extractUsernameFromToken(bearerToken.split(" ")[1]);

        var user = (User) loadUserByUsername(email);
        user.setCanTakeRole(true);

        userRepository.save(user);
    }
}
