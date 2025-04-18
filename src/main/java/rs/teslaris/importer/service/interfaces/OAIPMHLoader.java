package rs.teslaris.importer.service.interfaces;

import org.springframework.stereotype.Service;
import rs.teslaris.importer.dto.RemainingRecordsCountResponseDTO;
import rs.teslaris.importer.utility.DataSet;

@Service
public interface OAIPMHLoader {

    void loadRecordsAuto(DataSet requestDataSet, boolean performIndex, Integer userId);

    <R> R loadRecordsWizard(DataSet requestDataSet, Integer userId);

    <R> R loadSkippedRecordsWizard(DataSet requestDataSet, Integer userId);

    void skipRecord(DataSet requestDataSet, Integer userId);

    void markRecordAsLoaded(DataSet requestDataSet, Integer userId);

    RemainingRecordsCountResponseDTO countRemainingDocumentsForLoading(Integer userId);
}
