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

        if (SecurityContextHolder.getContext().getAuthentication()
            .getPrincipal() instanceof String) {
            return Optional.of(StringUtils.capitalize(
                (String) SecurityContextHolder.getContext().getAuthentication()
                    .getPrincipal()));
        }

        return Optional.of(((User) SecurityContextHolder.getContext().getAuthentication()
            .getPrincipal()).getUsername());
    }
}
