package rs.teslaris.project.service.impl.commontypes;

import jakarta.annotation.Nullable;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;
import rs.teslaris.core.dto.commontypes.MonetaryAmountDTO;
import rs.teslaris.core.dto.commontypes.MultilingualContentDTO;
import rs.teslaris.core.model.commontypes.LanguageTag;
import rs.teslaris.core.service.interfaces.commontypes.CurrencyService;
import rs.teslaris.core.service.interfaces.commontypes.LanguageTagService;
import rs.teslaris.core.util.search.StringUtil;
import rs.teslaris.project.dto.project.PrepopulatedOrganisationDTO;
import rs.teslaris.project.dto.project.PrepopulatedEventDTO;
import rs.teslaris.project.dto.project.PrepopulatedProjectMetadataDTO;
import rs.teslaris.project.model.project.OrganisationUnitProjectContributionType;
import rs.teslaris.project.model.project.ProjectStatus;
import rs.teslaris.project.service.interfaces.commontypes.CordisProjectDataService;
import rs.teslaris.project.util.CordisXmlClient;

import javax.xml.xpath.XPath;
import javax.xml.xpath.XPathConstants;
import javax.xml.xpath.XPathFactory;
import java.util.ArrayList;
import java.util.Objects;

@Service
@RequiredArgsConstructor
@Slf4j
public class CordisProjectDataServiceImpl implements CordisProjectDataService {

    private static final String EU_FUNDER_DISPLAY_NAME = "European Commission";

    private static final String CURRENCY_CODE = "EUR";

    private final CordisXmlClient cordisXmlClient;

    private final LanguageTagService languageTagService;

    private final CurrencyService currencyService;

    @Override
    public PrepopulatedProjectMetadataDTO fetchMetadata(String cordisProjectId, String doi) {
        return mapProjectMetadata(cordisXmlClient.fetchDocument(cordisProjectId), doi);
    }

    @Override
    public PrepopulatedProjectMetadataDTO mapProjectMetadata(@Nullable Document document,
                                                             String doi) {
        var metadata = new PrepopulatedProjectMetadataDTO();
        metadata.setDoi(doi);

        if (Objects.isNull(document)) {
            return metadata;
        }

        try {
            populateFromDocument(metadata, document);
        } catch (Exception e) {
            log.error("Failed to map CORDIS metadata for DOI {}: {}", doi, e.getMessage());
        }

        return metadata;
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
        if (Objects.nonNull(keywords)) {
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
            var url = StringUtil.sanitizeUrl(uriNodes.item(i).getTextContent());
            if (Objects.nonNull(url)) {
                metadata.getUris().add(url);
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
    // CORDIS returns the whole XML document in a single language (default EN)
    // but it also returns a list of supported languages which could be used
    // to perform secondary fetch to get data in other languages
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

        member.setVatNumber(evaluateText(xpath, orgElement, "./*[local-name()='vatNumber']"));

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