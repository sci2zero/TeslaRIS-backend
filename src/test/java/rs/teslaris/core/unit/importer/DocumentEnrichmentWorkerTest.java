package rs.teslaris.core.unit.importer;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import java.util.Optional;
import org.junit.jupiter.api.Test;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.springframework.boot.test.context.SpringBootTest;
import rs.teslaris.core.indexmodel.DocumentPublicationIndex;
import rs.teslaris.core.indexrepository.DocumentPublicationIndexRepository;
import rs.teslaris.core.model.document.JournalPublication;
import rs.teslaris.core.service.interfaces.commontypes.LanguageTagService;
import rs.teslaris.core.service.interfaces.document.DocumentLookupService;
import rs.teslaris.core.service.interfaces.document.DocumentPublicationService;
import rs.teslaris.importer.model.common.DocumentImport;
import rs.teslaris.importer.service.impl.worker.DocumentEnrichmentWorker;

@SpringBootTest
class DocumentEnrichmentWorkerTest {

    @Mock
    private DocumentLookupService documentLookupService;

    @Mock
    private LanguageTagService languageTagService;

    @Mock
    private DocumentPublicationService documentPublicationService;

    @Mock
    private DocumentPublicationIndexRepository documentPublicationIndexRepository;

    @InjectMocks
    private DocumentEnrichmentWorker worker;


    @Test
    void shouldReturnWhenIndexNotFound() {
        var documentId = 1;

        when(documentPublicationIndexRepository
            .findDocumentPublicationIndexByDatabaseId(documentId))
            .thenReturn(Optional.empty());

        worker.enrichDocumentMetadata(documentId, new DocumentImport());

        verifyNoInteractions(documentLookupService);
        verify(documentPublicationIndexRepository, never()).save(any());
    }

    @Test
    void shouldEnrichSimpleFieldsWhenMissing() {
        var documentId = 1;

        var index = new DocumentPublicationIndex();
        var document = new JournalPublication();
        var importObj = new DocumentImport();

        importObj.setScopusId("SC123");
        importObj.setOpenAlexId("OA123");
        importObj.setWebOfScienceId("WOS123");

        when(documentPublicationIndexRepository
            .findDocumentPublicationIndexByDatabaseId(documentId))
            .thenReturn(Optional.of(index));

        when(documentLookupService.fastDocumentLookup(index))
            .thenReturn(document);

        worker.enrichDocumentMetadata(documentId, importObj);

        assertEquals("SC123", document.getScopusId());
        assertEquals("OA123", document.getOpenAlexId());
        assertEquals("WOS123", document.getWebOfScienceId());

        verify(documentPublicationService).save(document);
        verify(documentPublicationIndexRepository).save(index);
    }

    @Test
    void shouldEnrichPrintedPageableFields() {
        var documentId = 1;

        var index = new DocumentPublicationIndex();
        var document = new JournalPublication();
        var importObj = new DocumentImport();

        importObj.setStartPage("1");
        importObj.setEndPage("10");
        importObj.setNumberOfPages(9);
        importObj.setArticleNumber("A1");

        when(documentPublicationIndexRepository
            .findDocumentPublicationIndexByDatabaseId(documentId))
            .thenReturn(Optional.of(index));

        when(documentLookupService.fastDocumentLookup(index))
            .thenReturn(document);

        worker.enrichDocumentMetadata(documentId, importObj);

        assertEquals("1", document.getStartPage());
        assertEquals("10", document.getEndPage());
        assertEquals(9, document.getNumberOfPages());
        assertEquals("A1", document.getArticleNumber());
        assertEquals(9, index.getNumberOfPages());

        verify(documentPublicationService).save(document);
        verify(documentPublicationIndexRepository).save(index);
    }

    @Test
    void shouldEnrichJournalSpecificFields() {
        var documentId = 1;

        var index = new DocumentPublicationIndex();
        var document = new JournalPublication();
        var importObj = new DocumentImport();

        importObj.setVolume("12");
        importObj.setIssue("3");

        when(documentPublicationIndexRepository
            .findDocumentPublicationIndexByDatabaseId(documentId))
            .thenReturn(Optional.of(index));

        when(documentLookupService.fastDocumentLookup(index))
            .thenReturn(document);

        worker.enrichDocumentMetadata(documentId, importObj);

        assertEquals("12", document.getVolume());
        assertEquals("3", document.getIssue());

        verify(documentPublicationService).save(document);
        verify(documentPublicationIndexRepository).save(index);
    }
}
