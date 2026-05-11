package rs.teslaris.core.unit;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.util.Optional;
import org.junit.jupiter.api.Test;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.springframework.boot.test.context.SpringBootTest;
import rs.teslaris.core.indexmodel.DocumentPublicationIndex;
import rs.teslaris.core.indexmodel.DocumentPublicationType;
import rs.teslaris.core.indexrepository.DocumentPublicationIndexRepository;
import rs.teslaris.core.model.document.Dataset;
import rs.teslaris.core.model.document.GeneticMaterial;
import rs.teslaris.core.model.document.IntangibleProduct;
import rs.teslaris.core.model.document.JournalPublication;
import rs.teslaris.core.model.document.MaterialProduct;
import rs.teslaris.core.model.document.Monograph;
import rs.teslaris.core.model.document.MonographPublication;
import rs.teslaris.core.model.document.Patent;
import rs.teslaris.core.model.document.PerformanceRelatedOutput;
import rs.teslaris.core.model.document.Proceedings;
import rs.teslaris.core.model.document.ProceedingsPublication;
import rs.teslaris.core.model.document.Thesis;
import rs.teslaris.core.repository.document.DatasetRepository;
import rs.teslaris.core.repository.document.GeneticMaterialRepository;
import rs.teslaris.core.repository.document.IntangibleProductRepository;
import rs.teslaris.core.repository.document.JournalPublicationRepository;
import rs.teslaris.core.repository.document.MaterialProductRepository;
import rs.teslaris.core.repository.document.MonographPublicationRepository;
import rs.teslaris.core.repository.document.MonographRepository;
import rs.teslaris.core.repository.document.PatentRepository;
import rs.teslaris.core.repository.document.PerformanceRelatedOutputRepository;
import rs.teslaris.core.repository.document.ProceedingsPublicationRepository;
import rs.teslaris.core.repository.document.ProceedingsRepository;
import rs.teslaris.core.repository.document.ThesisRepository;
import rs.teslaris.core.service.impl.document.DocumentLookupServiceImpl;
import rs.teslaris.core.util.exceptionhandling.exception.NotFoundException;

@SpringBootTest
class DocumentLookupServiceTest {

    @Mock
    private DocumentPublicationIndexRepository documentPublicationIndexRepository;

    @Mock
    private JournalPublicationRepository journalPublicationRepository;

    @Mock
    private ProceedingsRepository proceedingsRepository;

    @Mock
    private ProceedingsPublicationRepository proceedingsPublicationRepository;

    @Mock
    private MonographRepository monographRepository;

    @Mock
    private MonographPublicationRepository monographPublicationRepository;

    @Mock
    private ThesisRepository thesisRepository;

    @Mock
    private PatentRepository patentRepository;

    @Mock
    private IntangibleProductRepository intangibleProductRepository;

    @Mock
    private MaterialProductRepository materialProductRepository;

    @Mock
    private DatasetRepository datasetRepository;

    @Mock
    private GeneticMaterialRepository geneticMaterialRepository;

    @Mock
    private PerformanceRelatedOutputRepository performanceRelatedOutputRepository;

    @InjectMocks
    private DocumentLookupServiceImpl documentLookupService;

