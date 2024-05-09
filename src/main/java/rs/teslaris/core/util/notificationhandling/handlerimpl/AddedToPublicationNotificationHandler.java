package rs.teslaris.core.util.notificationhandling.handlerimpl;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;
import rs.teslaris.core.model.commontypes.Notification;
import rs.teslaris.core.model.person.Person;
import rs.teslaris.core.model.person.PersonName;
import rs.teslaris.core.repository.document.PersonContributionRepository;
import rs.teslaris.core.service.interfaces.person.PersonService;
import rs.teslaris.core.util.exceptionhandling.exception.NotFoundException;
import rs.teslaris.core.util.exceptionhandling.exception.NotificationException;
import rs.teslaris.core.util.notificationhandling.NotificationAction;
import rs.teslaris.core.util.notificationhandling.NotificationHandler;

@Component
@RequiredArgsConstructor
@Transactional
public class AddedToPublicationNotificationHandler implements NotificationHandler {

    private final PersonService personService;

    private final PersonContributionRepository personContributionRepository;

    @Override
    public void handle(Notification notification, NotificationAction action) {
        if (!action.equals(NotificationAction.REMOVE_FROM_PUBLICATION)) {
            throw new NotificationException("Invalid action.");
        }

        var bindedPerson =
            personService.findOne(Integer.parseInt(notification.getValues().get("personId")));
        var newPerson = new Person();
        newPerson.setName(new PersonName(bindedPerson.getName().getFirstname(),
            bindedPerson.getName().getOtherName(), bindedPerson.getName().getLastname(), null,
            null));

        var savedPerson = personService.save(newPerson);

        var contributionId = Integer.parseInt(notification.getValues().get("contributionId"));
        var contributionOptional = personContributionRepository.findById(contributionId);
        if (contributionOptional.isEmpty()) {
            throw new NotFoundException("Invalid contribution id (" + contributionId + ")");
        }

        var contribution = contributionOptional.get();
        contribution.setPerson(savedPerson);
        personContributionRepository.save(contribution);

        // TODO: notify admin?
    }
}
