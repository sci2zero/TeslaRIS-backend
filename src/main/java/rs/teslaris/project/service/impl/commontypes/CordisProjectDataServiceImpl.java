package rs.teslaris.project.service.impl.commontypes;

import jakarta.annotation.Nullable;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.web.client.HttpClientErrorException;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;
import org.xml.sax.InputSource;
import rs.teslaris.core.dto.commontypes.MonetaryAmountDTO;
import rs.teslaris.core.dto.commontypes.MultilingualContentDTO;
import rs.teslaris.core.model.commontypes.LanguageTag;
import rs.teslaris.core.service.interfaces.commontypes.CurrencyService;
import rs.teslaris.core.service.interfaces.commontypes.LanguageTagService;
import rs.teslaris.core.util.session.RestTemplateProvider;
import rs.teslaris.project.dto.project.PrepopulatedOrganisationDTO;
import rs.teslaris.project.dto.project.PrepopulatedEventDTO;
import rs.teslaris.project.dto.project.PrepopulatedProjectMetadataDTO;
import rs.teslaris.project.model.project.OrganisationUnitProjectContributionType;
import rs.teslaris.project.model.project.ProjectStatus;
import rs.teslaris.project.service.interfaces.commontypes.CordisProjectDataService;

import javax.xml.XMLConstants;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.xpath.XPath;
import javax.xml.xpath.XPathConstants;
import javax.xml.xpath.XPathFactory;
import java.io.StringReader;
import java.util.ArrayList;
import java.util.Objects;
import java.util.regex.Pattern;

@Service
@RequiredArgsConstructor
@Slf4j
public class CordisProjectDataServiceImpl implements CordisProjectDataService {

    private static final String CORDIS_PROJECT_URL =
            "https://cordis.europa.eu/project/id/%s?format=xml";

    private static final Pattern MODEL_VERSION_PATTERN =
            Pattern.compile("Model Version:(\\d+)");

    private static final String EXPECTED_MODEL_VERSION = "132";

    private static final String EU_FUNDER_DISPLAY_NAME = "European Commission";

    private static final String CURRENCY_CODE = "EUR";

    private final RestTemplateProvider restTemplateProvider;

    private final LanguageTagService languageTagService;

    private final CurrencyService currencyService;

    @Override
    public PrepopulatedProjectMetadataDTO fetchFullMetadata(String cordisProjectId, String doi) {
        var metadata = new PrepopulatedProjectMetadataDTO();
        metadata.setDoi(doi);

        var xml = fetchRawXml(cordisProjectId);
        if (Objects.isNull(xml)) {
            return metadata;
        }

        checkModelVersion(xml, cordisProjectId);

        try {
            var document = parseXmlSecurely(xml);
            populateFromDocument(metadata, document);
        } catch (Exception e) {
            log.error("Failed to parse CORDIS XML for project {}: {}", cordisProjectId,
                    e.getMessage());
        }

        return metadata;
    }