    @Test
    public void shouldReturnDocumentWhenFoundForAllDocumentTypes() {
        // Given
        var documentId = 1;
        var journalIndex = createIndex(DocumentPublicationType.JOURNAL_PUBLICATION);
        var journalDocument = new JournalPublication();
        var proceedingsIndex = createIndex(DocumentPublicationType.PROCEEDINGS_PUBLICATION);
        var proceedingsDocument = new ProceedingsPublication();
        var monographIndex = createIndex(DocumentPublicationType.MONOGRAPH);
        var monographDocument = new Monograph();
        var monographPubIndex = createIndex(DocumentPublicationType.MONOGRAPH_PUBLICATION);
        var monographPubDocument = new MonographPublication();
        var thesisIndex = createIndex(DocumentPublicationType.THESIS);
        var thesisDocument = new Thesis();
        var procIndex = createIndex(DocumentPublicationType.PROCEEDINGS);
        var procDocument = new Proceedings();
        var patentIndex = createIndex(DocumentPublicationType.PATENT);
        var patentDocument = new Patent();
        var intangibleIndex = createIndex(DocumentPublicationType.INTANGIBLE_PRODUCT);
        var intangibleDocument = new IntangibleProduct();
        var datasetIndex = createIndex(DocumentPublicationType.DATASET);
        var datasetDocument = new Dataset();
        var materialIndex = createIndex(DocumentPublicationType.MATERIAL_PRODUCT);
        var materialDocument = new MaterialProduct();
        var geneticIndex = createIndex(DocumentPublicationType.GENETIC_MATERIAL);
        var geneticDocument = new GeneticMaterial();
        var performanceIndex = createIndex(DocumentPublicationType.PERFORMANCE_RELATED_OUTPUT);
        var performanceDocument = new PerformanceRelatedOutput();

        when(
            documentPublicationIndexRepository.findDocumentPublicationIndexByDatabaseId(documentId))
            .thenReturn(Optional.of(journalIndex))
            .thenReturn(Optional.of(proceedingsIndex))
            .thenReturn(Optional.of(monographIndex))
            .thenReturn(Optional.of(monographPubIndex))
            .thenReturn(Optional.of(thesisIndex))
            .thenReturn(Optional.of(procIndex))
            .thenReturn(Optional.of(patentIndex))
            .thenReturn(Optional.of(intangibleIndex))
            .thenReturn(Optional.of(datasetIndex))
            .thenReturn(Optional.of(materialIndex))
            .thenReturn(Optional.of(geneticIndex))
            .thenReturn(Optional.of(performanceIndex));

        when(journalPublicationRepository.findById(documentId)).thenReturn(
            Optional.of(journalDocument));
        when(proceedingsPublicationRepository.findById(documentId)).thenReturn(
            Optional.of(proceedingsDocument));
        when(monographRepository.findById(documentId)).thenReturn(Optional.of(monographDocument));
        when(monographPublicationRepository.findById(documentId)).thenReturn(
            Optional.of(monographPubDocument));
        when(thesisRepository.findById(documentId)).thenReturn(Optional.of(thesisDocument));
        when(proceedingsRepository.findById(documentId)).thenReturn(Optional.of(procDocument));
        when(patentRepository.findById(documentId)).thenReturn(Optional.of(patentDocument));
        when(intangibleProductRepository.findById(documentId)).thenReturn(
            Optional.of(intangibleDocument));
        when(datasetRepository.findById(documentId)).thenReturn(Optional.of(datasetDocument));
        when(materialProductRepository.findById(documentId)).thenReturn(
            Optional.of(materialDocument));
        when(geneticMaterialRepository.findById(documentId)).thenReturn(
            Optional.of(geneticDocument));
        when(performanceRelatedOutputRepository.findById(documentId)).thenReturn(
            Optional.of(performanceDocument));

        // When & Then
        assertThat(documentLookupService.fastDocumentLookup(documentId)).isEqualTo(journalDocument);
        assertThat(documentLookupService.fastDocumentLookup(documentId)).isEqualTo(
            proceedingsDocument);
        assertThat(documentLookupService.fastDocumentLookup(documentId)).isEqualTo(
            monographDocument);
        assertThat(documentLookupService.fastDocumentLookup(documentId)).isEqualTo(
            monographPubDocument);
        assertThat(documentLookupService.fastDocumentLookup(documentId)).isEqualTo(thesisDocument);
        assertThat(documentLookupService.fastDocumentLookup(documentId)).isEqualTo(procDocument);
        assertThat(documentLookupService.fastDocumentLookup(documentId)).isEqualTo(patentDocument);
        assertThat(documentLookupService.fastDocumentLookup(documentId)).isEqualTo(
            intangibleDocument);
        assertThat(documentLookupService.fastDocumentLookup(documentId)).isEqualTo(datasetDocument);
        assertThat(documentLookupService.fastDocumentLookup(documentId)).isEqualTo(
            materialDocument);
        assertThat(documentLookupService.fastDocumentLookup(documentId)).isEqualTo(geneticDocument);
        assertThat(documentLookupService.fastDocumentLookup(documentId)).isEqualTo(
            performanceDocument);
    }

