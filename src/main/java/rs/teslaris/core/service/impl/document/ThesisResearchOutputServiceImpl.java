package rs.teslaris.core.service.impl.document;

import java.util.Collections;
import java.util.Objects;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;
import java.util.stream.Collectors;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import rs.teslaris.core.annotation.Traceable;
import rs.teslaris.core.indexmodel.DocumentPublicationIndex;
import rs.teslaris.core.indexrepository.DocumentPublicationIndexRepository;
import rs.teslaris.core.model.document.Document;
import rs.teslaris.core.model.document.Thesis;
import rs.teslaris.core.model.document.ThesisResearchOutput;
import rs.teslaris.core.repository.document.ThesisResearchOutputRepository;
import rs.teslaris.core.service.interfaces.document.DocumentPublicationService;
import rs.teslaris.core.service.interfaces.document.ThesisResearchOutputService;
import rs.teslaris.core.service.interfaces.document.ThesisService;
import rs.teslaris.core.util.exceptionhandling.exception.NotFoundException;
import rs.teslaris.core.util.exceptionhandling.exception.ThesisException;

@Service
@RequiredArgsConstructor
@Transactional
@Traceable
public class ThesisResearchOutputServiceImpl implements ThesisResearchOutputService {

    private final ThesisResearchOutputRepository thesisResearchOutputRepository;

    private final ThesisService thesisService;

    private final DocumentPublicationService documentPublicationService;

    private final DocumentPublicationIndexRepository documentPublicationIndexRepository;

    private final Lock lock = new ReentrantLock();


    @Override
    public Page<DocumentPublicationIndex> readResearchOutputsForThesis(Integer thesisId,
                                                                       Pageable pageable) {
        var thesis =
            documentPublicationIndexRepository.findDocumentPublicationIndexByDatabaseId(thesisId);

        if (thesis.isEmpty()) {
            throw new NotFoundException("Thesis with ID " + thesisId + " does not exist.");
        }

        return documentPublicationIndexRepository.findDocumentPublicationIndexByDatabaseIdIn(
            thesis.get()
                .getResearchOutputIds(), pageable);
    }

    @Override
    public void addResearchOutput(Integer thesisId, Integer researchOutputId) {
        validateIds(thesisId, researchOutputId);

        var thesis = thesisService.getThesisById(thesisId);
        var document = documentPublicationService.findOne(researchOutputId);

        validateAuthorship(thesis, document);

        var newResearchOutput = new ThesisResearchOutput(thesis, document);
        thesisResearchOutputRepository.save(newResearchOutput);

        updateDocumentPublicationIndex(thesisId, researchOutputId);
    }

    private void validateIds(Integer thesisId, Integer researchOutputId) {
        if (Objects.equals(thesisId, researchOutputId)) {
            throw new ThesisException("cantBeOutputToItselfMessage");
        }

        if (thesisResearchOutputRepository.findByThesisIdAndResearchOutputId(thesisId,
            researchOutputId).isPresent()) {
            throw new ThesisException("publicationListedInOutputsMessage");
        }
    }

    private void validateAuthorship(Thesis thesis, Document document) {
        var thesisAuthors = thesis.getContributors().stream()
            .filter(contribution -> Objects.nonNull(contribution.getPerson()))
            .map(contribution -> contribution.getPerson().getId())
            .collect(Collectors.toSet());

        var documentAuthors = document.getContributors().stream()
            .filter(contribution -> Objects.nonNull(contribution.getPerson()))
            .map(contribution -> contribution.getPerson().getId())
            .collect(Collectors.toSet());

        if (Collections.disjoint(thesisAuthors, documentAuthors)) {
            throw new ThesisException("notYourPublicationMessage");
        }
    }

    private void updateDocumentPublicationIndex(Integer thesisId, Integer researchOutputId) {
        lock.lock();
        try {
            documentPublicationIndexRepository.findDocumentPublicationIndexByDatabaseId(thesisId)
                .ifPresent(thesisIndex -> {
                    thesisIndex.getResearchOutputIds().add(researchOutputId);
                    documentPublicationIndexRepository.save(thesisIndex);
                });
        } finally {
            lock.unlock();
        }
    }

    @Override
    public void removeResearchOutput(Integer thesisId, Integer researchOutputId) {
        var existingResearchOutput =
            thesisResearchOutputRepository.findByThesisIdAndResearchOutputId(thesisId,
                researchOutputId);

        existingResearchOutput.ifPresentOrElse(researchOutput -> {
                thesisResearchOutputRepository.delete(researchOutput);

                lock.lock();
                try {
                    documentPublicationIndexRepository.findDocumentPublicationIndexByDatabaseId(
                            thesisId)
                        .ifPresent(thesisIndex -> {
                            thesisIndex.getResearchOutputIds().remove(researchOutputId);
                            documentPublicationIndexRepository.save(thesisIndex);
                        });
                } finally {
                    lock.unlock();
                }
            },
            () -> {
                throw new NotFoundException("Document with ID " + researchOutputId +
                    " is not listed in research outputs of thesis with ID " + thesisId);
            });
    }
}
