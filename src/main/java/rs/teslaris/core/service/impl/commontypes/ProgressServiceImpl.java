package rs.teslaris.core.service.impl.commontypes;

import java.io.IOException;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.ConcurrentHashMap;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;
import rs.teslaris.core.dto.commontypes.ProgressEvent;
import rs.teslaris.core.service.interfaces.commontypes.ProgressService;

@Service
@Slf4j
public class ProgressServiceImpl implements ProgressService {

    private static final Map<String, SseEmitter> emitters = new ConcurrentHashMap<>();


    public SseEmitter register(String exportId) {
        SseEmitter emitter = new SseEmitter(0L); // no timeout
        emitters.put(exportId, emitter);

        emitter.onCompletion(() -> emitters.remove(exportId));
        emitter.onTimeout(() -> emitters.remove(exportId));
        emitter.onError(e -> emitters.remove(exportId));

        try {
            emitter.send(SseEmitter.event()
                .name("init")
                .data(new ProgressEvent(0, "REGISTERED")));
        } catch (IOException e) {
            emitter.completeWithError(e);
        }

        return emitter;
    }

    public void send(String exportId, int percent, String stage) {
        SseEmitter emitter = emitters.get(exportId);

        log.debug("Sending to emitter {} present={}", exportId, emitters.containsKey(exportId));

        if (Objects.isNull(emitter)) {
            return;
        }

        try {
            emitter.send(SseEmitter.event()
                .name("progress")
                .data(new ProgressEvent(percent, stage)));
        } catch (Exception e) {
            emitters.remove(exportId);
        }
    }

    public void complete(String exportId) {
        SseEmitter emitter = emitters.remove(exportId);
        if (Objects.nonNull(emitter)) {
            emitter.complete();
        }
    }
}

