package rs.teslaris.core.importer.service.interfaces;

import rs.teslaris.core.importer.utility.OAIPMHDataSet;
import rs.teslaris.core.importer.utility.OAIPMHSource;

public interface OAIPMHHarvester {

    void harvest(OAIPMHDataSet requestDataSet, OAIPMHSource source, Integer userId);
}
