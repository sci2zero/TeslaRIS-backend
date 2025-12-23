package rs.teslaris.core.service.interfaces.commontypes;

import org.springframework.stereotype.Service;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

@Service
public interface ProgressService {

    SseEmitter register(String exportId);

    void send(String exportId, int percent, String stage);

    void complete(String exportId);
}
