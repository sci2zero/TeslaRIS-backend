package rs.teslaris.core.util.notificationhandling.handlerimpl;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.Objects;
import java.util.stream.Collectors;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;
import rs.teslaris.core.indexmodel.DocumentPublicationIndex;
import rs.teslaris.core.indexrepository.DocumentPublicationIndexRepository;
import rs.teslaris.core.model.commontypes.ApproveStatus;
import rs.teslaris.core.model.commontypes.BaseEntity;
import rs.teslaris.core.model.commontypes.Notification;
import rs.teslaris.core.model.document.Document;
import rs.teslaris.core.model.document.PersonContribution;
import rs.teslaris.core.model.person.Person;
import rs.teslaris.core.model.person.PersonName;
import rs.teslaris.core.model.person.PersonalInfo;
import rs.teslaris.core.repository.document.DocumentRepository;
import rs.teslaris.core.repository.document.PersonContributionRepository;
import rs.teslaris.core.service.interfaces.person.PersonService;
import rs.teslaris.core.util.exceptionhandling.exception.NotFoundException;
import rs.teslaris.core.util.exceptionhandling.exception.NotificationException;
import rs.teslaris.core.util.notificationhandling.NotificationAction;
import rs.teslaris.core.util.notificationhandling.NotificationHandler;
import rs.teslaris.core.util.search.StringUtil;

@Component
@RequiredArgsConstructor
@Transactional
public class AddedToPublicationNotificationHandler implements NotificationHandler {

    private final PersonService personService;

    private final PersonContributionRepository personContributionRepository;

    private final DocumentRepository documentRepository;

    private final DocumentPublicationIndexRepository documentPublicationIndexRepository;


    @Override
    public void handle(Notification notification, NotificationAction action) {
        if (!action.equals(NotificationAction.REMOVE_FROM_PUBLICATION)) {
            throw new NotificationException("Invalid action.");
        }

        var newPerson = new Person();
        newPerson.setApproveStatus(ApproveStatus.APPROVED);

        var contributionId = Integer.parseInt(notification.getValues().get("contributionId"));
        var contributionOptional = personContributionRepository.findById(contributionId);
        if (contributionOptional.isEmpty()) {
            throw new NotFoundException("Invalid contribution id (" + contributionId + ")");
        }

        var contribution = contributionOptional.get();
        newPerson.setName(new PersonName(
            contribution.getAffiliationStatement().getDisplayPersonName().getFirstname(),
            contribution.getAffiliationStatement().getDisplayPersonName().getOtherName(),
            contribution.getAffiliationStatement().getDisplayPersonName().getLastname(), null,
            null));
        newPerson.setPersonalInfo(new PersonalInfo());
        var savedPerson = personService.save(newPerson);

        contribution.setPerson(savedPerson);
        contribution.getInstitutions().clear();
        personContributionRepository.save(contribution);

        var documentId = Integer.parseInt(notification.getValues().get("documentId"));
        var document = documentRepository.findById(documentId);
        var indexOptional =
            documentPublicationIndexRepository.findDocumentPublicationIndexByDatabaseId(documentId);

        if (document.isPresent() && indexOptional.isPresent()) {
            var index = indexOptional.get();
            reindexContributors(document.get(), index);
            documentPublicationIndexRepository.save(index);
        }

        // TODO: notify admin?
    }

    private void reindexContributors(Document document, DocumentPublicationIndex index) {
        index.setAuthorNames("");
        index.setEditorNames("");
        index.setReviewerNames("");
        index.setAdvisorNames("");

        index.getAuthorIds().clear();
        index.getEditorIds().clear();
        index.getReviewerIds().clear();
        index.getAdvisorIds().clear();

        var organisationUnitIds = new ArrayList<Integer>();

        document.getContributors()
            .stream().sorted(Comparator.comparingInt(PersonContribution::getOrderNumber))
            .forEach(contribution -> {
                var personExists = Objects.nonNull(contribution.getPerson());

                var contributorDisplayName =
                    contribution.getAffiliationStatement().getDisplayPersonName();
                var contributorName =
                    (Objects.toString(contributorDisplayName.getFirstname(), "") + " " +
                        Objects.toString(contributorDisplayName.getOtherName(), "") + " " +
                        Objects.toString(contributorDisplayName.getLastname(), "")).trim();

                organisationUnitIds.addAll(
                    contribution.getInstitutions().stream().map((BaseEntity::getId)).collect(
                        Collectors.toList()));

                switch (contribution.getContributionType()) {
                    case AUTHOR:
                        if (contribution.getOrderNumber() == 1) {
                            contributorName += "*";
                        }

                        if (personExists) {
                            index.getAuthorIds().add(contribution.getPerson().getId());
                        }
                        index.setAuthorNames(StringUtil.removeLeadingColonSpace(
                            index.getAuthorNames() + "; " + contributorName));
                        break;
                    case EDITOR:
                        if (personExists) {
                            index.getEditorIds().add(contribution.getPerson().getId());
                        }
                        index.setEditorNames(StringUtil.removeLeadingColonSpace(
                            index.getEditorNames() + "; " + contributorName));
                        break;
                    case ADVISOR:
                        if (personExists) {
                            index.getAdvisorIds().add(contribution.getPerson().getId());
                        }
                        index.setAdvisorNames(StringUtil.removeLeadingColonSpace(
                            index.getAdvisorNames() + "; " + contributorName));
                        break;
                    case REVIEWER:
                        if (personExists) {
                            index.getReviewerIds().add(contribution.getPerson().getId());
                        }
                        index.setReviewerNames(StringUtil.removeLeadingColonSpace(
                            index.getReviewerNames() + "; " + contributorName));
                        break;
                }
            });
        index.setAuthorNamesSortable(index.getAuthorNames());
        index.setOrganisationUnitIds(organisationUnitIds);
    }
}
