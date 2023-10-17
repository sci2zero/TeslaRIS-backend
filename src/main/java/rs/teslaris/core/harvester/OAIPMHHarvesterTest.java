package rs.teslaris.core.harvester;

import java.io.BufferedReader;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.StringReader;
import java.security.KeyManagementException;
import java.security.KeyStoreException;
import java.security.NoSuchAlgorithmException;
import javax.net.ssl.SSLContext;
import javax.xml.bind.JAXBContext;
import javax.xml.bind.JAXBException;
import lombok.RequiredArgsConstructor;
import org.apache.http.conn.ssl.NoopHostnameVerifier;
import org.apache.http.conn.ssl.SSLConnectionSocketFactory;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClients;
import org.apache.http.ssl.TrustStrategy;
import org.springframework.http.client.HttpComponentsClientHttpRequestFactory;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestTemplate;

@Component
@RequiredArgsConstructor
public class OAIPMHHarvesterTest {

    public void harvest() {
//        String endpoint =
//            "https://cris.uns.ac.rs/OAIHandlerOpenAIRECRIS?verb=ListRecords&set=openaire_cris_orgunits&metadataPrefix=oai_cerif_openaire";
//
//        var restTemplate = getRestTemplate();
//
//        while (true) {
//            ResponseEntity<String> responseEntity =
//                restTemplate.getForEntity(endpoint, String.class);
//
//            if (responseEntity.getStatusCodeValue() == 200) {
//                String responseBody = responseEntity.getBody();
//
//                System.out.println(responseBody);
//
//                var oaiPmhResponse = parseResponse(responseBody);
//                System.out.println(oaiPmhResponse.getResponseDate());
//
//                var resumptionToken = oaiPmhResponse.getListRecords().getResumptionToken();
//                if (resumptionToken == null) {
//                    break;
//                }
//                endpoint =
//                    "https://cris.uns.ac.rs/OAIHandlerOpenAIRECRIS?verb=ListRecords&resumptionToken=" +
//                        resumptionToken.getValue();
//            } else {
//                System.err.println("HTTP request failed with response code: " +
//                    responseEntity.getStatusCodeValue());
//                break;
//            }
//        }

        try {
            BufferedReader reader = new BufferedReader(new InputStreamReader(new FileInputStream(
                "/home/ivanmrsulja/Desktop/aaa.xml")));
            StringBuilder sb = new StringBuilder();
            reader.lines().forEach(sb::append);
            System.out.println(sb);
            var parsed = parseResponse(sb.toString());
            System.out.println(parsed.getListRecords().getResumptionToken().getValue());
        } catch (IOException e) {
            throw new RuntimeException(e);
        }

    }

    public OAIPMHResponse parseResponse(String xml) {
        try {
            var jaxbContext = JAXBContext.newInstance(OAIPMHResponse.class);
            var jaxbUnmarshaller = jaxbContext.createUnmarshaller();

            return (OAIPMHResponse) jaxbUnmarshaller.unmarshal(new StringReader(xml));
        } catch (JAXBException e) {
            e.printStackTrace();
            return null;
        }
    }

    private RestTemplate getRestTemplate() {
        TrustStrategy acceptingTrustStrategy = (x509Certificates, s) -> true;
        SSLContext sslContext;
        try {
            sslContext = org.apache.http.ssl.SSLContexts.custom()
                .loadTrustMaterial(null, acceptingTrustStrategy)
                .build();
        } catch (NoSuchAlgorithmException | KeyManagementException | KeyStoreException e) {
            throw new RuntimeException(e);
        }
        SSLConnectionSocketFactory csf =
            new SSLConnectionSocketFactory(sslContext, new NoopHostnameVerifier());
        CloseableHttpClient httpClient = HttpClients.custom().setSSLSocketFactory(csf).build();
        HttpComponentsClientHttpRequestFactory requestFactory =
            new HttpComponentsClientHttpRequestFactory();
        requestFactory.setHttpClient(httpClient);
        return new RestTemplate(requestFactory);
    }
}
