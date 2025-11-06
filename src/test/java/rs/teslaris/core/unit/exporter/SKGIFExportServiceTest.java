package rs.teslaris.core.unit.exporter;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.Collections;
import java.util.List;
import java.util.Objects;
import org.junit.jupiter.api.Test;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.query.Query;
import rs.teslaris.core.util.exceptionhandling.exception.NotFoundException;
import rs.teslaris.exporter.model.common.ExportDocument;
import rs.teslaris.exporter.model.common.ExportPerson;
import rs.teslaris.exporter.model.common.ExportPersonName;
import rs.teslaris.exporter.service.impl.SKGIFExportServiceImpl;
import rs.teslaris.exporter.util.skgif.SKGIFFilterCriteria;

@SpringBootTest
public class SKGIFExportServiceTest {

    @Mock
    private MongoTemplate mongoTemplate;

    @InjectMocks
    private SKGIFExportServiceImpl skgifExportService;


    @Test
    void shouldReturnSKGIFResponseWhenEntityExists() {
        // given
        var localIdentifier = 123;
        var exportPerson = new ExportPerson();
        exportPerson.setDatabaseId(localIdentifier);
        exportPerson.setName(new ExportPersonName("John", "", "Doe"));

        when(mongoTemplate.find(any(Query.class), eq(ExportPerson.class)))
            .thenReturn(List.of(exportPerson));

        // when
        var result = skgifExportService.getEntityById(ExportPerson.class, localIdentifier, false);

        // then
        assertNotNull(result);
        verify(mongoTemplate).find(any(Query.class), eq(ExportPerson.class));
    }

    @Test
    void shouldThrowNotFoundExceptionWhenEntityDoesNotExist() {
        // given
        var localIdentifier = 999;
        when(mongoTemplate.find(any(Query.class), eq(ExportPerson.class)))
            .thenReturn(Collections.emptyList());

        // when & then
        assertThrows(NotFoundException.class, () ->
            skgifExportService.getEntityById(ExportPerson.class, localIdentifier, false));
    }

    @Test
    void shouldThrowRuntimeExceptionWhenConversionFails() {
        // given
        var localIdentifier = 123;
        var exportPerson = new ExportPerson();
        exportPerson.setDatabaseId(localIdentifier);

        when(mongoTemplate.find(any(Query.class), eq(ExportPerson.class)))
            .thenReturn(List.of(exportPerson));

        // when & then
        assertThrows(RuntimeException.class, () ->
            skgifExportService.getEntityById(ExportPerson.class, localIdentifier, false));
    }

    @Test
    void shouldReturnSKGIFListResponseWithPagination() {
        // given
        var pageable = PageRequest.of(0, 10);
        var exportPersons = List.of(new ExportPerson(), new ExportPerson());
        var totalCount = 25L;

        when(mongoTemplate.count(any(Query.class), eq(ExportPerson.class)))
            .thenReturn(totalCount);
        when(mongoTemplate.find(any(Query.class), eq(ExportPerson.class)))
            .thenReturn(exportPersons);

        // when
        var result = skgifExportService.getEntitiesFiltered(
            ExportPerson.class, null, false, new SKGIFFilterCriteria(""), pageable);

        // then
        assertNotNull(result);
        assertNotNull(result.getMeta());
        assertEquals(totalCount, result.getMeta().getCount());
        assertEquals(0, result.getMeta().getPage());
        assertEquals(10, result.getMeta().getSize());
        assertNotNull(result.getResults());

        verify(mongoTemplate).count(any(Query.class), eq(ExportPerson.class));
        verify(mongoTemplate).find(any(Query.class), eq(ExportPerson.class));
    }

    @Test
    void shouldFilterNullConversions() {
        // given
        var pageable = PageRequest.of(0, 10);
        var exportPersons = List.of(new ExportPerson(), new ExportPerson());
        var totalCount = 25L;

        when(mongoTemplate.count(any(Query.class), eq(ExportPerson.class)))
            .thenReturn(totalCount);
        when(mongoTemplate.find(any(Query.class), eq(ExportPerson.class)))
            .thenReturn(exportPersons);

        // when
        var result = skgifExportService.getEntitiesFiltered(
            ExportPerson.class, null, false, new SKGIFFilterCriteria(""), pageable);

        // then
        assertNotNull(result);
        assertTrue(result.getResults().stream().allMatch(Objects::nonNull));
    }

    @Test
    void shouldHandleEmptyResults() {
        // given
        var pageable = PageRequest.of(0, 10);
        var totalCount = 0L;

        when(mongoTemplate.count(any(Query.class), eq(ExportPerson.class)))
            .thenReturn(totalCount);
        when(mongoTemplate.find(any(Query.class), eq(ExportPerson.class)))
            .thenReturn(Collections.emptyList());

        // when
        var result = skgifExportService.getEntitiesFiltered(
            ExportPerson.class, null, false, new SKGIFFilterCriteria(""), pageable);

        // then
        assertNotNull(result);
        assertEquals(0, result.getMeta().getCount());
        assertTrue(result.getResults().isEmpty());
    }

    @Test
    void shouldApplyDocumentTypeConstraintsForVenue() {
        // given
        var pageable = PageRequest.of(0, 10);
        var exportDocuments = List.of(new ExportDocument());
        var totalCount = 5L;

        when(mongoTemplate.count(any(Query.class), eq(ExportDocument.class)))
            .thenReturn(totalCount);
        when(mongoTemplate.find(any(Query.class), eq(ExportDocument.class)))
            .thenReturn(exportDocuments);

        // when
        var result = skgifExportService.getEntitiesFiltered(
            ExportDocument.class, null, true, new SKGIFFilterCriteria(""), pageable);

        // then
        assertNotNull(result);
        verify(mongoTemplate).count(any(Query.class), eq(ExportDocument.class));
        verify(mongoTemplate).find(any(Query.class), eq(ExportDocument.class));
    }

    @Test
    void shouldApplyDocumentTypeConstraintsForNonVenue() {
        // given
        var pageable = PageRequest.of(0, 10);
        var exportDocuments = List.of(new ExportDocument());
        var totalCount = 5L;

        when(mongoTemplate.count(any(Query.class), eq(ExportDocument.class)))
            .thenReturn(totalCount);
        when(mongoTemplate.find(any(Query.class), eq(ExportDocument.class)))
            .thenReturn(exportDocuments);

        // when
        var result = skgifExportService.getEntitiesFiltered(
            ExportDocument.class, null, false, new SKGIFFilterCriteria(""), pageable);

        // then
        assertNotNull(result);
        verify(mongoTemplate).count(any(Query.class), eq(ExportDocument.class));
        verify(mongoTemplate).find(any(Query.class), eq(ExportDocument.class));
    }
}
