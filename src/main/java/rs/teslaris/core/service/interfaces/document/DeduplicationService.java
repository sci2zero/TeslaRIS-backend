package rs.teslaris.core.service.interfaces.document;

import org.springframework.stereotype.Service;

@Service
public interface DeduplicationService {

    void startDeduplicationProcessBeforeSchedule();
}
