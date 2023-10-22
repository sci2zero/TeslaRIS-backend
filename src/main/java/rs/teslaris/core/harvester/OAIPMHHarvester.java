package rs.teslaris.core.harvester;

import java.io.StringReader;
import java.security.KeyManagementException;
import java.security.KeyStoreException;
import java.security.NoSuchAlgorithmException;
import javax.net.ssl.SSLContext;
import javax.xml.bind.JAXBContext;
import javax.xml.bind.JAXBException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.http.conn.ssl.NoopHostnameVerifier;
import org.apache.http.conn.ssl.SSLConnectionSocketFactory;
import org.apache.http.impl.client.HttpClients;
import org.apache.http.ssl.TrustStrategy;
import org.springframework.http.ResponseEntity;
import org.springframework.http.client.HttpComponentsClientHttpRequestFactory;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestTemplate;
import rs.teslaris.core.harvester.common.OAIPMHDataSet;
import rs.teslaris.core.harvester.common.OAIPMHResponse;
import rs.teslaris.core.util.exceptionhandling.exception.CantConstructRestTemplateException;

@Component
@RequiredArgsConstructor
@Slf4j
public class OAIPMHHarvester {

    private final String BASE_URL = "https://cris.uns.ac.rs/OAIHandlerOpenAIRECRIS";


    public void harvest(OAIPMHDataSet requestDataSet) {
        String endpoint = constructOAIPMHEndpoint(requestDataSet.getStringValue());
        var restTemplate = constructRestTemplate();

        while (true) {
            ResponseEntity<String> responseEntity =
                restTemplate.getForEntity(endpoint, String.class);
            if (responseEntity.getStatusCodeValue() == 200) {
                String responseBody = responseEntity.getBody();
                var oaiPmhResponse = parseResponse(responseBody);

                System.out.println(responseBody);
                System.out.println(oaiPmhResponse);

                // TODO: Save in temporary DB or DB table
                switch (requestDataSet) {
                    case EVENTS:
                        break;
                    case PATENTS:
                        break;
                    case PERSONS:
                        break;
                    case PRODUCTS:
                        break;
                    case PUBLICATIONS:
                        break;
                    case ORGANISATION_UNITS:
                        break;
                }
                break;
//                var resumptionToken = oaiPmhResponse.getListRecords().getResumptionToken();
//                if (resumptionToken == null) {
//                    break;
//                }
//
//                endpoint =
//                    BASE_URL + "?verb=ListRecords&resumptionToken=" + resumptionToken.getValue();
            } else {
                log.error("OAI-PMH request failed with response code: " +
                    responseEntity.getStatusCodeValue());
                break;
            }
        }
    }

    public OAIPMHResponse parseResponse(String xml) {
        try {
            var jaxbContext = JAXBContext.newInstance(OAIPMHResponse.class);
            var jaxbUnmarshaller = jaxbContext.createUnmarshaller();

            return (OAIPMHResponse) jaxbUnmarshaller.unmarshal(new StringReader(xml));
        } catch (JAXBException e) {
            log.error("Parsing OAI-PMH response failed. Reason: " + e.getMessage());
            return null;
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
