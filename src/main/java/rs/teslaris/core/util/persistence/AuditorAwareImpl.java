package rs.teslaris.core.util.persistence;

import java.util.Optional;
import org.springframework.data.domain.AuditorAware;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.util.StringUtils;
import rs.teslaris.core.model.user.User;

public class AuditorAwareImpl implements AuditorAware<String> {
    @Override
    public Optional<String> getCurrentAuditor() {
        if (SecurityContextHolder.getContext().getAuthentication() == null) {
            return Optional.of("SCRIPT_CREATED");
        }

        Object principal = SecurityContextHolder.getContext().getAuthentication().getPrincipal();

        if (principal instanceof String) {
            return Optional.of(StringUtils.capitalize((String) principal));
        } else if (principal instanceof User) {
            return Optional.of(((User) principal).getUsername());
        } else if (principal instanceof org.springframework.security.core.userdetails.User) {
            return Optional.of(
                ((org.springframework.security.core.userdetails.User) principal).getUsername());
        }

        return Optional.of("UNKNOWN");
    }
}
