package rs.teslaris.core.service.impl.document;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import rs.teslaris.core.indexmodel.DocumentPublicationIndex;
import rs.teslaris.core.indexmodel.DocumentPublicationType;
import rs.teslaris.core.indexrepository.DocumentPublicationIndexRepository;
import rs.teslaris.core.model.document.Document;
import rs.teslaris.core.repository.document.DatasetRepository;
import rs.teslaris.core.repository.document.GeneticMaterialRepository;
import rs.teslaris.core.repository.document.IntangibleProductRepository;
import rs.teslaris.core.repository.document.JournalPublicationRepository;
import rs.teslaris.core.repository.document.MaterialProductRepository;
import rs.teslaris.core.repository.document.MonographPublicationRepository;
import rs.teslaris.core.repository.document.MonographRepository;
import rs.teslaris.core.repository.document.PatentRepository;
import rs.teslaris.core.repository.document.ProceedingsPublicationRepository;
import rs.teslaris.core.repository.document.ProceedingsRepository;
import rs.teslaris.core.repository.document.ThesisRepository;
import rs.teslaris.core.service.interfaces.document.DocumentLookupService;
import rs.teslaris.core.util.exceptionhandling.exception.NotFoundException;

@Service
@RequiredArgsConstructor
public class DocumentLookupServiceImpl implements DocumentLookupService {

    private final DocumentPublicationIndexRepository documentPublicationIndexRepository;

    private final JournalPublicationRepository journalPublicationRepository;

    private final ProceedingsRepository proceedingsRepository;

    private final ProceedingsPublicationRepository proceedingsPublicationRepository;

    private final MonographRepository monographRepository;

    private final MonographPublicationRepository monographPublicationRepository;

    private final ThesisRepository thesisRepository;

    private final PatentRepository patentRepository;

    private final IntangibleProductRepository intangibleProductRepository;

    private final MaterialProductRepository materialProductRepository;

    private final DatasetRepository datasetRepository;

    private final GeneticMaterialRepository geneticMaterialRepository;


    @Override
    public Document fastDocumentLookup(Integer documentId) {
        var index = getDocumentIndex(documentId);

        if (index.getType().equals(DocumentPublicationType.JOURNAL_PUBLICATION.name())) {
            return journalPublicationRepository.findById(documentId)
                .orElseThrow(this::throwNotFoundException);
        } else if (index.getType()
            .equals(DocumentPublicationType.PROCEEDINGS_PUBLICATION.name())) {
            return proceedingsPublicationRepository.findById(documentId)
                .orElseThrow(this::throwNotFoundException);
        } else if (index.getType().equals(DocumentPublicationType.MONOGRAPH.name())) {
            return monographRepository.findById(documentId)
                .orElseThrow(this::throwNotFoundException);
        } else if (index.getType()
            .equals(DocumentPublicationType.MONOGRAPH_PUBLICATION.name())) {
            return monographPublicationRepository.findById(documentId)
                .orElseThrow(this::throwNotFoundException);
        } else if (index.getType().equals(DocumentPublicationType.THESIS.name())) {
            return thesisRepository.findById(documentId)
                .orElseThrow(this::throwNotFoundException);
        } else if (index.getType().equals(DocumentPublicationType.PROCEEDINGS.name())) {
            return proceedingsRepository.findById(documentId)
                .orElseThrow(this::throwNotFoundException);
        } else if (index.getType().equals(DocumentPublicationType.PATENT.name())) {
            return patentRepository.findById(documentId)
                .orElseThrow(this::throwNotFoundException);
        } else if (index.getType()
            .equals(DocumentPublicationType.INTANGIBLE_PRODUCT.name())) {
            return intangibleProductRepository.findById(documentId)
                .orElseThrow(this::throwNotFoundException);
        } else if (index.getType().equals(DocumentPublicationType.DATASET.name())) {
            return datasetRepository.findById(documentId)
                .orElseThrow(this::throwNotFoundException);
        } else if (index.getType().equals(DocumentPublicationType.MATERIAL_PRODUCT.name())) {
            return materialProductRepository.findById(documentId)
                .orElseThrow(this::throwNotFoundException);
        } else if (index.getType().equals(DocumentPublicationType.GENETIC_MATERIAL.name())) {
            return geneticMaterialRepository.findById(documentId)
                .orElseThrow(this::throwNotFoundException);
        }

        throw throwNotFoundException();
    }

    @Override
    public DocumentPublicationIndex getDocumentIndex(Integer documentId) {
        return documentPublicationIndexRepository.findDocumentPublicationIndexByDatabaseId(
                documentId)
            .orElseThrow(this::throwNotFoundException);
    }

    private NotFoundException throwNotFoundException() {
        return new NotFoundException("Document with given ID is not present in the database.");
    }
}
