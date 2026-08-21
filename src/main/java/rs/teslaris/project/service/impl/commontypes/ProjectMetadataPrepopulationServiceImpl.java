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
import rs.teslaris.core.model.commontypes.LanguageTag;
import rs.teslaris.core.service.interfaces.commontypes.CurrencyService;
import rs.teslaris.core.service.interfaces.commontypes.LanguageTagService;
import rs.teslaris.core.util.search.StringUtil;
import rs.teslaris.core.util.session.RestTemplateProvider;
import rs.teslaris.project.dto.project.PrepopulatedPersonDTO;
import rs.teslaris.project.dto.project.PrepopulatedProjectMetadataDTO;
import rs.teslaris.project.service.interfaces.commontypes.CordisProjectDataService;
import rs.teslaris.project.service.interfaces.commontypes.ProjectMetadataPrepopulationService;

import java.util.Objects;

@Service
@RequiredArgsConstructor
@Slf4j
public class ProjectMetadataPrepopulationServiceImpl
        implements ProjectMetadataPrepopulationService {

    private static final String CROSSREF_WORKS_URL = "https://api.crossref.org/works/";

    private final RestTemplateProvider restTemplateProvider;

    private final LanguageTagService languageTagService;

    private final CurrencyService currencyService;

    private final CordisProjectDataService cordisProjectDataService;

    @Override
    public PrepopulatedProjectMetadataDTO fetchProjectDataForDoi(String doi) {
        if (isEuHorizonDoi(doi)) {
            return fetchFromCordis(doi);
        }
        return fetchFromCrossref(doi);
    }

    private boolean isEuHorizonDoi(String doi) {
        return doi.startsWith("10.3030/");
    }

    private PrepopulatedProjectMetadataDTO fetchFromCordis(String doi) {
        var cordisProjectId = doi.substring("10.3030/".length());
        return cordisProjectDataService.fetchFullMetadata(cordisProjectId, doi);
    }

    private PrepopulatedProjectMetadataDTO fetchFromCrossref(String doi) {
        var json = fetchRawCrossrefWork(doi);
        if (Objects.isNull(json)) {
            return new PrepopulatedProjectMetadataDTO();
        }
        return mapToProjectDTO(json.path("message"));
    }

    @Nullable
    private JsonNode fetchRawCrossrefWork(String doi) {
        var url = CROSSREF_WORKS_URL + doi;

        var headers = new HttpHeaders();
        headers.set("User-Agent", "TeslaRIS/1.0 (https://github.com/uns-cris/teslaris)");

        var entity = new HttpEntity<>(headers);
        var restTemplate = restTemplateProvider.provideRestTemplate();

        try {
            var response = restTemplate.exchange(url, HttpMethod.GET, entity, JsonNode.class);
            return response.getBody();
        } catch (HttpClientErrorException e) {
            log.warn("Unable to fetch project metadata for DOI: {}. Response code: {}", doi,
                    e.getStatusCode().value());
            return null;
        }
    }

    private PrepopulatedProjectMetadataDTO mapToProjectDTO(JsonNode message) {
        var metadata = new PrepopulatedProjectMetadataDTO();

        if (!"grant".equals(message.path("type").asText())) {
            log.warn("DOI {} is not a grant record (type={})",
                    message.path("DOI").asText(), message.path("type").asText());
            return metadata;
        }

        metadata.setDoi(message.path("DOI").asText(null));

        var projectsNode = message.path("project");
        if (projectsNode.isArray() && !projectsNode.isEmpty()) {
            populateFromProject(metadata, projectsNode.get(0));

            if (projectsNode.size() > 1) {
                log.info("Grant with DOI {} has {} projects, data pulled only from the first one",
                        metadata.getDoi(), projectsNode.size());
            }
        }

        return metadata;
    }

    private void populateFromProject(PrepopulatedProjectMetadataDTO metadata,
                                     JsonNode projectNode) {
        var english = languageTagService.findLanguageTagByValue("EN");

        projectNode.path("project-title").forEach(titleNode -> {
            var titleText = titleNode.path("title").asText(null);
            if (Objects.isNull(titleText)) {
                return;
            }

            var alreadyPresent = metadata.getName().stream()
                    .anyMatch(c -> c.getContent().equalsIgnoreCase(titleText.trim()))
                    || metadata.getNameAbbreviation().stream()
                    .anyMatch(c -> c.getContent().equalsIgnoreCase(titleText.trim()));

            if (alreadyPresent) {
                return;
            }

            var content = new MultilingualContentDTO(
                    english.getId(), english.getLanguageTag(), titleText, 1);

            if (StringUtil.looksLikeAbbreviation(titleText)) {
                metadata.getNameAbbreviation().add(content);
            } else {
                metadata.getName().add(content);
            }
        });

        projectNode.path("project-description").forEach(descNode -> {
            var descText = descNode.path("description").asText(null);
            if (Objects.nonNull(descText)) {
                metadata.getDescription().add(new MultilingualContentDTO(
                        english.getId(), english.getLanguageTag(), descText, 1));
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
                    metadata.setCosts(
                            new MonetaryAmountDTO(currency.getId(), amountValue.asDouble()));
                } else {
                    log.warn("Currency code {} from Crossref not found in local currency table",
                            currencyCode);
                }
            }
        }

        // TODO: Add a new field to DTO that will mark the PRINCIPAL_INVESTIGATOR
        projectNode.path("lead-investigator").forEach(invNode ->
                metadata.getPersons().add(mapInvestigator(invNode, english)));

        projectNode.path("investigator").forEach(invNode ->
                metadata.getPersons().add(mapInvestigator(invNode, english)));
    }

    private PrepopulatedPersonDTO mapInvestigator(JsonNode invNode, LanguageTag english) {
        var investigator = new PrepopulatedPersonDTO();
        investigator.setGivenName(invNode.path("given").asText(null));
        investigator.setFamilyName(invNode.path("family").asText(null));
        investigator.setOrcid(invNode.path("ORCID").asText(null));

        var affiliationArray = invNode.path("affiliation");
        if (affiliationArray.isArray() && !affiliationArray.isEmpty()) {
            var affiliation = affiliationArray.get(0);

            var affiliationName = affiliation.path("name").asText(null);
            if (Objects.nonNull(affiliationName)) {
                // Crossref grant records are English-only, so the tag is known rather than guessed.
                investigator.getAffiliationName().add(new MultilingualContentDTO(
                        english.getId(), english.getLanguageTag(), affiliationName, 1));
            }

            for (var idNode : affiliation.path("id")) {
                if ("ROR".equals(idNode.path("id-type").asText())) {
                    investigator.setAffiliationRor(idNode.path("id").asText(null));
                    break;
                }
            }
        }

        return investigator;
    }

}