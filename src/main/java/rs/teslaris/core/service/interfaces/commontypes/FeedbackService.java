package rs.teslaris.core.service.interfaces.commontypes;

import org.springframework.stereotype.Service;
import rs.teslaris.core.dto.commontypes.ContactFormContentDTO;

@Service
public interface FeedbackService {

    void sendFeedbackMessage(ContactFormContentDTO contactFormContent);
}