    @Nullable
    private String extractModelVersion(String xml) {
        var matcher = MODEL_VERSION_PATTERN.matcher(xml);
        if (matcher.find()) {
            return matcher.group(1);
        }
        return null;
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

    private void populateFromDocument(PrepopulatedProjectMetadataDTO metadata, Document document)
            throws Exception {
        var xpath = XPathFactory.newInstance().newXPath();
        var english = languageTagService.findLanguageTagByValue("EN");

        var title = evaluateText(xpath, document, "//*[local-name()='title']");
        if (Objects.nonNull(title)) {
            metadata.getName().add(new MultilingualContentDTO(
                    english.getId(), english.getLanguageTag(), title, 1));
        }

        var acronym = evaluateText(xpath, document, "//*[local-name()='acronym']");
        if (Objects.nonNull(acronym)) {
            metadata.getNameAbbreviation().add(new MultilingualContentDTO(
                    english.getId(), english.getLanguageTag(), acronym, 1));
        }

        var objective = evaluateText(xpath, document, "//*[local-name()='objective']");
        if (Objects.nonNull(objective)) {
            metadata.getDescription().add(new MultilingualContentDTO(
                    english.getId(), english.getLanguageTag(), objective, 1));
        }

        var keywords = evaluateText(xpath, document, "//*[local-name()='keywords']");
        if (Objects.nonNull(objective)) {
            metadata.getKeywords().add(new MultilingualContentDTO(
                    english.getId(), english.getLanguageTag(), keywords, 1));
        }

        var status = evaluateText(xpath, document, "//*[local-name()='status']");
        metadata.setStatus(mapCordisStatusToProjectStatus(status));

        metadata.setDateFrom(evaluateText(xpath, document, "//*[local-name()='startDate']"));
        metadata.setDateTo(evaluateText(xpath, document, "//*[local-name()='endDate']"));

        var totalCost = evaluateText(xpath, document, "//*[local-name()='totalCost']");
        if (Objects.nonNull(totalCost)) {
            var currency = currencyService.findCurrencyByCode(CURRENCY_CODE);
            try {
                metadata.setCosts(
                        new MonetaryAmountDTO(currency, Double.parseDouble(totalCost)));
            } catch (NumberFormatException e) {
                log.warn("Unable to parse totalCost value: {}", totalCost);
            }
        }

        // Could be helpful when searching for possible funders on frontend.
        metadata.getFunderName().add(new MultilingualContentDTO(
                english.getId(), english.getLanguageTag(), EU_FUNDER_DISPLAY_NAME, 1));

        var uriNodes = (NodeList) xpath.evaluate(
                "//*[local-name()='webLink'][@represents='project'][@type='relatedWebsite']/*[local-name()='physUrl']", document, XPathConstants.NODESET);
        metadata.setUris(new ArrayList<>());
        for (var i = 0; i < uriNodes.getLength(); i++) {
            var url = uriNodes.item(i).getTextContent();
            if (Objects.nonNull(url) && !url.isBlank()) {
                metadata.getUris().add(url.trim());
            }
        }

        var organizationNodes = (NodeList) xpath.evaluate(
                "//*[local-name()='organization']", document, XPathConstants.NODESET);

        for (var i = 0; i < organizationNodes.getLength(); i++) {
            var orgElement = (Element) organizationNodes.item(i);
            metadata.getOrganisations().add(mapConsortiumMember(orgElement, xpath, english));
        }

        var eventNodes = (NodeList) xpath.evaluate(
                "//*[local-name()='event']", document, XPathConstants.NODESET);

        for (var i = 0; i < eventNodes.getLength(); i++) {
            var evemtElement = (Element) eventNodes.item(i);
            metadata.getEvents().add(mapEvent(evemtElement, xpath, english));
        }

        // EU/Horizon projects never report individual investigators, this
        // data isn't publicly exposed, so "investigators" field stays empty here.
    }

    // TODO: Replace hardcoded EN language tag with the right one
    private PrepopulatedOrganisationDTO mapConsortiumMember(Element orgElement, XPath xpath,
                                                            LanguageTag lang)
            throws Exception {
        var member = new PrepopulatedOrganisationDTO();

        var type = orgElement.getAttribute("type");
        member.setContributionType(mapCordisTypeToContributionType(type));

        var legalName = evaluateText(xpath, orgElement, "./*[local-name()='legalName']");
        if (Objects.nonNull(legalName)) {
            member.getOrganisationName().add(new MultilingualContentDTO(
                    lang.getId(), lang.getLanguageTag(), legalName, 1));
        }

        member.setCountry(evaluateText(xpath, orgElement,
                "./*[local-name()='address']/*[local-name()='country']"));

        var netContributionAttr = orgElement.getAttribute("netEcContribution");
        if (!netContributionAttr.isBlank()) {
            var currency = currencyService.findCurrencyByCode(CURRENCY_CODE);
            try {
                member.setNetContribution(
                        new MonetaryAmountDTO(currency, Double.parseDouble(netContributionAttr)));
            } catch (NumberFormatException e) {
                log.warn("Unable to parse netEcContribution value: {}", netContributionAttr);
            }
        }

        return member;
    }

    private PrepopulatedEventDTO mapEvent(Element eventElement, XPath xpath, LanguageTag lang)
            throws Exception {
        var event = new PrepopulatedEventDTO();

        var title = evaluateText(xpath, eventElement, "./*[local-name()='title']");
        if (Objects.nonNull(title)) {
            event.getName().add(new MultilingualContentDTO(
                    lang.getId(), lang.getLanguageTag(), title, 1));
        }

        var teaser = evaluateText(xpath, eventElement, "./*[local-name()='teaser']");
        if (Objects.nonNull(teaser)) {
            event.getDescription().add(new MultilingualContentDTO(
                    lang.getId(), lang.getLanguageTag(), teaser, 1));
        }

        event.setCity(evaluateText(xpath, eventElement,
                "./*[local-name()='address']/*[local-name()='city']"));
        event.setCountryCode(evaluateText(xpath, eventElement,
                "./*[local-name()='address']/*[local-name()='country']"));

        return event;
    }


    @Nullable
    private String evaluateText(XPath xpath, Node context, String expression) throws Exception {
        var result = (String) xpath.evaluate(expression, context, XPathConstants.STRING);
        return Objects.isNull(result) || result.isBlank() ? null : result.trim();
    }

    @Nullable
    private OrganisationUnitProjectContributionType mapCordisTypeToContributionType(String cordisType) {
        return switch (cordisType) {
            case "coordinator" -> OrganisationUnitProjectContributionType.COORDINATOR;
            case "participant" -> OrganisationUnitProjectContributionType.PARTNER;
            case "thirdParty" -> OrganisationUnitProjectContributionType.INKIND_CONTRIBUTOR;
            default -> {
                log.warn("Unknown CORDIS organization type: {}", cordisType);
                yield null;
            }
        };
    }

    @Nullable
    private ProjectStatus mapCordisStatusToProjectStatus(String cordisStatus) {
        return switch (cordisStatus) {
            case "SIGNED" -> ProjectStatus.ONGOING;
            case "CLOSED" -> ProjectStatus.CONCLUDED;
            case "TERMINATED" -> ProjectStatus.CANCELLED; // CORDIS website could be queried by this status but fetching a project with it results in request time out
            default -> {
                log.warn("Unknown CORDIS project status: {}", cordisStatus);
                yield null;
            }
        };
    }

}