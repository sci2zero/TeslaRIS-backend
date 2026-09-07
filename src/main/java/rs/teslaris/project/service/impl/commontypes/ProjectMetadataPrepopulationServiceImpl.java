package rs.teslaris.project.service.impl.commontypes;

import com.fasterxml.jackson.databind.JsonNode;
import jakarta.annotation.Nullable;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.tika.language.detect.LanguageDetector;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;
import rs.teslaris.core.dto.commontypes.MonetaryAmountDTO;
import rs.teslaris.core.dto.commontypes.MultilingualContentDTO;
import rs.teslaris.core.service.interfaces.commontypes.CurrencyService;
import rs.teslaris.core.service.interfaces.commontypes.LanguageTagService;
import rs.teslaris.core.service.interfaces.institution.OrganisationUnitService;
import rs.teslaris.core.service.interfaces.person.PersonService;
import rs.teslaris.core.util.language.LanguageAbbreviations;
import rs.teslaris.core.util.search.StringUtil;
import rs.teslaris.project.dto.project.PrepopulatedOrganisationDTO;
import rs.teslaris.project.dto.project.PrepopulatedPersonDTO;
import rs.teslaris.project.dto.project.PrepopulatedProjectMetadataDTO;
import rs.teslaris.project.model.project.PersonProjectContributionType;
import rs.teslaris.project.service.interfaces.commontypes.CordisFundingDataService;
import rs.teslaris.project.service.interfaces.commontypes.CordisProjectDataService;
import rs.teslaris.project.service.interfaces.commontypes.FundingMetadataPrepopulationService;
import rs.teslaris.project.service.interfaces.commontypes.ProjectMetadataPrepopulationService;
import rs.teslaris.project.util.CordisDoiUtil;
import rs.teslaris.project.util.CrossrefWorksClient;
import rs.teslaris.project.util.CordisXmlClient;

import java.util.Objects;

