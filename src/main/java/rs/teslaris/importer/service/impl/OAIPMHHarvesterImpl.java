package rs.teslaris.importer.service.impl;

import jakarta.annotation.Nullable;
import jakarta.xml.bind.JAXBContext;
import jakarta.xml.bind.JAXBException;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.StringReader;
import java.security.KeyManagementException;
import java.security.KeyStore;
import java.security.KeyStoreException;
import java.security.NoSuchAlgorithmException;
import java.security.cert.CertificateException;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import javax.net.ssl.SSLContext;
import javax.xml.XMLConstants;
import javax.xml.parsers.ParserConfigurationException;
import javax.xml.parsers.SAXParserFactory;
import javax.xml.transform.sax.SAXSource;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.hc.client5.http.impl.classic.HttpClients;
import org.apache.hc.client5.http.impl.io.PoolingHttpClientConnectionManagerBuilder;
import org.apache.hc.client5.http.ssl.NoopHostnameVerifier;
import org.apache.hc.client5.http.ssl.SSLConnectionSocketFactory;
import org.apache.hc.client5.http.ssl.TrustSelfSignedStrategy;
import org.apache.hc.core5.http.HttpHost;
import org.apache.hc.core5.ssl.SSLContexts;
import org.apache.hc.core5.ssl.TrustStrategy;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.query.Criteria;
import org.springframework.data.mongodb.core.query.Query;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.http.client.HttpComponentsClientHttpRequestFactory;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;
import org.xml.sax.InputSource;
import org.xml.sax.SAXException;
import rs.teslaris.core.model.oaipmh.common.OAIPMHResponse;
import rs.teslaris.core.model.oaipmh.common.ResumptionToken;
import rs.teslaris.core.model.oaipmh.event.Event;
import rs.teslaris.core.model.oaipmh.organisationunit.OrgUnit;
import rs.teslaris.core.model.oaipmh.patent.Patent;
import rs.teslaris.core.model.oaipmh.person.Person;
import rs.teslaris.core.model.oaipmh.product.Product;
import rs.teslaris.core.model.oaipmh.publication.Publication;
import rs.teslaris.core.util.exceptionhandling.exception.CantConstructRestTemplateException;
import rs.teslaris.importer.service.interfaces.OAIPMHHarvester;
import rs.teslaris.importer.utility.DataSet;
import rs.teslaris.importer.utility.HarvestProgressReport;
import rs.teslaris.importer.utility.OAIPMHSource;
import rs.teslaris.importer.utility.ProgressReportUtility;

@Service
@RequiredArgsConstructor
@Slf4j
public class OAIPMHHarvesterImpl implements OAIPMHHarvester {

    private static final int MAX_RESTART_NUMBER = 10;

    private final MongoTemplate mongoTemplate;
    @Value("${ssl.trust-store}")
    private String trustStorePath;

    @Value("${ssl.trust-store-password}")
    private String trustStorePassword;

    @Value("${proxy.enabled}")
    private Boolean proxyEnabled;

    @Value("${proxy.host:}")
    private String proxyHost;

    @Value("${proxy.port:80}")
    private Integer proxyPort;


