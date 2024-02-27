package rs.teslaris.core.importer.service.interfaces;

import org.springframework.stereotype.Service;
import rs.teslaris.core.importer.utility.OAIPMHDataSet;

@Service
public interface OAIPMHLoader {

    void loadRecordsAuto(OAIPMHDataSet requestDataSet, boolean performIndex);

    <R> R loadRecordsWizard(OAIPMHDataSet requestDataSet);
}
