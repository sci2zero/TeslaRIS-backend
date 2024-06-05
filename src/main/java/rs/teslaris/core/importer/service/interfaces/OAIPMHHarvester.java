package rs.teslaris.core.importer.service.interfaces;

import org.springframework.stereotype.Service;
import rs.teslaris.core.importer.utility.DataSet;
import rs.teslaris.core.importer.utility.OAIPMHSource;

@Service
public interface OAIPMHHarvester {

    void harvest(DataSet requestDataSet, OAIPMHSource source, Integer userId);
}
