package rs.teslaris.core.service.impl.document;

import co.elastic.clients.elasticsearch._types.query_dsl.BoolQuery;
import co.elastic.clients.elasticsearch._types.query_dsl.Query;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import rs.teslaris.core.indexmodel.DocumentPublicationIndex;
import rs.teslaris.core.indexmodel.PersonIndex;
import rs.teslaris.core.indexrepository.DocumentPublicationIndexRepository;
import rs.teslaris.core.model.person.DeclinedDocumentClaim;
import rs.teslaris.core.model.person.Person;
import rs.teslaris.core.model.person.PersonName;
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
public class DocumentClaimingServiceImpl implements DocumentClaimingService {

    private final DocumentPublicationIndexRepository documentPublicationIndexRepository;

    private final SearchService<PersonIndex> personSearchService;

    private final PersonService personService;

    private final PersonDocumentContributionRepository personDocumentContributionRepository;

    private final NotificationService notificationService;

    private final UserService userService;

    private final DocumentPublicationService documentPublicationService;

    private final DeclinedDocumentClaimRepository declinedDocumentClaimRepository;


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
    public void claimDocument(Integer userId, Integer documentId) {
        var personId = personService.getPersonIdForUserId(userId);
        var person = personService.findOne(personId);

        var contributionsToClaim =
            personDocumentContributionRepository.findUnmanagedContributionsForDocument(documentId);

        var counter = 1;
        for (var contribution : contributionsToClaim) {
            var displayName = contribution.getAffiliationStatement().getDisplayPersonName();

            if (isMatchingName(person, displayName)) {
                contribution.setPerson(person);
                personDocumentContributionRepository.save(contribution);

                var document =
                    documentPublicationIndexRepository.findDocumentPublicationIndexByDatabaseId(
                        documentId);
                if (document.isPresent()) {
                    var authorIndex =
                        getIndexOfNthUnmanagedAuthor(document.get().getAuthorIds(), counter);
                    if (authorIndex >= 0) { // should always be true
                        document.get().getAuthorIds().set(authorIndex, personId);
                    }
                    document.get().getClaimerIds().clear();
                    documentPublicationIndexRepository.save(document.get());
                }
                break;
            }
            counter++;
        }
    }

    private boolean isMatchingName(Person person, PersonName displayName) {
        if (person.getName().toString().equals(displayName.toString())) {
            return true;
        }
        return person.getOtherNames().stream()
            .anyMatch(otherName -> otherName.toString().equals(displayName.toString()));
    }

    @Scheduled(cron = "${refresh-potential-claims.schedule}")
    protected void updateClaimerInformation() {
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
                var authorNames = document.getAuthorNames().split(";");
                for (int i = 0; i < authorNames.length; i++) {
                    if (document.getAuthorIds().get(i) != -1) {
                        continue;
                    }

                    var results = personSearchService.runQuery(buildNameQuery(authorNames[i]),
                        Pageable.unpaged(), PersonIndex.class, "person");
                    results.getContent().forEach(person -> {
                        if (declinedDocumentClaimRepository.canBeClaimedByPerson(
                            person.getDatabaseId(), document.getDatabaseId())) {
                            document.getClaimerIds().add(person.getDatabaseId());
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

    private Query buildNameQuery(String authorName) {
        return BoolQuery.of(q -> q
            .must(mb -> mb.matchPhrase(m -> m.field("name").query(authorName)))
        )._toQuery();
    }
}
