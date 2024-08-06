package rs.teslaris.core.exporter.service.impl;

import java.util.List;
import java.util.Optional;
import lombok.RequiredArgsConstructor;
import org.jetbrains.annotations.NotNull;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.domain.Sort;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.query.Criteria;
import org.springframework.data.mongodb.core.query.Query;
import org.springframework.stereotype.Service;
import rs.teslaris.core.exporter.model.common.ExportDocument;
import rs.teslaris.core.exporter.service.interfaces.OutboundExportService;
import rs.teslaris.core.exporter.util.ExportDataFormat;
import rs.teslaris.core.exporter.util.ExportHandlersConfigurationLoader;
import rs.teslaris.core.importer.model.oaipmh.common.Description;
import rs.teslaris.core.importer.model.oaipmh.common.GetRecord;
import rs.teslaris.core.importer.model.oaipmh.common.Identify;
import rs.teslaris.core.importer.model.oaipmh.common.ListMetadataFormats;
import rs.teslaris.core.importer.model.oaipmh.common.ListRecords;
import rs.teslaris.core.importer.model.oaipmh.common.ListSets;
import rs.teslaris.core.importer.model.oaipmh.common.MetadataFormat;
import rs.teslaris.core.importer.model.oaipmh.common.OAIIdentifier;
import rs.teslaris.core.importer.model.oaipmh.common.ServiceDescription;
import rs.teslaris.core.importer.model.oaipmh.common.Set;
import rs.teslaris.core.importer.model.oaipmh.common.Toolkit;
import rs.teslaris.core.util.exceptionhandling.exception.LoadingException;

@Service
@RequiredArgsConstructor
public class OutboundExportServiceImpl implements OutboundExportService {

    private final MongoTemplate mongoTemplate;

    @Value("${export.base.url}")
    private String baseUrl;

    @Value("${export.repo.name}")
    private String repositoryName;

    @Value("${export.admin.email}")
    private String adminEmail;

    @Value("${client.address}")
    private String frontendURL;

    @Override
    public ListRecords listRequestedRecords(String handler, String metadataPrefix,
                                            String from, String until, String set) {
        return null;
    }

    @Override
    public GetRecord listRequestedRecord(String handler, String metadataPrefix,
                                         String identifier) {
        return null;
    }

    @Override
    public Identify identifyHandler(String handler) {
        var handlerConfiguration =
            ExportHandlersConfigurationLoader.getHandlerByIdentifier(handler);
        if (handlerConfiguration.isEmpty()) {
            throw new LoadingException("No handler with identifier " + handler);
        }

        var identify = new Identify();
        identify.setBaseURL(baseUrl + "/" + handler);
        identify.setRepositoryName(repositoryName);
        identify.setProtocolVersion("2.0");
        identify.setAdminEmail("mailto:" + adminEmail);

        var earliestDocument = findEarliestDocument(
            Integer.parseInt(handlerConfiguration.get().internalInstitutionId()));
        earliestDocument.ifPresent(
            exportDocument -> identify.setEarliestDatestamp(exportDocument.getDocumentDate()));

        identify.setDeletedRecord("persistent");
        identify.setGranularity("YYYY-MM-DD");
        identify.setCompression(List.of("gzip", "deflate"));

        var service = getServiceDescription(identify, handlerConfiguration.get());

        var oaiIdentifier = new OAIIdentifier();
        oaiIdentifier.setScheme("oai");
        oaiIdentifier.setRepositoryIdentifier(repositoryName);
        oaiIdentifier.setDelimiter(":");
        oaiIdentifier.setSampleIdentifier("oai:CRIS.UNS:Publications/(BISIS)1000");

        var toolkit = new Toolkit(); // TODO: Check this data
        toolkit.setTitle("Sci2Zero Alliance Custom implementation");
        toolkit.setAuthor(
            new Toolkit.Author("Sci2Zero team", "mailto:chenejac@uns.ac.rs", "Sci2Zero"));
        toolkit.setVersion("1.0.0");

        var serviceDescription = new Description();
        serviceDescription.setService(service);
        var identifierDescription = new Description();
        identifierDescription.setOaiIdentifier(oaiIdentifier);
        var toolkitDescription = new Description();
        toolkitDescription.setToolkit(toolkit);

        identify.getDescriptions()
            .addAll(List.of(serviceDescription, identifierDescription, toolkitDescription));

        return identify;
    }

    public Optional<ExportDocument> findEarliestDocument(int internalInstitutionId) {
        Query query = new Query();
        query.addCriteria(Criteria.where("relatedInstitutionIds").in(internalInstitutionId));
        query.with(Sort.by(Sort.Direction.ASC, "documentDate"));
        query.limit(1);

        ExportDocument earliestDocument = mongoTemplate.findOne(query, ExportDocument.class);
        return Optional.ofNullable(earliestDocument);
    }

    @NotNull
    private ServiceDescription getServiceDescription(Identify identify,
                                                     ExportHandlersConfigurationLoader.Handler handler) {

        var service = new ServiceDescription();
        service.setCompatibility(
            "https://www.openaire.eu/cerif-profile/vocab/OpenAIRE_Service_Compatibility#1.1");
        service.setAcronym(repositoryName);
        service.setName(handler.handlerName());
        service.setDescription(handler.handlerDescription());
        service.setWebsiteURL(frontendURL);
        service.setOaiPMHBaseURL(identify.getBaseURL());
        return service;
    }

    @Override
    public ListSets listSetsForHandler(String handler) {
        var handlerConfiguration =
            ExportHandlersConfigurationLoader.getHandlerByIdentifier(handler);
        if (handlerConfiguration.isEmpty()) {
            throw new LoadingException("No handler with identifier " + handler);
        }

        var listSets = new ListSets();
        handlerConfiguration.get().sets().forEach(set -> {
            var newSet = new Set();
            newSet.setSetName(set.setName());
            newSet.setSetSpec(set.setSpec());
            listSets.getSet().add(newSet);
        });

        return listSets;
    }

    @Override
    public ListMetadataFormats listMetadataFormatsForHandler(String handler) {
        var handlerConfiguration =
            ExportHandlersConfigurationLoader.getHandlerByIdentifier(handler);
        if (handlerConfiguration.isEmpty()) {
            throw new LoadingException("No handler with identifier " + handler);
        }

        var listMetadataFormats = new ListMetadataFormats();
        handlerConfiguration.get().metadataFormats().forEach(format -> {
            var newMetadataFormat = new MetadataFormat();
            var exportFormatEnum = ExportDataFormat.fromStringValue(format);
            newMetadataFormat.setMetadataPrefix(format);
            newMetadataFormat.setSchema(exportFormatEnum.getSchema());
            newMetadataFormat.setMetadataNamespace(exportFormatEnum.getNamespace());
            listMetadataFormats.getMetadataFormats().add(newMetadataFormat);
        });

        return listMetadataFormats;
    }

    private void setCommonMetadataFormatFields(String format) {

    }
}
