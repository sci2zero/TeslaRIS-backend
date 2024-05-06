package rs.teslaris.core.util.notificationhandling;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;
import rs.teslaris.core.model.commontypes.Notification;
import rs.teslaris.core.model.person.PersonName;
import rs.teslaris.core.repository.person.PersonRepository;


@Component
@RequiredArgsConstructor
@Transactional
public class NewOtherNameNotificationHandler implements NotificationHandler {

    private final PersonRepository personRepository;

    @Override
    public void handle(Notification notification) {
        var person = notification.getUser().getPerson();
        var firstname = notification.getValues().get("firstname");
        var lastname = notification.getValues().get("firstname");
        var middlename = notification.getValues().get("firstname");
        person.getOtherNames().add(new PersonName(firstname, middlename, lastname, null, null));

        personRepository.save(person);
    }
}
