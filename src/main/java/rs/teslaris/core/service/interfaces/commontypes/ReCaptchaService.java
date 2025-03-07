package rs.teslaris.core.service.interfaces.commontypes;

import org.springframework.stereotype.Service;

@Service
public interface ReCaptchaService {

    boolean isCaptchaValid(String token);
}
