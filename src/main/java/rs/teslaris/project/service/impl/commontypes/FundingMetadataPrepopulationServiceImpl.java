package rs.teslaris.project.service.impl.commontypes;

import com.fasterxml.jackson.databind.JsonNode;
import jakarta.annotation.Nullable;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import rs.teslaris.core.dto.commontypes.MonetaryAmountDTO;
import rs.teslaris.core.dto.commontypes.MultilingualContentDTO;
import rs.teslaris.core.service.interfaces.commontypes.CurrencyService;
import rs.teslaris.core.service.interfaces.commontypes.LanguageTagService;
import rs.teslaris.core.util.search.StringUtil;
import rs.teslaris.project.dto.funding.PrepopulatedFundingMetadataDTO;
import rs.teslaris.project.service.interfaces.commontypes.CordisFundingDataService;
import rs.teslaris.project.service.interfaces.commontypes.FundingMetadataPrepopulationService;
import rs.teslaris.project.util.CordisDoiUtil;
import rs.teslaris.project.util.CrossrefWorksClient;

import java.util.Objects;

@Service
@RequiredArgsConstructor
@Slf4j
public class FundingMetadataPrepopulationServiceImpl implements FundingMetadataPrepopulationService {

    private final CrossrefWorksClient crossrefWorksClient;

    private final LanguageTagService languageTagService;

    private final CurrencyService currencyService;

    private final CordisFundingDataService cordisFundingDataService;

    @Override
    public PrepopulatedFundingMetadataDTO fetchFundingDataForDoi(String doi) {
        if (CordisDoiUtil.isEuHorizonDoi(doi)) {
            return cordisFundingDataService.fetchMetadata(
                    CordisDoiUtil.extractCordisProjectId(doi), doi);
        }

        return mapCrossrefFundingData(crossrefWorksClient.fetchWorkMessage(doi), doi);
    }

    @Override
    public PrepopulatedFundingMetadataDTO mapCrossrefFundingData(@Nullable JsonNode message,
                                                                 String doi) {
        var metadata = new PrepopulatedFundingMetadataDTO();

        if (Objects.isNull(message)) {
            return metadata;
        }

        if (!"grant".equals(message.path("type").asText())) {
            log.warn("DOI {} is not a grant record (type={})",
                    doi, message.path("type").asText());
            return metadata;
        }

        metadata.setDoi(message.path("DOI").asText(doi));
        metadata.setGrantAgreementId(message.path("award").asText(null));
        metadata.setDateAwarded(StringUtil.parseDateParts(message.path("issued").path("date-parts")));

        var resourceUrl = StringUtil.sanitizeUrl(
                message.path("resource").path("primary").path("URL").asText(null));
        var doiUrl = StringUtil.sanitizeUrl(message.path("URL").asText(null));

        if (Objects.nonNull(resourceUrl)) {
            metadata.getUris().add(resourceUrl);
        } else if (Objects.nonNull(doiUrl)) {
            metadata.getUris().add(doiUrl);
        }

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