    @Test
    public void shouldReturnDocumentWhenFoundForAllDocumentTypesByIndex() {
        // Given
        var documentId = 1;
        var documentIndex = mock(DocumentPublicationIndex.class);
        when(documentIndex.getType())
            .thenReturn(DocumentPublicationType.JOURNAL_PUBLICATION.name())
            .thenReturn(DocumentPublicationType.PROCEEDINGS_PUBLICATION.name())
            .thenReturn(DocumentPublicationType.MONOGRAPH.name())
            .thenReturn(DocumentPublicationType.MONOGRAPH_PUBLICATION.name())
            .thenReturn(DocumentPublicationType.THESIS.name())
            .thenReturn(DocumentPublicationType.PROCEEDINGS.name())
            .thenReturn(DocumentPublicationType.PATENT.name())
            .thenReturn(DocumentPublicationType.INTANGIBLE_PRODUCT.name())
            .thenReturn(DocumentPublicationType.DATASET.name())
            .thenReturn(DocumentPublicationType.MATERIAL_PRODUCT.name())
            .thenReturn(DocumentPublicationType.GENETIC_MATERIAL.name())
            .thenReturn(DocumentPublicationType.PERFORMANCE_RELATED_OUTPUT.name());
        when(documentIndex.getDatabaseId()).thenReturn(documentId);

        var journalDocument = new JournalPublication();
        var proceedingsDocument = new ProceedingsPublication();
        var monographDocument = new Monograph();
        var monographPubDocument = new MonographPublication();
        var thesisDocument = new Thesis();
        var procDocument = new Proceedings();
        var patentDocument = new Patent();
        var intangibleDocument = new IntangibleProduct();
        var datasetDocument = new Dataset();
        var materialDocument = new MaterialProduct();
        var geneticDocument = new GeneticMaterial();
        var performanceRelatedOutput = new PerformanceRelatedOutput();

        when(journalPublicationRepository.findById(documentId)).thenReturn(
            Optional.of(journalDocument));
        when(proceedingsPublicationRepository.findById(documentId)).thenReturn(
            Optional.of(proceedingsDocument));
        when(monographRepository.findById(documentId)).thenReturn(Optional.of(monographDocument));
        when(monographPublicationRepository.findById(documentId)).thenReturn(
            Optional.of(monographPubDocument));
        when(thesisRepository.findById(documentId)).thenReturn(Optional.of(thesisDocument));
        when(proceedingsRepository.findById(documentId)).thenReturn(Optional.of(procDocument));
        when(patentRepository.findById(documentId)).thenReturn(Optional.of(patentDocument));
        when(intangibleProductRepository.findById(documentId)).thenReturn(
            Optional.of(intangibleDocument));
        when(datasetRepository.findById(documentId)).thenReturn(Optional.of(datasetDocument));
        when(materialProductRepository.findById(documentId)).thenReturn(
            Optional.of(materialDocument));
        when(geneticMaterialRepository.findById(documentId)).thenReturn(
            Optional.of(geneticDocument));
        when(performanceRelatedOutputRepository.findById(documentId)).thenReturn(
            Optional.of(performanceRelatedOutput));

        // When & Then
        assertThat(documentLookupService.fastDocumentLookup(documentIndex)).isEqualTo(
            journalDocument);
        assertThat(documentLookupService.fastDocumentLookup(documentIndex)).isEqualTo(
            proceedingsDocument);
        assertThat(documentLookupService.fastDocumentLookup(documentIndex)).isEqualTo(
            monographDocument);
        assertThat(documentLookupService.fastDocumentLookup(documentIndex)).isEqualTo(
            monographPubDocument);
        assertThat(documentLookupService.fastDocumentLookup(documentIndex)).isEqualTo(
            thesisDocument);
        assertThat(documentLookupService.fastDocumentLookup(documentIndex)).isEqualTo(procDocument);
        assertThat(documentLookupService.fastDocumentLookup(documentIndex)).isEqualTo(
            patentDocument);
        assertThat(documentLookupService.fastDocumentLookup(documentIndex)).isEqualTo(
            intangibleDocument);
        assertThat(documentLookupService.fastDocumentLookup(documentIndex)).isEqualTo(
            datasetDocument);
        assertThat(documentLookupService.fastDocumentLookup(documentIndex)).isEqualTo(
            materialDocument);
        assertThat(documentLookupService.fastDocumentLookup(documentIndex)).isEqualTo(
            geneticDocument);
        assertThat(documentLookupService.fastDocumentLookup(documentIndex)).isEqualTo(
            performanceRelatedOutput);
    }

