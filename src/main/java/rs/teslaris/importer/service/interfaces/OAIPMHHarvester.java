package rs.teslaris.importer.service.interfaces;

import org.springframework.stereotype.Service;
import rs.teslaris.importer.utility.DataSet;
import rs.teslaris.importer.utility.oaipmh.OAIPMHSource;

@Service
public interface OAIPMHHarvester {

    void harvest(DataSet requestDataSet, OAIPMHSource source, Integer userId);
}