    @Override
    public void harvest(DataSet requestDataSet, OAIPMHSource source, Integer userId) {
        String endpoint =
            constructOAIPMHEndpoint(requestDataSet.getStringValue(), source.getStringValue());
        var restTemplate = constructRestTemplate();

        // Reset Loader progress
        ProgressReportUtility.resetProgressReport(requestDataSet, userId, mongoTemplate);

        var harvestProgressReport = getProgressReport(requestDataSet, userId);

        if (Objects.nonNull(harvestProgressReport)) {
            endpoint = source.getStringValue() + "?verb=ListRecords&resumptionToken=" +
                harvestProgressReport.getResumptionToken();
        }

        int restartCount = 0;
        while (true) {
            try {
                ResponseEntity<String> responseEntity =
                    restTemplate.getForEntity(endpoint, String.class);
                if (responseEntity.getStatusCode() == HttpStatus.OK) {
                    String responseBody = responseEntity.getBody();
                    var optionalOaiPmhResponse = parseResponse(responseBody);
                    if (optionalOaiPmhResponse.isEmpty()) {
                        break;
                    }

                    var optionalResumptionToken =
                        handleOAIPMHResponse(requestDataSet, optionalOaiPmhResponse.get(), userId);
                    if (optionalResumptionToken.isEmpty() ||
                        optionalResumptionToken.get().getValue().isBlank()) {
                        break;
                    }

                    endpoint = source.getStringValue() + "?verb=ListRecords&resumptionToken=" +
                        optionalResumptionToken.get().getValue();

                    updateProgressReport(requestDataSet, optionalResumptionToken.get().getValue(),
                        userId);
                } else {
                    log.error("OAI-PMH request failed with response code: " +
                        responseEntity.getStatusCode());
                }
            } catch (Exception e) {
                if (restartCount == MAX_RESTART_NUMBER) {
                    log.error(
                        "Harvest did not complete because host (" + source.getStringValue() +
                            ") keeps crashing. Manual restart required.");
                    return;
                }

                restartCount += 1;

                log.warn(
                    "No route to host for endpoint: " + endpoint + " - Restarting " +
                        restartCount + " of " + MAX_RESTART_NUMBER);
            }
        }

        // Delete progress report after completing the harvest
        deleteProgressReport(requestDataSet, userId);
    }

    private Optional<ResumptionToken> handleOAIPMHResponse(DataSet requestDataSet,
                                                           OAIPMHResponse oaiPmhResponse,
                                                           Integer userId) {
        if (oaiPmhResponse.getListRecords() == null) {
            return Optional.empty();
        }
        var records = oaiPmhResponse.getListRecords().getRecords();
        records.forEach(
            record -> {
                if (Objects.nonNull(record.getHeader().getStatus()) &&
                    record.getHeader().getStatus().equalsIgnoreCase("deleted")) {
                    // TODO: should deleted records be removed from our db?
                    return;
                }
                var metadata = record.getMetadata();
                switch (requestDataSet) {
                    case EVENTS:
                        ((Event) metadata.getEvent()).setImportUserId(List.of(userId));
                        ((Event) metadata.getEvent()).setLoaded(false);
                        mongoTemplate.save(metadata.getEvent());
                        break;
                    case PATENTS:
                        ((Patent) metadata.getPatent()).setImportUserId(List.of(userId));
                        ((Patent) metadata.getPatent()).setLoaded(false);
                        mongoTemplate.save(metadata.getPatent());
                        break;
                    case PERSONS:
                        ((Person) metadata.getPerson()).setImportUserId(List.of(userId));
                        ((Person) metadata.getPerson()).setLoaded(false);
                        mongoTemplate.save(metadata.getPerson());
                        break;
                    case PRODUCTS:
                        ((Product) metadata.getProduct()).setImportUserId(List.of(userId));
                        ((Product) metadata.getProduct()).setLoaded(false);
                        mongoTemplate.save(metadata.getProduct());
                        break;
                    case PUBLICATIONS:
                        ((Publication) metadata.getPublication()).setImportUserId(List.of(userId));
                        ((Publication) metadata.getPublication()).setLoaded(false);
                        mongoTemplate.save(metadata.getPublication());
                        break;
                    case ORGANISATION_UNITS:
                        ((OrgUnit) metadata.getOrgUnit()).setImportUserId(List.of(userId));
                        ((OrgUnit) metadata.getOrgUnit()).setLoaded(false);
                        mongoTemplate.save(metadata.getOrgUnit());
                        break;
                }
            });
        var resumptionToken = oaiPmhResponse.getListRecords().getResumptionToken();
        if (resumptionToken.getValue() == null) {
            return Optional.empty();
        }

        return Optional.of(resumptionToken);
    }

