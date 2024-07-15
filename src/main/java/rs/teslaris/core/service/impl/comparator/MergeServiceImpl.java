package rs.teslaris.core.service.impl.comparator;

import java.util.List;
import java.util.Objects;
import java.util.function.BiConsumer;
import java.util.function.Function;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import rs.teslaris.core.indexmodel.DocumentPublicationIndex;
import rs.teslaris.core.indexmodel.DocumentPublicationType;
import rs.teslaris.core.indexrepository.DocumentPublicationIndexRepository;
import rs.teslaris.core.repository.document.DocumentRepository;
import rs.teslaris.core.repository.document.JournalPublicationRepository;
import rs.teslaris.core.service.interfaces.document.DocumentPublicationService;
import rs.teslaris.core.service.interfaces.document.JournalPublicationService;
import rs.teslaris.core.service.interfaces.document.JournalService;
import rs.teslaris.core.service.interfaces.merge.MergeService;
import rs.teslaris.core.service.interfaces.person.PersonService;
import rs.teslaris.core.util.exceptionhandling.exception.NotFoundException;

@Service
@RequiredArgsConstructor
@Transactional
public class MergeServiceImpl implements MergeService {

    private final JournalService journalService;

    private final JournalPublicationService journalPublicationService;

    private final JournalPublicationRepository journalPublicationRepository;

    private final DocumentPublicationIndexRepository documentPublicationIndexRepository;

    private final DocumentPublicationService documentPublicationService;

    private final DocumentRepository documentRepository;

    private final PersonService personService;


    @Override
    public void switchJournalPublicationToOtherJournal(Integer targetJournalId,
                                                       Integer publicationId) {
        performJournalPublicationSwitch(targetJournalId, publicationId);
    }

    @Override
    public void switchAllPublicationsToOtherJournal(Integer sourceId, Integer targetId) {
        processChunks(
            sourceId,
            (srcId, journalPublicationIndex) -> performJournalPublicationSwitch(targetId,
                journalPublicationIndex.getDatabaseId()),
            pageRequest -> documentPublicationIndexRepository.findByTypeAndJournalId(
                    DocumentPublicationType.JOURNAL_PUBLICATION.name(), sourceId, pageRequest)
                .getContent()
        );
    }

    @Override
    public void switchPublicationToOtherPerson(Integer sourcePersonId, Integer targetPersonId,
                                               Integer publicationId) {
        performPersonPublicationSwitch(sourcePersonId, targetPersonId, publicationId);
    }

    @Override
    public void switchAllPublicationToOtherPerson(Integer sourcePersonId, Integer targetPersonId) {
        processChunks(
            sourcePersonId,
            (srcId, personPublicationIndex) -> performPersonPublicationSwitch(srcId, targetPersonId,
                personPublicationIndex.getDatabaseId()),
            pageRequest -> documentPublicationService.findResearcherPublications(sourcePersonId,
                pageRequest).getContent()
        );
    }

    private void performPersonPublicationSwitch(Integer sourcePersonId, Integer targetPersonId,
                                                Integer publicationId) {
        var document = documentPublicationService.findDocumentById(publicationId);

        document.getContributors().forEach(contribution -> {
            if (Objects.nonNull(contribution.getPerson()) &&
                contribution.getPerson().getId().equals(sourcePersonId)) {
                contribution.setPerson(personService.findOne(targetPersonId));
            }
        });

        documentRepository.save(document);
    }

    private void performJournalPublicationSwitch(Integer targetJournalId, Integer publicationId) {
        var publication = journalPublicationRepository.findById(publicationId);

        if (publication.isEmpty()) {
            throw new NotFoundException("Publication does not exist.");
        }

        var targetJournal = journalService.findJournalById(targetJournalId);

        publication.get().setJournal(targetJournal);

        var index = documentPublicationIndexRepository.findDocumentPublicationIndexByDatabaseId(
            publicationId).orElse(new DocumentPublicationIndex());

        journalPublicationService.indexJournalPublication(publication.get(), index);
        journalPublicationRepository.save(publication.get());
    }

    private <T> void processChunks(int sourceId,
                                   BiConsumer<Integer, T> switchOperation,
                                   Function<PageRequest, List<T>> fetchChunk) {
        var pageNumber = 0;
        var chunkSize = 10;
        var hasNextPage = true;

        while (hasNextPage) {
            List<T> chunk = fetchChunk.apply(PageRequest.of(pageNumber, chunkSize));

            chunk.forEach(item -> switchOperation.accept(sourceId, item));

            pageNumber++;
            hasNextPage = chunk.size() == chunkSize;
        }
    }
}
