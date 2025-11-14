package rs.teslaris.exporter.model.converter.skgif;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.stream.Collectors;
import rs.teslaris.core.model.skgif.agent.Agent;
import rs.teslaris.core.model.skgif.agent.SKGIFAffiliation;
import rs.teslaris.core.model.skgif.agent.SKGIFOrganisation;
import rs.teslaris.core.model.skgif.agent.SKGIFPerson;
import rs.teslaris.core.model.skgif.agent.TimePeriod;
import rs.teslaris.core.model.skgif.common.SKGIFIdentifier;
import rs.teslaris.core.util.language.LanguageAbbreviations;
import rs.teslaris.core.util.persistence.IdentifierUtil;
import rs.teslaris.core.util.search.StringUtil;
import rs.teslaris.exporter.model.common.ExportMultilingualContent;
import rs.teslaris.exporter.model.common.ExportOrganisationUnit;
import rs.teslaris.exporter.model.common.ExportPerson;

public class AgentConverter extends BaseConverter {

    public static List<Agent> toSKGIF(ExportPerson person) {
        var organisations = new ArrayList<SKGIFOrganisation>();

        var affiliations = person.getEmployments().stream()
            .map(employment -> {
                String affiliationName =
                    extractTitleFromMC(employment.getEmploymentInstitution().getName());

                var institutionId = employment.getEmploymentInstitution().getDatabaseId();
                if (Objects.nonNull(institutionId)) {
                    organisations.addAll(toSKGIF(employment.getEmploymentInstitution()));
                }

                return SKGIFAffiliation.builder()
                    .affiliation(Objects.nonNull(institutionId) ?
                        (IdentifierUtil.identifierPrefix + institutionId) : affiliationName)
                    .role(employment.getRole())
                    .period(new TimePeriod(
                        Objects.nonNull(employment.getDateFrom()) ?
                            employment.getDateFrom().atStartOfDay() : null,
                        Objects.nonNull(employment.getDateTo()) ?
                            employment.getDateTo().atTime(23, 59) : null
                    ))
                    .build();
            })
            .collect(Collectors.toList());

        var identifiers = getPersonIdentifiers(person);

        var response = new ArrayList<Agent>(List.of(SKGIFPerson.builder()
            .localIdentifier(IdentifierUtil.identifierPrefix + person.getDatabaseId())
            .entityType("person")
            .name(person.getName().toString())
            .identifiers(identifiers)
            .givenName(person.getName().getFirstName())
            .familyName(person.getName().getLastName())
            .affiliations(affiliations)
            .build()));

        response.addAll(organisations);

        return response;
    }

    public static List<SKGIFOrganisation> toSKGIF(ExportOrganisationUnit institution) {
        var institutionName = extractTitleFromMC(institution.getName());

        var otherNames = Optional.ofNullable(institution.getName())
            .orElse(Collections.emptyList())
            .stream()
            .filter(Objects::nonNull)
            .filter(mc ->
                !LanguageAbbreviations.ENGLISH.equalsIgnoreCase(mc.getLanguageTag()) &&
                    !mc.getContent().equals(institutionName))
            .map(ExportMultilingualContent::getContent)
            .filter(Objects::nonNull)
            .collect(Collectors.toCollection(ArrayList::new));

        var identifiers = new ArrayList<SKGIFIdentifier>();
        if (StringUtil.valueExists(institution.getScopusAfid())) {
            identifiers.add(new SKGIFIdentifier("url",
                "https://www.scopus.com/pages/organization/" + institution.getScopusAfid()));
        }

        if (StringUtil.valueExists(institution.getRor())) {
            identifiers.add(new SKGIFIdentifier("ror", institution.getRor()));
        }

        if (StringUtil.valueExists(institution.getOpenAlex())) {
            identifiers.add(new SKGIFIdentifier("openalex", institution.getOpenAlex()));
        }

        var website = "";
        if (Objects.nonNull(institution.getUris()) && !institution.getUris().isEmpty()) {
            website = institution.getUris().getFirst();
        }

        return List.of(SKGIFOrganisation.builder()
            .localIdentifier(IdentifierUtil.identifierPrefix + institution.getDatabaseId())
            .entityType("organisation")
            .name(institutionName)
            .otherNames(otherNames)
            .shortName(institution.getNameAbbreviation())
            .website(website.isBlank() ? null : website)
            .country(institution.getCountry())
            .identifiers(identifiers)
            .types(List.of("research"))
            .build());
    }

    public static List<SKGIFIdentifier> getPersonIdentifiers(ExportPerson person) {
        var identifiers = new ArrayList<SKGIFIdentifier>();
        if (StringUtil.valueExists(person.getOrcid())) {
            identifiers.add(new SKGIFIdentifier("orcid", person.getOrcid()));
        }

        if (StringUtil.valueExists(person.getOpenAlexId())) {
            identifiers.add(new SKGIFIdentifier("openalex", person.getOpenAlexId()));
        }

        if (StringUtil.valueExists(person.getScopusAuthorId())) {
            identifiers.add(new SKGIFIdentifier("url",
                "https://www.scopus.com/authid/detail.uri?authorId=" + person.getScopusAuthorId()));
        }

        return identifiers;
    }
}
