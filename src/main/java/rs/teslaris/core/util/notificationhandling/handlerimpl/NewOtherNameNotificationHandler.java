package rs.teslaris.core.util.notificationhandling.handlerimpl;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;
import rs.teslaris.core.model.commontypes.Notification;
import rs.teslaris.core.model.person.PersonName;
import rs.teslaris.core.service.interfaces.person.PersonService;
import rs.teslaris.core.util.exceptionhandling.exception.NotificationException;
import rs.teslaris.core.util.notificationhandling.NotificationAction;
import rs.teslaris.core.util.notificationhandling.NotificationHandler;


@Component
@RequiredArgsConstructor
@Transactional
public class NewOtherNameNotificationHandler implements NotificationHandler {

    private final PersonService personService;

    @Override
    public void handle(Notification notification, NotificationAction action) {
        if (!action.equals(NotificationAction.APPROVE)) {
            throw new NotificationException("Invalid action.");
        }

        var person = notification.getUser().getPerson();
        var firstname = notification.getValues().get("firstname");
        var lastname = notification.getValues().get("lastname");
        var middlename = notification.getValues().get("middlename");
        person.getOtherNames().add(new PersonName(firstname, middlename, lastname, null, null));

        personService.indexPerson(personService.save(person));
    }
}
