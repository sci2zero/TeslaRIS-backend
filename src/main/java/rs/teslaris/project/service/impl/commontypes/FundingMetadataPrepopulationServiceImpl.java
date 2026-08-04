package rs.teslaris.project.service.impl.commontypes;

import com.fasterxml.jackson.databind.JsonNode;
import jakarta.annotation.Nullable;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.stereotype.Service;
import org.springframework.web.client.HttpClientErrorException;
import rs.teslaris.core.dto.commontypes.MonetaryAmountDTO;
import rs.teslaris.core.dto.commontypes.MultilingualContentDTO;
import rs.teslaris.core.service.interfaces.commontypes.CurrencyService;
import rs.teslaris.core.service.interfaces.commontypes.LanguageTagService;
import rs.teslaris.core.util.search.StringUtil;
import rs.teslaris.core.util.session.RestTemplateProvider;
import rs.teslaris.project.dto.funding.PrepopulatedFundingMetadataDTO;
import rs.teslaris.project.service.interfaces.commontypes.FundingMetadataPrepopulationService;

import java.util.Objects;

@Service
@RequiredArgsConstructor
@Slf4j
public class FundingMetadataPrepopulationServiceImpl implements FundingMetadataPrepopulationService {

    private static final String CROSSREF_WORKS_URL = "https://api.crossref.org/works/";

    private final RestTemplateProvider restTemplateProvider;

    private final LanguageTagService languageTagService;

    private final CurrencyService currencyService;

    @Override
    public PrepopulatedFundingMetadataDTO fetchFundingDataForDoi(String doi) {
        var json = fetchRawCrossrefWork(doi);
        if (Objects.isNull(json)) {
            return new PrepopulatedFundingMetadataDTO();
        }

        return mapToFundingDTO(json.path("message"));
    }

    @Nullable
    private JsonNode fetchRawCrossrefWork(String doi) {
        var url = CROSSREF_WORKS_URL + doi;

        var headers = new HttpHeaders();
        // When this header is set, Crossref gives access to Polite Pool which increases the rate limit from 5 -> 10
        // and concurrency limit from 1 -> 3
        headers.set("User-Agent", "TeslaRIS/1.10 (mailto:teslaris@uns.ac.rs)");

        var entity = new HttpEntity<>(headers);
        var restTemplate = restTemplateProvider.provideRestTemplate();

        try {
            var response =
                    restTemplate.exchange(url, HttpMethod.GET, entity, JsonNode.class);
            return response.getBody();
        } catch (HttpClientErrorException e) {
            log.warn("Unable to fetch grant metadata for DOI: {}. Response code: {}", doi,
                    e.getStatusCode().value());
            return null;
        }
    }

    private PrepopulatedFundingMetadataDTO mapToFundingDTO(JsonNode message) {
        var metadata = new PrepopulatedFundingMetadataDTO();

        if (!"grant".equals(message.path("type").asText())) {
            log.warn("DOI {} is not a grant record (type={})",
                    message.path("DOI").asText(), message.path("type").asText());
            return metadata;
        }

        metadata.setDoi(message.path("DOI").asText(null));
        metadata.setGrantAgreementId(message.path("award").asText(null));
        metadata.setDateAwarded(StringUtil.parseDateParts(message.path("issued").path("date-parts")));

        var resourceUrl = message.path("resource").path("primary").path("URL").asText(null);
        metadata.getUris().add(
                Objects.nonNull(resourceUrl) ? resourceUrl : message.path("URL").asText(null));

        var projectsNode = message.path("project");
        if (projectsNode.isArray() && !projectsNode.isEmpty()) {
            // Crossref's grant JSON schema doesn't have grant-level attributes like name, description, dates...
            // I couldn't find any grants on Crossref that contain multiple projects, so I pulled the data from the
            // single one that was always there
            populateFromProject(metadata, projectsNode.get(0));

            if (projectsNode.size() > 1) {
                log.warn("Grant with DOI {} has {} projects, but the data is only pulled from the first one",
                        metadata.getDoi(), projectsNode.size());
            }
        }

        return metadata;
    }

    private void populateFromProject(PrepopulatedFundingMetadataDTO metadata,
                                     JsonNode projectNode) {
        var english = languageTagService.findLanguageTagByValue("EN");

        projectNode.path("project-title").forEach(titleNode -> {
            var titleText = titleNode.path("title").asText(null);
            if (Objects.nonNull(titleText)) {
                var content = new MultilingualContentDTO(
                        english.getId(), english.getLanguageTag(), titleText, 1);

                if (StringUtil.looksLikeAbbreviation(titleText)) {
                    metadata.getNameAbbreviation().add(content);
                } else {
                    metadata.getName().add(content);
                }
            }
        });

        projectNode.path("project-description").forEach(descNode -> {
            var descText = descNode.path("description").asText(null);
            if (Objects.nonNull(descText)) {
                var content = new MultilingualContentDTO(
                        english.getId(), english.getLanguageTag(), descText, 1);
                metadata.getDescription().add(content);
            }
        });

        metadata.setDateFrom(StringUtil.parseDateParts(projectNode.path("award-start").path("date-parts")));
        metadata.setDateTo(StringUtil.parseDateParts(projectNode.path("award-end").path("date-parts")));

        var awardAmountNode = projectNode.path("award-amount");
        if (!awardAmountNode.isMissingNode()) {
            var amountValue = awardAmountNode.path("amount");
            var currencyCode = awardAmountNode.path("currency").asText(null);

            if (!amountValue.isMissingNode() && Objects.nonNull(currencyCode)) {
                var currency = currencyService.findCurrencyByCode(currencyCode);
                if (Objects.nonNull(currency)) {
                    metadata.setMonetaryAmount(
                            new MonetaryAmountDTO(currency, amountValue.asDouble()));
                } else {
                    log.warn("Currency code {} from Crossref not found in local currency table",
                            currencyCode);
                }
            }
        }

        var fundingArray = projectNode.path("funding");
        if (fundingArray.isArray() && !fundingArray.isEmpty()) {
            var funderNode = fundingArray.get(0).path("funder");
            var funderName = funderNode.path("name").asText(null);
            if (Objects.nonNull(funderName)) {
                metadata.getDisplayFunder().add(new MultilingualContentDTO(
                        english.getId(), english.getLanguageTag(), funderName, 1));
            }

            for (var idNode : funderNode.path("id")) {
                if ("DOI".equals(idNode.path("id-type").asText())) {
                    metadata.setFunderDoi(idNode.path("id").asText(null));
                    break;
                }
            }
        }
    }
}
