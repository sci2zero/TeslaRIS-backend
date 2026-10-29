package rs.teslaris.revisioner.util.fixators.impl;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Objects;
import java.util.function.Consumer;
import java.util.function.Supplier;
import org.springframework.stereotype.Component;
import rs.teslaris.core.dto.commontypes.MultilingualContentDTO;
import rs.teslaris.core.dto.document.ConferenceDTO;
import rs.teslaris.core.dto.document.DocumentDTO;
import rs.teslaris.core.dto.document.EventDTO;
import rs.teslaris.core.dto.document.PublicationSeriesDTO;
import rs.teslaris.core.dto.institution.OrganisationUnitDTO;
import rs.teslaris.core.dto.person.PersonSnapshotDTO;
import rs.teslaris.core.revision.DataQualityFixator;
import rs.teslaris.revisioner.util.fixators.IdentifierAffixRules;
import rs.teslaris.revisioner.util.fixators.IdentifierAffixRules.Rule;

/**
 * Strips the resolver prefix an identifier was pasted with, trims what a copy-paste left behind it
 * and fills an empty identifier from a URI or a description that spells one out.
 */
@Component
public class IdentifierAffixFixator implements DataQualityFixator {

    @Override
    public String key() {
        return "identifierAffix";
    }

    @Override
    public String name() {
        return "Identifier prefix and suffix removal";
    }

    @Override
    public void applyFix(DocumentDTO document) {
        var sources = sources(document.getUris(), document.getDescription());

        apply(IdentifierAffixRules.DOI, document::getDoi, document::setDoi, sources);
        apply(IdentifierAffixRules.DOCUMENT_SCOPUS_ID, document::getScopusId,
            document::setScopusId, sources);
        apply(IdentifierAffixRules.DOCUMENT_OPEN_ALEX_ID, document::getOpenAlexId,
            document::setOpenAlexId, sources);
        apply(IdentifierAffixRules.WEB_OF_SCIENCE_ID, document::getWebOfScienceId,
            document::setWebOfScienceId, sources);
        apply(IdentifierAffixRules.HANDLE_ID, document::getHandleId, document::setHandleId,
            sources);
        apply(IdentifierAffixRules.ARXIV_ID, document::getArxivId, document::setArxivId, sources);
        apply(IdentifierAffixRules.PUBMED_ID, document::getPubmedId, document::setPubmedId,
            sources);
        apply(IdentifierAffixRules.SSRN_ID, document::getSsrnId, document::setSsrnId, sources);
    }

    @Override
    public void applyFix(PersonSnapshotDTO person) {
        var personalInfo = person.getPersonalInfo();

        if (Objects.isNull(personalInfo)) {
            return;
        }

        var sources = sources(personalInfo.getUris(), person.getBiography());

        apply(IdentifierAffixRules.ORCID, personalInfo::getOrcid, personalInfo::setOrcid, sources);
        apply(IdentifierAffixRules.SCOPUS_AUTHOR_ID, personalInfo::getScopusAuthorId,
            personalInfo::setScopusAuthorId, sources);
        apply(IdentifierAffixRules.PERSON_OPEN_ALEX_ID, personalInfo::getOpenAlexId,
            personalInfo::setOpenAlexId, sources);
        apply(IdentifierAffixRules.WEB_OF_SCIENCE_RESEARCHER_ID,
            personalInfo::getWebOfScienceResearcherId, personalInfo::setWebOfScienceResearcherId,
            sources);
        apply(IdentifierAffixRules.SCHOLAR_ID, personalInfo::getScholarId,
            personalInfo::setScholarId, sources);
        apply(IdentifierAffixRules.AUTHENTICUS_ID, personalInfo::getAuthenticusId,
            personalInfo::setAuthenticusId, sources);
        apply(IdentifierAffixRules.LATTES_ID, personalInfo::getLattesId,
            personalInfo::setLattesId, sources);
    }

    @Override
    public void applyFix(OrganisationUnitDTO organisationUnit) {
        var sources = sources(organisationUnit.getUris(), organisationUnit.getDescription());

        apply(IdentifierAffixRules.SCOPUS_AFID, organisationUnit::getScopusAfid,
            organisationUnit::setScopusAfid, sources);
        apply(IdentifierAffixRules.ORGANISATION_UNIT_OPEN_ALEX_ID, organisationUnit::getOpenAlexId,
            organisationUnit::setOpenAlexId, sources);
        apply(IdentifierAffixRules.ROR, organisationUnit::getRor, organisationUnit::setRor,
            sources);
        apply(IdentifierAffixRules.RINGGOLD, organisationUnit::getRinggold,
            organisationUnit::setRinggold, sources);
        apply(IdentifierAffixRules.FUNDREF, organisationUnit::getFundref,
            organisationUnit::setFundref, sources);
        apply(IdentifierAffixRules.ISNI, organisationUnit::getIsni, organisationUnit::setIsni,
            sources);
        apply(IdentifierAffixRules.GRID, organisationUnit::getGrid, organisationUnit::setGrid,
            sources);
        apply(IdentifierAffixRules.WIKIDATA, organisationUnit::getWikidata,
            organisationUnit::setWikidata, sources);
    }

    @Override
    public void applyFix(PublicationSeriesDTO publicationSeries) {
        var sources = sources(publicationSeries.getUris(), List.of());

        apply(IdentifierAffixRules.PUBLICATION_SERIES_OPEN_ALEX_ID,
            publicationSeries::getOpenAlexId, publicationSeries::setOpenAlexId, sources);

        // A harvested ISSN cannot be told apart from the print one, so both are only ever cleaned.
        publicationSeries.setEissn(
            IdentifierAffixRules.clean(IdentifierAffixRules.ISSN, publicationSeries.getEissn()));
        publicationSeries.setPrintISSN(
            IdentifierAffixRules.clean(IdentifierAffixRules.ISSN,
                publicationSeries.getPrintISSN()));
    }

    @Override
    public void applyFix(EventDTO event) {
        if (!(event instanceof ConferenceDTO conference)) {
            return;
        }

        var sources = sources(conference.getUris(), conference.getDescription());

        apply(IdentifierAffixRules.CONFERENCE_OPEN_ALEX_ID, conference::getOpenAlexId,
            conference::setOpenAlexId, sources);
        apply(IdentifierAffixRules.CONF_ID, conference::getConfId, conference::setConfId, sources);
    }

    private void apply(Rule rule, Supplier<String> getter, Consumer<String> setter,
                       List<String> sources) {
        var storedValue = getter.get();

        if (Objects.nonNull(storedValue) && !storedValue.isBlank()) {
            setter.accept(IdentifierAffixRules.clean(rule, storedValue));
            return;
        }

        var harvestedValue = IdentifierAffixRules.harvest(rule, sources);

        if (Objects.nonNull(harvestedValue)) {
            setter.accept(harvestedValue);
        }
    }

    private List<String> sources(Collection<String> uris, List<MultilingualContentDTO> texts) {
        var sources = new ArrayList<String>();

        if (Objects.nonNull(uris)) {
            uris.stream().filter(Objects::nonNull).forEach(sources::add);
        }

        if (Objects.nonNull(texts)) {
            texts.stream().map(MultilingualContentDTO::getContent).filter(Objects::nonNull)
                .forEach(sources::add);
        }

        return sources;
    }
}
