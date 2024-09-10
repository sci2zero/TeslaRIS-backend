package rs.teslaris.core.exporter.service.interfaces;

import org.springframework.stereotype.Service;
import rs.teslaris.core.importer.model.oaipmh.common.GetRecord;
import rs.teslaris.core.importer.model.oaipmh.common.Identify;
import rs.teslaris.core.importer.model.oaipmh.common.ListMetadataFormats;
import rs.teslaris.core.importer.model.oaipmh.common.ListRecords;
import rs.teslaris.core.importer.model.oaipmh.common.ListSets;
import rs.teslaris.core.importer.model.oaipmh.common.OAIPMHResponse;

@Service
public interface OutboundExportService {

    ListRecords listRequestedRecords(String handler, String metadataPrefix, String from,
                                     String until, String set, OAIPMHResponse response, int page,
                                     boolean identifiersOnly);

    GetRecord listRequestedRecord(String handler, String metadataPrefix,
                                  String identifier, OAIPMHResponse response);

    Identify identifyHandler(String handler);

    ListSets listSetsForHandler(String handler);

    ListMetadataFormats listMetadataFormatsForHandler(String handler);
}