@Service
@RequiredArgsConstructor
@Slf4j
public class ProjectMetadataPrepopulationServiceImpl
        implements ProjectMetadataPrepopulationService {

    private final CrossrefWorksClient crossrefWorksClient;

    private final LanguageTagService languageTagService;

    private final CurrencyService currencyService;

    private final CordisXmlClient cordisXmlClient;

    private final CordisProjectDataService cordisProjectDataService;

    private final CordisFundingDataService cordisFundingDataService;

    private final FundingMetadataPrepopulationService fundingMetadataPrepopulationService;

    private final PersonService personService;

    private final OrganisationUnitService organisationUnitService;

    private final LanguageDetector languageDetector;

    @Override
    public PrepopulatedProjectMetadataDTO fetchProjectDataForDoi(String doi) {
        var metadata = CordisDoiUtil.isEuHorizonDoi(doi)
                ? fetchFromCordis(doi)
                : fetchFromCrossref(doi);

        resolveExistingEntities(metadata);

        return metadata;
    }

    private void resolveExistingEntities(PrepopulatedProjectMetadataDTO metadata) {
        metadata.getPersons().forEach(this::resolvePerson);
        metadata.getOrganisations().forEach(this::resolveOrganisation);
    }

    private void resolvePerson(PrepopulatedPersonDTO person) {
        if (!StringUtils.hasText(person.getOrcid())) {
            return;
        }

        var orcid = StringUtil.normalizeIdentifier(person.getOrcid());

        try {
            var index = personService.findPersonByImportIdentifier(orcid);
            if (Objects.nonNull(index)) {
                person.setPersonId(index.getDatabaseId());
            }
        } catch (Exception e) {
            // An unmatched contributor is a valid outcome, so a failing lookup must not sink the
            // whole prepopulation request.
            log.warn("Person lookup by ORCID {} failed: {}", orcid, e.getMessage());
        }
    }

    private void resolveOrganisation(PrepopulatedOrganisationDTO organisation) {
        try {
            var index = organisationUnitService.findOrganisationUnitByTaxNumber(
                    organisation.getVatNumber());
            if (Objects.nonNull(index)) {
                organisation.setOrganisationId(index.getDatabaseId());
            }
        } catch (Exception e) {
            // An unmatched member is a valid outcome, same as for persons.
            log.warn("Organisation lookup by VAT (tax) number {} failed: {}",
                    organisation.getVatNumber(), e.getMessage());
        }
    }

    private PrepopulatedProjectMetadataDTO fetchFromCordis(String doi) {
        var document = cordisXmlClient.fetchDocument(
                CordisDoiUtil.extractCordisProjectId(doi));

        var metadata = cordisProjectDataService.mapProjectMetadata(document, doi);
        metadata.setFunding(cordisFundingDataService.mapFundingMetadata(document, doi));

        return metadata;
    }

    private PrepopulatedProjectMetadataDTO fetchFromCrossref(String doi) {
        var message = crossrefWorksClient.fetchWorkMessage(doi);

        var metadata = Objects.nonNull(message)
                ? mapToProjectDTO(message)
                : new PrepopulatedProjectMetadataDTO();
        metadata.setFunding(
                fundingMetadataPrepopulationService.mapCrossrefFundingData(message, doi));

        return metadata;
    }

    private PrepopulatedProjectMetadataDTO mapToProjectDTO(JsonNode message) {
        var metadata = new PrepopulatedProjectMetadataDTO();

        if (!"grant".equals(message.path("type").asText())) {
            log.warn("DOI {} is not a grant record (type={})",
                    message.path("DOI").asText(), message.path("type").asText());
            return metadata;
        }

        metadata.setDoi(message.path("DOI").asText(null));

        addUriIfValid(metadata, message.path("URL").asText(null));
        addUriIfValid(metadata, message.path("resource").path("primary").path("URL").asText(null));

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

    private void addUriIfValid(PrepopulatedProjectMetadataDTO metadata, @Nullable String rawUri) {
        var sanitizedUri = StringUtil.sanitizeUrl(rawUri);

        if (Objects.nonNull(sanitizedUri) && !metadata.getUris().contains(sanitizedUri)) {
            metadata.getUris().add(sanitizedUri);
        }
    }

    private void populateFromProject(PrepopulatedProjectMetadataDTO metadata,
                                     JsonNode projectNode) {

        projectNode.path("project-title").forEach(titleNode -> {
            var titleText = titleNode.path("title").asText(null);
            if (Objects.isNull(titleText)) {
                return;
            }

            var language = titleNode.path("language").asText(null);

            var alreadyPresent = metadata.getName().stream()
                    .anyMatch(c -> c.getContent().equalsIgnoreCase(titleText.trim()))
                    || metadata.getNameAbbreviation().stream()
                    .anyMatch(c -> c.getContent().equalsIgnoreCase(titleText.trim()));

            if (alreadyPresent) {
                return;
            }

            var content = resolveMultilingualContent(titleText, language);

            if (StringUtil.looksLikeAbbreviation(titleText)) {
                metadata.getNameAbbreviation().add(content);
            } else {
                metadata.getName().add(content);
            }
        });

        projectNode.path("project-description").forEach(descNode -> {
            var descText = descNode.path("description").asText(null);
            if (Objects.nonNull(descText)) {
                var language = descNode.path("language").asText(null);
                metadata.getDescription().add(resolveMultilingualContent(descText, language));
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

        projectNode.path("lead-investigator").forEach(invNode ->
                metadata.getPersons().add(mapInvestigator(invNode,
                        PersonProjectContributionType.PRINCIPLE_INVESTIGATOR)));

        // Should we set the TEAM_MEMBER as the default contributionRole?
        projectNode.path("investigator").forEach(invNode ->
                metadata.getPersons().add(mapInvestigator(invNode,
                        PersonProjectContributionType.TEAM_MEMBER)));
    }

    private PrepopulatedPersonDTO mapInvestigator(JsonNode invNode, PersonProjectContributionType contributionType) {
        var investigator = new PrepopulatedPersonDTO();
        investigator.setContributionType(contributionType);
        investigator.setGivenName(invNode.path("given").asText(null));
        investigator.setFamilyName(invNode.path("family").asText(null));
        investigator.setOrcid(invNode.path("ORCID").asText(null));

        var affiliationArray = invNode.path("affiliation");
        if (affiliationArray.isArray() && !affiliationArray.isEmpty()) {
            var affiliation = affiliationArray.get(0);

            var affiliationName = affiliation.path("name").asText(null);
            if (Objects.nonNull(affiliationName)) {
                investigator.getAffiliationName().add(resolveMultilingualContent(affiliationName, null));
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

    private MultilingualContentDTO resolveMultilingualContent(String text, String language) {
        var code = StringUtils.hasText(language)
                ? language.trim().toUpperCase()
                : languageDetector.detect(text).getLanguage().toUpperCase();

        if (LanguageAbbreviations.CROATIAN.equals(code)) {
            code = LanguageAbbreviations.SERBIAN;
        }

        var languageTag = languageTagService.findLanguageTagByValue(code);

        if (Objects.isNull(languageTag.getId())) {
            log.warn("No language tag for code '{}', falling back to English.", code);
            languageTag = languageTagService.findLanguageTagByValue(LanguageAbbreviations.ENGLISH);
        }

        return new MultilingualContentDTO(languageTag.getId(), languageTag.getLanguageTag(), text, 1);
    }
}
