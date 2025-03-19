package rs.teslaris.core.service.impl.person;

import jakarta.annotation.Nullable;
import jakarta.transaction.Transactional;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Service;
import rs.teslaris.core.dto.document.BookSeriesDTO;
import rs.teslaris.core.dto.document.DocumentDTO;
import rs.teslaris.core.dto.document.EventDTO;
import rs.teslaris.core.dto.document.PersonContributionDTO;
import rs.teslaris.core.dto.document.PublicationSeriesDTO;
import rs.teslaris.core.model.commontypes.ApproveStatus;
import rs.teslaris.core.model.commontypes.Notification;
import rs.teslaris.core.model.document.AffiliationStatement;
import rs.teslaris.core.model.document.Document;
import rs.teslaris.core.model.document.DocumentContributionType;
import rs.teslaris.core.model.document.Event;
import rs.teslaris.core.model.document.PersonContribution;
import rs.teslaris.core.model.document.PersonDocumentContribution;
import rs.teslaris.core.model.document.PersonEventContribution;
import rs.teslaris.core.model.document.PersonPublicationSeriesContribution;
import rs.teslaris.core.model.document.PublicationSeries;
import rs.teslaris.core.model.document.Thesis;
import rs.teslaris.core.model.person.Contact;
import rs.teslaris.core.model.person.Person;
import rs.teslaris.core.model.person.PersonName;
import rs.teslaris.core.model.person.PostalAddress;
import rs.teslaris.core.model.user.User;
import rs.teslaris.core.repository.commontypes.NotificationRepository;
import rs.teslaris.core.repository.document.PersonContributionRepository;
import rs.teslaris.core.repository.user.UserRepository;
import rs.teslaris.core.service.impl.JPAServiceImpl;
import rs.teslaris.core.service.interfaces.commontypes.MultilingualContentService;
import rs.teslaris.core.service.interfaces.person.OrganisationUnitService;
import rs.teslaris.core.service.interfaces.person.PersonContributionService;
import rs.teslaris.core.service.interfaces.person.PersonService;
import rs.teslaris.core.util.exceptionhandling.exception.TypeNotAllowedException;
import rs.teslaris.core.util.notificationhandling.NotificationFactory;

