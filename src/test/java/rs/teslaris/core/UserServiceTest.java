package rs.teslaris.core;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.Optional;
import org.junit.jupiter.api.Test;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.springframework.boot.test.context.SpringBootTest;
import rs.teslaris.core.exception.NotFoundException;
import rs.teslaris.core.model.Authority;
import rs.teslaris.core.model.User;
import rs.teslaris.core.repository.UserRepository;
import rs.teslaris.core.service.impl.UserServiceImpl;

@SpringBootTest
public class UserServiceTest {

    @Mock
    private UserRepository userRepository;

    @InjectMocks
    private UserServiceImpl userService;

    @Test
    public void shouldDeactivateUserSuccessfully() {
        // given
        Integer userId = 1;
        User user =
            new User("email@email.com", "username", false, true, new Authority("AUTHOR", null));
        user.setId(1);

        when(userRepository.findById(userId)).thenReturn(Optional.of(user));

        // when
        userService.deactivateUser(userId);

        // then
        assertTrue(user.getLocked());
        verify(userRepository, times(1)).save(user);
    }

    @Test
    public void shouldNotDeactivateAdminUser() {
        // given
        Integer userId = 1;
        User user =
            new User("email@email.com", "username", false, true, new Authority("ADMIN", null));
        user.setId(1);

        when(userRepository.findById(userId)).thenReturn(Optional.of(user));

        // when
        userService.deactivateUser(userId);

        // then
        assertFalse(user.getLocked());
        verify(userRepository, never()).save(user);
    }

    @Test
    public void shouldThrowNotFoundExceptionWhenUserNotFound() {
        // given
        Integer userId = 0;

        when(userRepository.findById(userId)).thenReturn(Optional.empty());

        // when
        assertThrows(NotFoundException.class, () -> userService.deactivateUser(userId));

        // then (expected exception)
    }
}
