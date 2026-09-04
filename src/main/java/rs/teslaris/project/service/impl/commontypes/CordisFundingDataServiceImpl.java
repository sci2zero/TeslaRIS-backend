package rs.teslaris.project.service.impl.commontypes;

import jakarta.annotation.Nullable;
import java.util.ArrayList;
import java.util.Objects;
import javax.xml.xpath.XPath;
import javax.xml.xpath.XPathConstants;
import javax.xml.xpath.XPathFactory;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.w3c.dom.Document;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;
import rs.teslaris.core.dto.commontypes.MonetaryAmountDTO;
import rs.teslaris.core.dto.commontypes.MultilingualContentDTO;
import rs.teslaris.core.service.interfaces.commontypes.CurrencyService;
import rs.teslaris.core.service.interfaces.commontypes.LanguageTagService;
import rs.teslaris.core.util.search.StringUtil;
import rs.teslaris.project.dto.funding.PrepopulatedFundingMetadataDTO;
import rs.teslaris.project.service.interfaces.commontypes.CordisFundingDataService;
import rs.teslaris.project.util.CordisXmlClient;

@Service
@RequiredArgsConstructor
@Slf4j
public class CordisFundingDataServiceImpl implements CordisFundingDataService {

    private static final String EU_FUNDER_DISPLAY_NAME = "European Commission";

    private static final String CURRENCY_CODE = "EUR";

    private final CordisXmlClient cordisXmlClient;

    private final LanguageTagService languageTagService;

    private final CurrencyService currencyService;

    @Override
    public PrepopulatedFundingMetadataDTO fetchMetadata(String cordisProjectId, String doi) {
        return mapFundingMetadata(cordisXmlClient.fetchDocument(cordisProjectId), doi);
    }

    @Override
    public PrepopulatedFundingMetadataDTO mapFundingMetadata(@Nullable Document document,
                                                             String doi) {
        var metadata = new PrepopulatedFundingMetadataDTO();

        if (Objects.isNull(document)) {
            return metadata;
        }

        try {
            metadata.setDoi(doi);
            populateFromDocument(metadata, document);
        } catch (Exception e) {
            log.error("Failed to map CORDIS funding metadata for DOI {}: {}", doi, e.getMessage());
        }

        return metadata;
    }

    // TODO: Replace hardcoded EN language tag with the right one
    // CORDIS returns the whole XML document in a single language (default EN)
    // but it also returns a list of supported languages which could be used
    // to perform secondary fetch to get data in other languages
    private void populateFromDocument(PrepopulatedFundingMetadataDTO metadata, Document document)
            throws Exception {
        var xpath = XPathFactory.newInstance().newXPath();
        var english = languageTagService.findLanguageTagByValue("EN");

        // For EC grants the CORDIS project id is the grant agreement number - the minted DOI is
        // literally "10.3030/<that id>".
        metadata.setGrantAgreementId(evaluateText(xpath, document, projectField("id")));

        var title = evaluateText(xpath, document, projectField("title"));
        if (Objects.nonNull(title)) {
            metadata.getName().add(new MultilingualContentDTO(
                    english.getId(), english.getLanguageTag(), title, 1));
        }

        var acronym = evaluateText(xpath, document, projectField("acronym"));
        if (Objects.nonNull(acronym)) {
            metadata.getNameAbbreviation().add(new MultilingualContentDTO(
                    english.getId(), english.getLanguageTag(), acronym, 1));
        }

        var objective = evaluateText(xpath, document, projectField("objective"));
        if (Objects.nonNull(objective)) {
            metadata.getDescription().add(new MultilingualContentDTO(
                    english.getId(), english.getLanguageTag(), objective, 1));
        }

        var keywords = evaluateText(xpath, document, projectField("keywords"));
        if (Objects.nonNull(keywords)) {
            metadata.getKeywords().add(new MultilingualContentDTO(
                    english.getId(), english.getLanguageTag(), keywords, 1));
        }

        metadata.setDateFrom(evaluateText(xpath, document, projectField("startDate")));
        metadata.setDateTo(evaluateText(xpath, document, projectField("endDate")));
        metadata.setDateAwarded(evaluateText(xpath, document, projectField("ecSignatureDate")));

        var ecMaxContribution = evaluateText(xpath, document, projectField("ecMaxContribution"));
        if (Objects.nonNull(ecMaxContribution)) {
            var currency = currencyService.findCurrencyByCode(CURRENCY_CODE);
            try {
                metadata.setMonetaryAmount(
                        new MonetaryAmountDTO(currency, Double.parseDouble(ecMaxContribution)));
            } catch (NumberFormatException e) {
                log.warn("Unable to parse ecMaxContribution value: {}", ecMaxContribution);
            }
        }

        metadata.getDisplayFunder().add(new MultilingualContentDTO(
                english.getId(), english.getLanguageTag(), EU_FUNDER_DISPLAY_NAME, 1));

        // CORDIS does not expose a DOI for the funder itself, only Crossref does.

        var call = evaluateText(xpath, document, association("call", "relatedMasterCall"));
        if (Objects.nonNull(call)) {
            metadata.getDisplayCall().add(new MultilingualContentDTO(
                    english.getId(), english.getLanguageTag(), call, 1));
        }

        var programme = evaluateText(xpath, document,
                association("programme", "relatedLegalBasis"));
        if (Objects.nonNull(programme)) {
            metadata.getDisplayProgram().add(new MultilingualContentDTO(
                    english.getId(), english.getLanguageTag(), programme, 1));
        }

        var uriNodes = (NodeList) xpath.evaluate(
                "//*[local-name()='webLink'][@represents='project'][@type='relatedWebsite']"
                        + "/*[local-name()='physUrl']", document, XPathConstants.NODESET);
        metadata.setUris(new ArrayList<>());
        for (var i = 0; i < uriNodes.getLength(); i++) {
            var url = StringUtil.sanitizeUrl(uriNodes.item(i).getTextContent());
            if (Objects.nonNull(url) && !metadata.getUris().contains(url)) {
                metadata.getUris().add(url);
            }
        }
    }

    private String projectField(String name) {
        return "/*[local-name()='project']/*[local-name()='" + name + "']";
    }

    private String association(String elementName, String type) {
        return "/*[local-name()='project']/*[local-name()='relations']"
                + "/*[local-name()='associations']"
                + "/*[local-name()='" + elementName + "'][@type='" + type + "']"
                + "/*[local-name()='title']";
    }

    @Nullable
    private String evaluateText(XPath xpath, Node context, String expression) throws Exception {
        var result = (String) xpath.evaluate(expression, context, XPathConstants.STRING);
        return Objects.isNull(result) || result.isBlank() ? null : result.trim();
    }
}