@Service
@RequiredArgsConstructor
@Transactional
public class PersonContributionServiceImpl extends JPAServiceImpl<PersonContribution>
    implements PersonContributionService {

    private final PersonService personService;

    private final OrganisationUnitService organisationUnitService;

    private final MultilingualContentService multilingualContentService;

    private final PersonContributionRepository personContributionRepository;

    private final UserRepository userRepository;

    private final NotificationRepository notificationRepository;


    @Value("${contribution.approved_by_default}")
    private Boolean contributionApprovedByDefault;


    @Override
    public void setPersonDocumentContributionsForDocument(Document document,
                                                          DocumentDTO documentDTO) {
        documentDTO.getContributions().forEach(contributionDTO -> {
            var contribution = new PersonDocumentContribution();
            setPersonContributionCommonFields(contribution, contributionDTO);

            if (contributionDTO.getContributionType()
                .equals(DocumentContributionType.BOARD_MEMBER) && !(document instanceof Thesis)) {
                throw new TypeNotAllowedException(
                    "Only thesis can have a board member contributor type.");
            }

            contribution.setContributionType(contributionDTO.getContributionType());
            contribution.setIsMainContributor(contributionDTO.getIsMainContributor());
            contribution.setIsCorrespondingContributor(
                contributionDTO.getIsCorrespondingContributor());

            var addedPrevoiusly = document.getContributors().stream().anyMatch(
                previousContribution -> compareContributions(previousContribution, contribution));

            if (!addedPrevoiusly) {
                document.addDocumentContribution(contribution);
            }
        });
    }

    @Override
    public void setPersonPublicationSeriesContributionsForJournal(
        PublicationSeries publicationSeries,
        PublicationSeriesDTO journalDTO) {

        journalDTO.getContributions().forEach(contributionDTO -> {
            var contribution = new PersonPublicationSeriesContribution();
            setPersonContributionCommonFields(contribution, contributionDTO);

            contribution.setContributionType(contributionDTO.getContributionType());
            contribution.setDateFrom(contributionDTO.getDateFrom());
            contribution.setDateTo(contributionDTO.getDateTo());

            var addedPrevoiusly = publicationSeries.getContributions().stream().anyMatch(
                previousContribution -> compareContributions(previousContribution, contribution));

            if (!addedPrevoiusly) {
                publicationSeries.addContribution(contribution);
            }
        });
    }

    @Override
    public void setPersonPublicationSeriesContributionsForBookSeries(
        PublicationSeries publicationSeries,
        BookSeriesDTO bookSeriesDTO) {

        bookSeriesDTO.getContributions().forEach(contributionDTO -> {
            var contribution = new PersonPublicationSeriesContribution();
            setPersonContributionCommonFields(contribution, contributionDTO);

            contribution.setContributionType(contributionDTO.getContributionType());
            contribution.setDateFrom(contributionDTO.getDateFrom());
            contribution.setDateTo(contributionDTO.getDateTo());

            var addedPrevoiusly = publicationSeries.getContributions().stream().anyMatch(
                previousContribution -> compareContributions(previousContribution, contribution));

            if (!addedPrevoiusly) {
                publicationSeries.addContribution(contribution);
            }
        });
    }

    @Override
    public void setPersonEventContributionForEvent(Event event, EventDTO eventDTO) {
        eventDTO.getContributions().forEach(contributionDTO -> {
            var contribution = new PersonEventContribution();
            setPersonContributionCommonFields(contribution, contributionDTO);

            contribution.setContributionType(contributionDTO.getEventContributionType());

            var addedPrevoiusly = event.getContributions().stream().anyMatch(
                previousContribution -> compareContributions(previousContribution, contribution));

            if (!addedPrevoiusly) {
                event.addContribution(contribution);
            }
        });
    }

    @Override
    public void deleteContribution(Integer contributionId) {
        personContributionRepository.deleteById(contributionId);
    }

    private void setAffiliationStatement(PersonContribution contribution,
                                         PersonContributionDTO contributionDTO,
                                         Person contributor) {
        var personName = getPersonName(contributionDTO, contributor);

        Contact contact = null;
        if (Objects.nonNull(contributor.getPersonalInfo().getContact())) {
            contact = new Contact(contributor.getPersonalInfo().getContact().getContactEmail(),
                contributor.getPersonalInfo().getContact().getPhoneNumber());
        }

        contribution.setAffiliationStatement(new AffiliationStatement(
            multilingualContentService.getMultilingualContent(
                contributionDTO.getDisplayAffiliationStatement()), personName,
            new PostalAddress(contributor.getPersonalInfo().getPostalAddress().getCountry(),
                multilingualContentService.deepCopy(
                    contributor.getPersonalInfo().getPostalAddress().getStreetAndNumber()),
                multilingualContentService.deepCopy(
                    contributor.getPersonalInfo().getPostalAddress().getCity())),
            contact));
    }

    private PersonName getPersonName(PersonContributionDTO contributionDTO,
                                     Person contributor) {
        var personName = new PersonName(contributionDTO.getPersonName().getFirstname(),
            contributionDTO.getPersonName().getOtherName(),
            contributionDTO.getPersonName().getLastname(),
            contributionDTO.getPersonName().getDateFrom(),
            contributionDTO.getPersonName().getDateTo());
        if (personName.getFirstname().isEmpty() && personName.getLastname().isEmpty() &&
            Objects.nonNull(contributor)) {
            personName = new PersonName(contributor.getName().getFirstname(),
                contributor.getName().getOtherName(),
                contributor.getName().getLastname(),
                contributor.getName().getDateFrom(),
                contributor.getName().getDateTo());
        } else if (Objects.nonNull(contributor)) {
            if (contributor.getOtherNames().contains(personName)) {
                return personName;
            }

            var userOptional = userRepository.findForResearcher(contributor.getId());
            if (userOptional.isPresent()) {
                var notificationValues = new HashMap<String, String>();
                notificationValues.put("firstname", contributionDTO.getPersonName().getFirstname());
                notificationValues.put("middlename",
                    contributionDTO.getPersonName().getOtherName());
                notificationValues.put("lastname", contributionDTO.getPersonName().getLastname());
                createNotification(
                    NotificationFactory.contructNewOtherNameDetectedNotification(notificationValues,
                        userOptional.get()));
            }
        }
        return personName;
    }

    private void setPersonContributionCommonFields(PersonContribution contribution,
                                                   PersonContributionDTO contributionDTO) {
        if (Objects.nonNull(contributionDTO.getPersonId())) {
            var contributor = personService.findOne(contributionDTO.getPersonId());
            contribution.setPerson(contributor);
            setAffiliationStatement(contribution, contributionDTO, contributor);
        } else {
            var affiliationStatement = new AffiliationStatement();
            affiliationStatement.setDisplayPersonName(getPersonName(contributionDTO, null));
            affiliationStatement.setDisplayAffiliationStatement(
                multilingualContentService.getMultilingualContent(
                    contributionDTO.getDisplayAffiliationStatement()));
            contribution.setAffiliationStatement(affiliationStatement);
        }

        contribution.setContributionDescription(multilingualContentService.getMultilingualContent(
            contributionDTO.getContributionDescription()));

        contribution.setInstitutions(new HashSet<>());
        if (Objects.nonNull(contributionDTO.getInstitutionIds())) {
            contributionDTO.getInstitutionIds().forEach(institutionId -> {
                var organisationUnit = organisationUnitService.findOne(institutionId);
                contribution.getInstitutions().add(organisationUnit);
            });
        }

        contribution.setOrderNumber(contributionDTO.getOrderNumber());
        contribution.setApproveStatus(
            contributionApprovedByDefault ? ApproveStatus.APPROVED : ApproveStatus.REQUESTED);
    }

    private boolean compareContributions(PersonContribution previousContribution,
                                         PersonContribution contribution) {
        if (Objects.nonNull(previousContribution.getPerson()) &&
            Objects.nonNull(contribution.getPerson())) {
            return previousContribution.getPerson().getId()
                .equals(contribution.getPerson().getId());
        }

        return previousContribution.getAffiliationStatement().getDisplayPersonName()
            .getFirstname().equals(
                contribution.getAffiliationStatement().getDisplayPersonName()
                    .getFirstname()) &&
            previousContribution.getAffiliationStatement().getDisplayPersonName()
                .getLastname().equals(
                    contribution.getAffiliationStatement().getDisplayPersonName()
                        .getLastname()) &&
            previousContribution.getAffiliationStatement().getDisplayPersonName()
                .getOtherName().equals(
                    contribution.getAffiliationStatement().getDisplayPersonName()
                        .getOtherName());
    }

    public Optional<User> getUserForContributor(Integer contributorId) {
        return userRepository.findForResearcher(contributorId);
    }

    public void notifyContributor(Notification notification) {
        createNotification(notification);
    }

    @Override
    public void reorderContributions(Set<PersonContribution> contributions,
                                     Integer contributionId,
                                     Integer oldContributionOrderNumber,
                                     Integer newContributionOrderNumber) {
        if (oldContributionOrderNumber > newContributionOrderNumber) {
            contributions.forEach(contribution -> {
                if (contribution.getId().equals(contributionId)) {
                    contribution.setOrderNumber(newContributionOrderNumber);
                } else if (contribution.getOrderNumber() >= newContributionOrderNumber &&
                    contribution.getOrderNumber() < oldContributionOrderNumber) {
                    contribution.setOrderNumber(contribution.getOrderNumber() + 1);
                }
            });
        } else if (oldContributionOrderNumber < newContributionOrderNumber) {
            contributions.forEach(contribution -> {
                if (contribution.getId().equals(contributionId)) {
                    contribution.setOrderNumber(newContributionOrderNumber);
                } else if (contribution.getOrderNumber() > oldContributionOrderNumber &&
                    contribution.getOrderNumber() <= newContributionOrderNumber) {
                    contribution.setOrderNumber(contribution.getOrderNumber() - 1);
                }
            });
        }
    }

    @Override
    @Nullable
    public PersonDocumentContribution findContributionForResearcherAndDocument(Integer personId,
                                                                               Integer documentId) {
        return personContributionRepository.fetchPersonDocumentContributionOnDocument(personId,
            documentId).orElse(null);
    }

    @Override
    public List<Integer> getIdsOfNonRelatedDocuments(Integer organisationUnitId, Integer personId) {
        return personContributionRepository.fetchAllDocumentsWhereInstitutionIsNotListed(personId,
            organisationUnitId);
    }

    @Override
    protected JpaRepository<PersonContribution, Integer> getEntityRepository() {
        return personContributionRepository;
    }

    private void createNotification(Notification notification) {
        var newOtherNameNotifications =
            notificationRepository.getNewOtherNameNotificationsForUser(
                notification.getUser().getId());
        for (var oldNotification : newOtherNameNotifications) {
            if (oldNotification.getValues().get("firstname")
                .equals(notification.getValues().get("firstname")) &&
                oldNotification.getValues().get("middlename")
                    .equals(notification.getValues().get("middlename")) &&
                oldNotification.getValues().get("lastname")
                    .equals(notification.getValues().get("lastname"))) {
                return;
            }
        }

        notificationRepository.save(notification);
    }
}
