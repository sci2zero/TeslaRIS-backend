package rs.teslaris.core.harvester;

import java.io.StringReader;
import java.security.KeyManagementException;
import java.security.KeyStoreException;
import java.security.NoSuchAlgorithmException;
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
import org.apache.http.conn.ssl.NoopHostnameVerifier;
import org.apache.http.conn.ssl.SSLConnectionSocketFactory;
import org.apache.http.impl.client.HttpClients;
import org.apache.http.ssl.TrustStrategy;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.http.ResponseEntity;
import org.springframework.http.client.HttpComponentsClientHttpRequestFactory;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestTemplate;
import org.xml.sax.InputSource;
import org.xml.sax.SAXException;
import rs.teslaris.core.harvester.common.OAIPMHDataSet;
import rs.teslaris.core.harvester.common.OAIPMHResponse;
import rs.teslaris.core.harvester.common.ResumptionToken;
import rs.teslaris.core.util.exceptionhandling.exception.CantConstructRestTemplateException;

@Component
@RequiredArgsConstructor
@Slf4j
public class OAIPMHHarvester {

    private final String BASE_URL = "https://cris.uns.ac.rs/OAIHandlerOpenAIRECRIS";

    private final MongoTemplate mongoTemplate;

    public void harvest(OAIPMHDataSet requestDataSet) {
        String endpoint = constructOAIPMHEndpoint(requestDataSet.getStringValue());
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

                endpoint =
                    BASE_URL + "?verb=ListRecords&resumptionToken=" +
                        optionalResumptionToken.get().getValue();
            } else {
                log.error("OAI-PMH request failed with response code: " +
                    responseEntity.getStatusCodeValue());
            }
        }
    }

    private Optional<ResumptionToken> handleOAIPMHResponse(OAIPMHDataSet requestDataSet,
                                                           OAIPMHResponse oaiPmhResponse) {
        var records = oaiPmhResponse.getListRecords().getRecords();
        records.forEach(
            record -> {
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
        if (resumptionToken == null) {
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
        try {
            sslContext = org.apache.http.ssl.SSLContexts.custom()
                .loadTrustMaterial(null, acceptingTrustStrategy)
                .build();
        } catch (NoSuchAlgorithmException | KeyManagementException | KeyStoreException e) {
            log.error("Rest template construction failed.");
            throw new CantConstructRestTemplateException(e.getMessage());
        }

        var connectionSocketFactory =
            new SSLConnectionSocketFactory(sslContext, new NoopHostnameVerifier());
        var httpClient = HttpClients.custom().setSSLSocketFactory(connectionSocketFactory).build();
        var requestFactory = new HttpComponentsClientHttpRequestFactory();
        requestFactory.setHttpClient(httpClient);

        return new RestTemplate(requestFactory);
    }

    private String constructOAIPMHEndpoint(String set) {
        return BASE_URL + "?verb=ListRecords&set=" + set + "&metadataPrefix=oai_cerif_openaire";
    }
}
