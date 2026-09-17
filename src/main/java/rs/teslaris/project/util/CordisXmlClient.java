package rs.teslaris.project.util;

import jakarta.annotation.Nullable;
import java.io.StringReader;
import java.util.Objects;
import java.util.regex.Pattern;
import javax.xml.XMLConstants;
import javax.xml.parsers.DocumentBuilderFactory;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.web.client.HttpClientErrorException;
import org.w3c.dom.Document;
import org.xml.sax.InputSource;
import rs.teslaris.core.util.session.RestTemplateProvider;

@Component
@RequiredArgsConstructor
@Slf4j
public class CordisXmlClient {

    private static final String CORDIS_PROJECT_URL =
            "https://cordis.europa.eu/project/id/%s?format=xml";

    private static final Pattern MODEL_VERSION_PATTERN =
            Pattern.compile("Model Version:(\\d+)");

    private static final String EXPECTED_MODEL_VERSION = "132";

    private final RestTemplateProvider restTemplateProvider;

    @Nullable
    public Document fetchDocument(String cordisProjectId) {
        var xml = fetchRawXml(cordisProjectId);
        if (Objects.isNull(xml)) {
            return null;
        }

        checkModelVersion(xml, cordisProjectId);

        try {
            return parseXmlSecurely(xml);
        } catch (Exception e) {
            log.error("Failed to parse CORDIS XML for project {}: {}", cordisProjectId,
                    e.getMessage());
            return null;
        }
    }

    @Nullable
    private String fetchRawXml(String cordisProjectId) {
        var url = String.format(CORDIS_PROJECT_URL, cordisProjectId);
        var restTemplate = restTemplateProvider.provideRestTemplate();

        try {
            return restTemplate.getForObject(url, String.class);
        } catch (HttpClientErrorException e) {
            log.warn("Unable to fetch CORDIS data for project {}. Response code: {}",
                    cordisProjectId, e.getStatusCode().value());
            return null;
        }
    }

    private void checkModelVersion(String xml, String cordisProjectId) {
        var modelVersion = extractModelVersion(xml);
        if (Objects.nonNull(modelVersion) && !EXPECTED_MODEL_VERSION.equals(modelVersion)) {
            log.warn("CORDIS Model Version changed from expected {} to {} for project {}. " +
                            "XML structure may have changed - parser might need review.",
                    EXPECTED_MODEL_VERSION, modelVersion, cordisProjectId);
        } else if (Objects.isNull(modelVersion)) {
            log.warn("Could not find Model Version comment in CORDIS XML for project {}. " +
                    "Response format may have changed.", cordisProjectId);
        }
    }

    @Nullable
    private String extractModelVersion(String xml) {
        var matcher = MODEL_VERSION_PATTERN.matcher(xml);
        if (matcher.find()) {
            return matcher.group(1);
        }
        return null;
    }

    private Document parseXmlSecurely(String xml) throws Exception {
        var factory = DocumentBuilderFactory.newInstance();

        factory.setFeature("http://apache.org/xml/features/disallow-doctype-decl", true);
        factory.setFeature("http://xml.org/sax/features/external-general-entities", false);
        factory.setFeature("http://xml.org/sax/features/external-parameter-entities", false);
        factory.setXIncludeAware(false);
        factory.setExpandEntityReferences(false);
        factory.setNamespaceAware(true);
        factory.setFeature(XMLConstants.FEATURE_SECURE_PROCESSING, true);

        var builder = factory.newDocumentBuilder();
        return builder.parse(new InputSource(new StringReader(xml)));
    }
}
