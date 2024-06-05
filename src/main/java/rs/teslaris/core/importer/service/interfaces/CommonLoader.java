package rs.teslaris.core.importer.service.interfaces;

import org.springframework.stereotype.Service;

@Service
public interface CommonLoader {

    <R> R loadRecordsWizard(Integer userId);

    <R> R loadSkippedRecordsWizard(Integer userId);

    void skipRecord(Integer userId);

    void markRecordAsLoaded(Integer userId);

    Integer countRemainingDocumentsForLoading(Integer userId);
}