    @Test
    public void shouldThrowNotFoundExceptionWhenDocumentNotFoundForAllDocumentTypes() {
        // Given
        var documentId = 1;
        var journalIndex = createIndex(DocumentPublicationType.JOURNAL_PUBLICATION);
        var proceedingsIndex = createIndex(DocumentPublicationType.PROCEEDINGS_PUBLICATION);
        var monographIndex = createIndex(DocumentPublicationType.MONOGRAPH);
        var monographPubIndex = createIndex(DocumentPublicationType.MONOGRAPH_PUBLICATION);
        var thesisIndex = createIndex(DocumentPublicationType.THESIS);
        var procIndex = createIndex(DocumentPublicationType.PROCEEDINGS);
        var patentIndex = createIndex(DocumentPublicationType.PATENT);
        var intangibleIndex = createIndex(DocumentPublicationType.INTANGIBLE_PRODUCT);
        var datasetIndex = createIndex(DocumentPublicationType.DATASET);
        var materialIndex = createIndex(DocumentPublicationType.MATERIAL_PRODUCT);
        var geneticIndex = createIndex(DocumentPublicationType.GENETIC_MATERIAL);
        var performanceIndex = createIndex(DocumentPublicationType.PERFORMANCE_RELATED_OUTPUT);

        when(
            documentPublicationIndexRepository.findDocumentPublicationIndexByDatabaseId(documentId))
            .thenReturn(Optional.of(journalIndex))
            .thenReturn(Optional.of(proceedingsIndex))
            .thenReturn(Optional.of(monographIndex))
            .thenReturn(Optional.of(monographPubIndex))
            .thenReturn(Optional.of(thesisIndex))
            .thenReturn(Optional.of(procIndex))
            .thenReturn(Optional.of(patentIndex))
            .thenReturn(Optional.of(intangibleIndex))
            .thenReturn(Optional.of(datasetIndex))
            .thenReturn(Optional.of(materialIndex))
            .thenReturn(Optional.of(geneticIndex))
            .thenReturn(Optional.of(performanceIndex));

        when(journalPublicationRepository.findById(documentId)).thenReturn(Optional.empty());
        when(proceedingsPublicationRepository.findById(documentId)).thenReturn(Optional.empty());
        when(monographRepository.findById(documentId)).thenReturn(Optional.empty());
        when(monographPublicationRepository.findById(documentId)).thenReturn(Optional.empty());
        when(thesisRepository.findById(documentId)).thenReturn(Optional.empty());
        when(proceedingsRepository.findById(documentId)).thenReturn(Optional.empty());
        when(patentRepository.findById(documentId)).thenReturn(Optional.empty());
        when(intangibleProductRepository.findById(documentId)).thenReturn(Optional.empty());
        when(datasetRepository.findById(documentId)).thenReturn(Optional.empty());
        when(materialProductRepository.findById(documentId)).thenReturn(Optional.empty());
        when(geneticMaterialRepository.findById(documentId)).thenReturn(Optional.empty());
        when(performanceRelatedOutputRepository.findById(documentId)).thenReturn(Optional.empty());

        // When & Then
        assertThatThrownBy(() -> documentLookupService.fastDocumentLookup(documentId))
            .isInstanceOf(NotFoundException.class);
        assertThatThrownBy(() -> documentLookupService.fastDocumentLookup(documentId))
            .isInstanceOf(NotFoundException.class);
        assertThatThrownBy(() -> documentLookupService.fastDocumentLookup(documentId))
            .isInstanceOf(NotFoundException.class);
        assertThatThrownBy(() -> documentLookupService.fastDocumentLookup(documentId))
            .isInstanceOf(NotFoundException.class);
        assertThatThrownBy(() -> documentLookupService.fastDocumentLookup(documentId))
            .isInstanceOf(NotFoundException.class);
        assertThatThrownBy(() -> documentLookupService.fastDocumentLookup(documentId))
            .isInstanceOf(NotFoundException.class);
        assertThatThrownBy(() -> documentLookupService.fastDocumentLookup(documentId))
            .isInstanceOf(NotFoundException.class);
        assertThatThrownBy(() -> documentLookupService.fastDocumentLookup(documentId))
            .isInstanceOf(NotFoundException.class);
        assertThatThrownBy(() -> documentLookupService.fastDocumentLookup(documentId))
            .isInstanceOf(NotFoundException.class);
        assertThatThrownBy(() -> documentLookupService.fastDocumentLookup(documentId))
            .isInstanceOf(NotFoundException.class);
        assertThatThrownBy(() -> documentLookupService.fastDocumentLookup(documentId))
            .isInstanceOf(NotFoundException.class);
        assertThatThrownBy(() -> documentLookupService.fastDocumentLookup(documentId))
            .isInstanceOf(NotFoundException.class);
    }

    @Test
    public void shouldThrowNotFoundExceptionWhenIndexNotFound() {
        // Given
        var documentId = 1;
        when(
            documentPublicationIndexRepository.findDocumentPublicationIndexByDatabaseId(documentId))
            .thenReturn(Optional.empty());

        // When & Then
        assertThatThrownBy(() -> documentLookupService.fastDocumentLookup(documentId))
            .isInstanceOf(NotFoundException.class)
            .hasMessage("Document with given ID is not present in the database.");
    }

    private DocumentPublicationIndex createIndex(DocumentPublicationType type) {
        var index = new DocumentPublicationIndex();
        index.setType(type.name());
        index.setDatabaseId(1);
        return index;
    }
}
