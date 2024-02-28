package rs.teslaris.core.importer.service.impl;

import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.StringReader;
import java.security.KeyManagementException;
import java.security.KeyStore;
import java.security.KeyStoreException;
import java.security.NoSuchAlgorithmException;
import java.security.cert.CertificateException;
import java.util.Objects;
import java.util.Optional;
import javax.net.ssl.SSLContext;
import javax.xml.XMLConstants;
import javax.xml.bind.JAXBContext;
import javax.xml.bind.JAXBException;
import javax.xml.parsers.ParserConfigurationException;
import javax.xml.parsers.SAXParserFactory;
import javax.xml.transform.sax.SAXSource;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.http.HttpHost;
import org.apache.http.conn.ssl.NoopHostnameVerifier;
import org.apache.http.conn.ssl.SSLConnectionSocketFactory;
import org.apache.http.impl.client.HttpClients;
import org.apache.http.ssl.SSLContexts;
import org.apache.http.ssl.TrustStrategy;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.http.ResponseEntity;
import org.springframework.http.client.HttpComponentsClientHttpRequestFactory;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;
import org.xml.sax.InputSource;
import org.xml.sax.SAXException;
import rs.teslaris.core.importer.model.common.OAIPMHResponse;
import rs.teslaris.core.importer.model.common.ResumptionToken;
import rs.teslaris.core.importer.service.interfaces.OAIPMHHarvester;
import rs.teslaris.core.importer.utility.OAIPMHDataSet;
import rs.teslaris.core.importer.utility.OAIPMHSource;
import rs.teslaris.core.util.exceptionhandling.exception.CantConstructRestTemplateException;

@Service
@RequiredArgsConstructor
@Slf4j
public class OAIPMHHarvesterImpl implements OAIPMHHarvester {

    private final MongoTemplate mongoTemplate;

    @Value("${ssl.trust-store}")
    private String trustStorePath;

    @Value("${ssl.trust-store-password}")
    private String trustStorePassword;

    @Value("${proxy.host:}")
    private String proxyHost;

    @Value("${proxy.port:80}")
    private Integer proxyPort;


    public void harvest(OAIPMHDataSet requestDataSet, OAIPMHSource source) {
        String endpoint =
            constructOAIPMHEndpoint(requestDataSet.getStringValue(), source.getStringValue());
        var restTemplate = constructRestTemplate();

        while (true) {
            ResponseEntity<String> responseEntity =
                restTemplate.getForEntity(endpoint, String.class);
            if (responseEntity.getStatusCodeValue() == 200) {
                String responseBody = responseEntity.getBody();
                var optionalOaiPmhResponse = parseResponse(responseBody);
                if (optionalOaiPmhResponse.isEmpty()) {
                    break;
                }

                var optionalResumptionToken =
                    handleOAIPMHResponse(requestDataSet, optionalOaiPmhResponse.get());
                if (optionalResumptionToken.isEmpty()) {
                    break;
                }

                endpoint = source.getStringValue() + "?verb=ListRecords&resumptionToken=" +
                    optionalResumptionToken.get().getValue();
            } else {
                log.error("OAI-PMH request failed with response code: " +
                    responseEntity.getStatusCodeValue());
            }
        }
    }

    private Optional<ResumptionToken> handleOAIPMHResponse(OAIPMHDataSet requestDataSet,
                                                           OAIPMHResponse oaiPmhResponse) {
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
                        mongoTemplate.save(metadata.getEvent());
                        break;
                    case PATENTS:
                        mongoTemplate.save(metadata.getPatent());
                        break;
                    case PERSONS:
                        mongoTemplate.save(metadata.getPerson());
                        break;
                    case PRODUCTS:
                        mongoTemplate.save(metadata.getProduct());
                        break;
                    case PUBLICATIONS:
                        mongoTemplate.save(metadata.getPublication());
                        break;
                    case ORGANISATION_UNITS:
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

    private RestTemplate constructRestTemplate() {
        TrustStrategy acceptingTrustStrategy = (x509Certificates, s) -> true;

        SSLContext sslContext;
        try (InputStream truststoreInputStream = new FileInputStream(trustStorePath)) {
            var truststore = KeyStore.getInstance(KeyStore.getDefaultType());
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

        var connectionSocketFactory =
            new SSLConnectionSocketFactory(sslContext, new NoopHostnameVerifier());
        var httpClient = HttpClients.custom().setSSLSocketFactory(connectionSocketFactory);

        if (!proxyHost.isEmpty()) {
            httpClient.setProxy(new HttpHost(proxyHost, proxyPort));
        }

        var requestFactory = new HttpComponentsClientHttpRequestFactory();
        requestFactory.setHttpClient(httpClient.build());

        return new RestTemplate(requestFactory);
    }

    private String constructOAIPMHEndpoint(String set, String base) {
        return base + "?verb=ListRecords&set=" + set + "&metadataPrefix=oai_cerif_openaire";
    }
}
