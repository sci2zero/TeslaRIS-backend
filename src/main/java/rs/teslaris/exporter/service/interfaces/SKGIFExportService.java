package rs.teslaris.exporter.service.interfaces;

import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import rs.teslaris.exporter.model.common.BaseExportEntity;
import rs.teslaris.exporter.model.skgif.SKGIFListResponse;
import rs.teslaris.exporter.model.skgif.SKGIFSingleResponse;
import rs.teslaris.exporter.util.skgif.SKGIFFilterCriteria;

@Service
public interface SKGIFExportService {

    <T extends BaseExportEntity> SKGIFSingleResponse getEntityById(Class<T> entityClass,
                                                                   Integer localIdentifier,
                                                                   boolean isVenue);

    <T extends BaseExportEntity> SKGIFListResponse getEntitiesFiltered(Class<T> entityClass,
                                                                       String filter,
                                                                       boolean isVenue,
                                                                       SKGIFFilterCriteria criteria,
                                                                       Pageable pageable);
}
