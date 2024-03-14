package rs.teslaris.core.importer.service.interfaces;

import org.springframework.stereotype.Service;
import rs.teslaris.core.importer.dto.RemainingRecordsCountResponseDTO;
import rs.teslaris.core.importer.utility.OAIPMHDataSet;

@Service
public interface OAIPMHLoader {

    void loadRecordsAuto(OAIPMHDataSet requestDataSet, boolean performIndex, Integer userId);

    <R> R loadRecordsWizard(OAIPMHDataSet requestDataSet, Integer userId);

    RemainingRecordsCountResponseDTO countRemainingDocumentsForLoading(Integer userId);
}