    public Optional<OAIPMHResponse> parseResponse(String xml) {
        try {
            var jaxbContext = JAXBContext.newInstance(OAIPMHResponse.class);

            var saxParserFactory = SAXParserFactory.newInstance();
            saxParserFactory.setFeature(XMLConstants.FEATURE_SECURE_PROCESSING, Boolean.TRUE);
            saxParserFactory.setFeature(
                "http://apache.org/xml/features/nonvalidating/load-external-dtd",
                Boolean.FALSE);
            saxParserFactory.setFeature("http://apache.org/xml/features/disallow-doctype-decl",
                Boolean.TRUE);
            saxParserFactory.setValidating(Boolean.FALSE);

            var xmlReader = saxParserFactory.newSAXParser().getXMLReader();
            var inputSource = new InputSource(new StringReader(xml));
            var source = new SAXSource(xmlReader, inputSource);
            var jaxbUnmarshaller = jaxbContext.createUnmarshaller();

            return Optional.of(
                (OAIPMHResponse) jaxbUnmarshaller.unmarshal(source.getInputSource()));
        } catch (JAXBException | ParserConfigurationException | SAXException e) {
            log.error("Parsing OAI-PMH response failed. Reason: " + e.getMessage());
            return Optional.empty();
        }
    }

    public RestTemplate constructRestTemplate() {
        TrustStrategy acceptingTrustStrategy = new TrustSelfSignedStrategy();

        SSLContext sslContext;
        try (InputStream truststoreInputStream = new FileInputStream(trustStorePath)) {
            KeyStore truststore = KeyStore.getInstance(KeyStore.getDefaultType());
            truststore.load(truststoreInputStream, trustStorePassword.toCharArray());

            sslContext = SSLContexts.custom()
                .loadTrustMaterial(truststore, acceptingTrustStrategy)
                .build();
        } catch (NoSuchAlgorithmException | KeyManagementException | KeyStoreException |
                 CertificateException | IOException e) {
            log.error("Rest template construction failed. Reason:\n" + e.getMessage());
            throw new CantConstructRestTemplateException(
                "Unable to establish secure connection to remote host.");
        }

        SSLConnectionSocketFactory connectionSocketFactory =
            new SSLConnectionSocketFactory(sslContext, NoopHostnameVerifier.INSTANCE);
        var connectionManager = PoolingHttpClientConnectionManagerBuilder.create()
            .setSSLSocketFactory(connectionSocketFactory).build();
        var httpClient = HttpClients.custom()
            .setConnectionManager(connectionManager)
            .build();

        if (proxyEnabled) {
            httpClient = HttpClients.custom()
                .setConnectionManager(connectionManager)
                .setProxy(new HttpHost(proxyHost, proxyPort))
                .build();
        }

        HttpComponentsClientHttpRequestFactory requestFactory =
            new HttpComponentsClientHttpRequestFactory(httpClient);

        return new RestTemplate(requestFactory);
    }

    private String constructOAIPMHEndpoint(String set, String base) {
        return base + "?verb=ListRecords&set=" + set + "&metadataPrefix=oai_cerif_openaire";
    }

    private void updateProgressReport(DataSet requestDataSet, String resumptionToken,
                                      Integer userId) {
        Query deleteQuery = new Query();
        deleteQuery.addCriteria(Criteria.where("dataset").is(requestDataSet))
            .addCriteria(Criteria.where("userId").is(userId));
        mongoTemplate.remove(deleteQuery, HarvestProgressReport.class);

        mongoTemplate.save(new HarvestProgressReport(resumptionToken, userId, requestDataSet));
    }

    @Nullable
    private HarvestProgressReport getProgressReport(DataSet requestDataSet, Integer userId) {
        Query query = new Query();
        query.addCriteria(Criteria.where("dataset").is(requestDataSet.name()))
            .addCriteria(Criteria.where("userId").is(userId));
        return mongoTemplate.findOne(query, HarvestProgressReport.class);
    }

    private void deleteProgressReport(DataSet requestDataSet, Integer userId) {
        Query deleteQuery = new Query();
        deleteQuery.addCriteria(Criteria.where("dataset").is(requestDataSet.name()))
            .addCriteria(Criteria.where("userId").is(userId));
        mongoTemplate.remove(deleteQuery, HarvestProgressReport.class);
    }

}
