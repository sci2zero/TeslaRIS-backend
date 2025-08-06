package rs.teslaris.importer.service.interfaces;

import org.springframework.stereotype.Service;
import rs.teslaris.importer.utility.DataSet;
import rs.teslaris.importer.utility.oaipmh.OAIPMHMigrationSource;

@Service
public interface OAIPMHMigrator {

    void harvest(DataSet requestDataSet, OAIPMHMigrationSource source, Integer userId);
}
