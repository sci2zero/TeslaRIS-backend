package rs.teslaris.core.unit;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mockStatic;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.time.ZoneId;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.query.Query;
import org.springframework.test.util.ReflectionTestUtils;
import rs.teslaris.core.exporter.model.common.ExportDocument;
import rs.teslaris.core.exporter.model.common.ExportPublicationType;
import rs.teslaris.core.exporter.model.converter.ExportConverterBase;
import rs.teslaris.core.exporter.service.impl.OutboundExportServiceImpl;
import rs.teslaris.core.exporter.util.ExportHandlersConfigurationLoader;
import rs.teslaris.core.importer.model.oaipmh.common.OAIPMHResponse;
import rs.teslaris.core.util.exceptionhandling.exception.LoadingException;

@SpringBootTest
public class OutboundExportServiceTest {

    @Mock
    private MongoTemplate mongoTemplate;

    @InjectMocks
    private OutboundExportServiceImpl outboundExportService;

    @BeforeAll
    public static void setup() {
        var handler =
            new ExportHandlersConfigurationLoader.Handler("handler", "1", "name", "description",
                List.of(new ExportHandlersConfigurationLoader.Set("openaire_cris_publications",
                    "OpenAIRE_CRIS_publications", "Publications", "ExportDocument",
                    "PROCEEDINGS,PROCEEDINGS_PUBLICATION,MONOGRAPH,MONOGRAPH_PUBLICATION,JOURNAL,JOURNAL_PUBLICATION,THESIS",
                    null)), List.of("oai_cerif_openaire", "oai_dim"), false, null, Map.of());

        var mocked = mockStatic(ExportHandlersConfigurationLoader.class);
        mocked.when(() -> ExportHandlersConfigurationLoader.getHandlerByIdentifier("handler"))
            .thenReturn(
                Optional.of(handler));
    }

    @BeforeEach
    public void setUp() {
        ReflectionTestUtils.setField(ExportConverterBase.class, "repositoryName", "CRIS UNS");
        ReflectionTestUtils.setField(ExportConverterBase.class, "baseFrontendUrl",
            "test://test.test");
        ReflectionTestUtils.setField(ExportConverterBase.class, "clientLanguages",
            new ArrayList<>());
        ReflectionTestUtils.setField(outboundExportService, "baseUrl", "test://test.test");
        ReflectionTestUtils.setField(outboundExportService, "repositoryName", "CRIS UNS");
        ReflectionTestUtils.setField(outboundExportService, "adminEmail", "admin@test.com");
    }

    @Test
    void shouldListRequestedRecordsWhenPresentedWithValidRequest() {
        // Given
        var document = new ExportDocument();
        document.setOpenAccess(true);
        document.setType(ExportPublicationType.JOURNAL_PUBLICATION);
        when(mongoTemplate.find(any(Query.class), eq(ExportDocument.class)))
            .thenReturn(List.of(document));
        when(mongoTemplate.count(any(Query.class), eq(ExportDocument.class))).thenReturn(1L);


        // When
        var result = outboundExportService.listRequestedRecords("handler", "oai_dim",
            "2023-01-01", "2023-12-31", "openaire_cris_publications", new OAIPMHResponse(), 0,
            false);

        // Then
        assertNotNull(result);
        assertFalse(result.getRecords().isEmpty());
    }

    @Test
    void shouldReturnFormatErrorListRequestedRecordsWhenInvalidMetadataPrefix() {
        // Given
        var response = new OAIPMHResponse();

        // When
        var result = outboundExportService.listRequestedRecords("handler", "INVALID_PREFIX",
            "2023-01-01", "2023-12-31", "openaire_cris_publications", response, 0, false);

        // Then
        assertNull(result);
        assertEquals("cannotDisseminateFormat", response.getError().getCode());
    }

    @Test
    void shouldListRequestedRecordWhenPresentedWithValidRequest() {
        // Given
        var document = new ExportDocument();
        document.setOpenAccess(true);
        document.setType(ExportPublicationType.JOURNAL_PUBLICATION);
        when(mongoTemplate.findOne(any(Query.class), eq(ExportDocument.class)))
            .thenReturn(document);

        var response = new OAIPMHResponse();

        // When
        var result = outboundExportService.listRequestedRecord("handler", "oai_dim",
            "oai:repo:123", response);

        // Then
        assertNotNull(result);
        assertNotNull(result.getRecord());
    }

    @Test
    void shouldReturnNotFoundErrorWhenRecordNotFound() {
        // Given
        when(mongoTemplate.findOne(any(Query.class), eq(ExportDocument.class)))
            .thenReturn(null);

        var response = new OAIPMHResponse();

        // When
        var result = outboundExportService.listRequestedRecord("handler", "oai_cerif_openaire",
            "oai:repo:123", response);

        // Then
        assertNull(result);
        assertEquals("idDoesNotExist", response.getError().getCode());
    }

    @Test
    void shouldIdentifyHandler() {
        // Given
        var earliestDocument = new ExportDocument();
        var earliestDate = new Date();
        earliestDocument.setLastUpdated(earliestDate);
        when(mongoTemplate.findOne(any(Query.class), eq(ExportDocument.class))).thenReturn(
            earliestDocument);

        // When
        var result = outboundExportService.identifyHandler("handler");

        // Then
        assertNotNull(result);
        assertEquals("test://test.test/api/export/handler", result.getBaseURL());
        assertEquals("CRIS UNS", result.getRepositoryName());
        assertEquals("2.0", result.getProtocolVersion());
        assertEquals("admin@test.com", result.getAdminEmail());
        assertEquals(
            earliestDate.toInstant().atZone(ZoneId.systemDefault()).toLocalDate().toString(),
            result.getEarliestDatestamp());
        assertEquals("persistent", result.getDeletedRecord());
        assertEquals("YYYY-MM-DD", result.getGranularity());
        assertEquals(3, result.getDescription().size());
        verify(mongoTemplate).findOne(any(Query.class), eq(ExportDocument.class));
    }

    @Test
    void shouldThrowLoadingExceptionWhenIdentifyingNonExistingHandler() {
        // When
        assertThrows(LoadingException.class,
            () -> outboundExportService.identifyHandler("invalidHandlerId"));

        // Then (LoadingException should be thrown)
    }

    @Test
    void testListMetadataFormatsForHandler() {
        // When
        var result = outboundExportService.listMetadataFormatsForHandler("handler");

        // Then
        assertNotNull(result);
        assertEquals(2, result.getMetadataFormat().size());
        assertEquals("oai_cerif_openaire",
            result.getMetadataFormat().getFirst().getMetadataPrefix());
        assertEquals("oai_dim", result.getMetadataFormat().get(1).getMetadataPrefix());
    }

    @Test
    void testListMetadataFormatsForHandlerThrowsException() {
        // When
        assertThrows(LoadingException.class,
            () -> outboundExportService.listMetadataFormatsForHandler("invalidHandlerId"));

        // Then (LoadingException should be thrown)
    }
}
