package rs.teslaris.core.importer.service.interfaces;

import org.springframework.stereotype.Service;
import rs.teslaris.core.importer.dto.RemainingRecordsCountResponseDTO;
import rs.teslaris.core.importer.utility.DataSet;

@Service
public interface OAIPMHLoader {

    void loadRecordsAuto(DataSet requestDataSet, boolean performIndex, Integer userId);

    <R> R loadRecordsWizard(DataSet requestDataSet, Integer userId);

    <R> R loadSkippedRecordsWizard(DataSet requestDataSet, Integer userId);

    void skipRecord(DataSet requestDataSet, Integer userId);

    void markRecordAsLoaded(DataSet requestDataSet, Integer userId);

    RemainingRecordsCountResponseDTO countRemainingDocumentsForLoading(Integer userId);
}
