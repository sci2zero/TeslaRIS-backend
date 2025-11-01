package rs.teslaris.core.service.impl.document;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import rs.teslaris.core.annotation.Traceable;
import rs.teslaris.core.indexmodel.DocumentPublicationIndex;
import rs.teslaris.core.indexmodel.PersonIndex;
import rs.teslaris.core.indexrepository.DocumentPublicationIndexRepository;
import rs.teslaris.core.model.person.DeclinedDocumentClaim;
import rs.teslaris.core.repository.document.PersonDocumentContributionRepository;
import rs.teslaris.core.repository.person.DeclinedDocumentClaimRepository;
import rs.teslaris.core.service.interfaces.commontypes.NotificationService;
import rs.teslaris.core.service.interfaces.commontypes.SearchService;
import rs.teslaris.core.service.interfaces.document.DocumentClaimingService;
import rs.teslaris.core.service.interfaces.document.DocumentPublicationService;
import rs.teslaris.core.service.interfaces.person.PersonService;
import rs.teslaris.core.service.interfaces.user.UserService;
import rs.teslaris.core.util.notificationhandling.NotificationFactory;

@Service
@RequiredArgsConstructor
@Transactional
@Traceable
public class DocumentClaimingServiceImpl implements DocumentClaimingService {

    private final DocumentPublicationIndexRepository documentPublicationIndexRepository;

    private final SearchService<PersonIndex> personSearchService;

    private final PersonService personService;

    private final PersonDocumentContributionRepository personDocumentContributionRepository;

    private final NotificationService notificationService;

    private final UserService userService;

    private final DocumentPublicationService documentPublicationService;

    private final DeclinedDocumentClaimRepository declinedDocumentClaimRepository;

    @Value("${refresh-potential-claims.allowed}")
    private Boolean refreshClaimsAllowed;


    private static int getIndexOfNthUnmanagedAuthor(List<Integer> array, int nth) {
        if (nth <= 0) {
            throw new IllegalArgumentException("nth must be greater than 0");
        }

        int count = 0;

        for (int i = 0; i < array.size(); i++) {
            if (array.get(i) == -1) {
                count++;
                if (count == nth) {
                    return i;
                }
            }
        }

        return -1;
    }

    @Override
    public Page<DocumentPublicationIndex> findPotentialClaimsForPerson(Integer userId,
                                                                       Pageable pageable) {
        return documentPublicationIndexRepository.findByClaimerIds(
            personService.getPersonIdForUserId(userId), pageable);
    }

    @Override
    public void declineDocumentClaim(Integer userId, Integer documentId) {
        var personId = personService.getPersonIdForUserId(userId);
        var person = personService.findOne(personId);
        var document = documentPublicationService.findOne(documentId);

        declinedDocumentClaimRepository.save(new DeclinedDocumentClaim(person, document));

        var documentIndex =
            documentPublicationIndexRepository.findDocumentPublicationIndexByDatabaseId(documentId);
        documentIndex.ifPresent(index -> {
            index.getClaimerIds().remove(personId);
            documentPublicationIndexRepository.save(index);
        });
    }

    @Override
    public synchronized void claimDocument(Integer userId, Integer documentId) {
        personDocumentContributionRepository.findUnmanagedContributionsForDocument(documentId);

        var personId = personService.getPersonIdForUserId(userId);
        var person = personService.findOne(personId);
        documentPublicationIndexRepository.findDocumentPublicationIndexByDatabaseId(documentId)
            .ifPresent(documentIndex -> {
                var claimerIds = documentIndex.getClaimerIds();
                var claimerOrdinals = documentIndex.getClaimerOrdinals();

                int claimerIndex = claimerIds.indexOf(personId);
                if (claimerIndex == -1) {
                    return; // personId not found among claimers
                }

                int authorIndex = claimerOrdinals.get(claimerIndex);
                var document = documentPublicationService.findOne(documentId);

                var matchingContribution = document.getContributors().stream()
                    .filter(contribution -> contribution.getOrderNumber() == (authorIndex + 1))
                    .findFirst();

                if (matchingContribution.isPresent()) {
                    matchingContribution.get().setPerson(person);
                    documentIndex.getAuthorIds().set(authorIndex, person.getId());

                    documentIndex.getClaimerIds().clear();
                    documentIndex.getClaimerOrdinals().clear();

                    documentPublicationIndexRepository.save(documentIndex);
                    documentPublicationService.save(document);
                }
            });
    }

    @Scheduled(cron = "${refresh-potential-claims.schedule}")
    protected void updateClaimerInformation() {
        if (!refreshClaimsAllowed) {
            return;
        }

        int pageNumber = 0;
        int chunkSize = 50;
        boolean hasNextPage = true;

        var userClaimCount = new HashMap<Integer, Integer>();

        while (hasNextPage) {
            List<DocumentPublicationIndex> chunk =
                documentPublicationIndexRepository.findByAuthorIds(-1,
                    PageRequest.of(pageNumber, chunkSize)).getContent();

            chunk.forEach((document) -> {
                document.getClaimerIds().clear();
                document.getClaimerOrdinals().clear();
                var authorNames = document.getAuthorNames().split(";");
                for (int i = 0; i < authorNames.length; i++) {
                    if (document.getAuthorIds().get(i) != -1) {
                        continue;
                    }

                    var results = personService.findPeopleByNameAndEmployment(
                        List.of(authorNames[i].trim().split(" ")), Pageable.unpaged(), false, null,
                        false);
                    int authorOrderNumber = i;

                    results.getContent().forEach(person -> {
                        if (!document.getAuthorIds().contains(person.getDatabaseId()) &&
                            declinedDocumentClaimRepository.canBeClaimedByPerson(
                                person.getDatabaseId(), document.getDatabaseId())) {
                            document.getClaimerIds().add(person.getDatabaseId());
                            document.getClaimerOrdinals().add(authorOrderNumber);
                            if (Objects.nonNull(person.getUserId())) {
                                userClaimCount.put(person.getUserId(),
                                    userClaimCount.getOrDefault(person.getUserId(), 0) + 1);
                            }
                        }
                    });
                }

                documentPublicationIndexRepository.save(document);
            });

            pageNumber++;
            hasNextPage = chunk.size() == chunkSize;
        }

        userClaimCount.forEach((key, value) -> {
            notificationService.createNotification(
                NotificationFactory.contructNewPotentialClaimsFoundNotification(
                    Map.of("potentialClaimsNumber", String.valueOf(value)),
                    userService.findOne(key))
            );
        });
    }
}
