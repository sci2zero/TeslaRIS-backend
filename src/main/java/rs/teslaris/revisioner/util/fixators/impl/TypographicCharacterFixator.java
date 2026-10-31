package rs.teslaris.revisioner.util.fixators.impl;

import java.util.Objects;
import org.springframework.stereotype.Component;
import rs.teslaris.core.dto.document.DocumentDTO;
import rs.teslaris.core.dto.document.EventDTO;
import rs.teslaris.core.dto.document.PublicationSeriesDTO;
import rs.teslaris.core.dto.document.PublisherDTO;
import rs.teslaris.core.dto.institution.OrganisationUnitDTO;
import rs.teslaris.core.dto.person.PersonNameDTO;
import rs.teslaris.core.dto.person.PersonSnapshotDTO;
import rs.teslaris.core.revision.DataQualityFixator;
import rs.teslaris.revisioner.util.fixators.TypographicCharacterRules;

/**
 * Replaces the typographic characters harvested metadata arrives with by their plain equivalents.
 */
@Component
public class TypographicCharacterFixator implements DataQualityFixator {

    @Override
    public String key() {
        return "typographicCharacter";
    }

    @Override
    public String name() {
        return "Typographic character replacement";
    }

    @Override
    public void applyFix(DocumentDTO document) {
        TypographicCharacterRules.normalise(document.getTitle());
        TypographicCharacterRules.normalise(document.getSubTitle());
        TypographicCharacterRules.normalise(document.getDescription());
        TypographicCharacterRules.normalise(document.getKeywords());
        TypographicCharacterRules.normalise(document.getCity());
        TypographicCharacterRules.normalise(document.getEdition());
        TypographicCharacterRules.normalise(document.getGeoSpaceDescription());
        TypographicCharacterRules.normalise(document.getChronologicalSpaceDescription());
    }

    @Override
    public void applyFix(PersonSnapshotDTO person) {
        normaliseName(person.getPersonName());

        if (Objects.nonNull(person.getPersonOtherNames())) {
            person.getPersonOtherNames().forEach(this::normaliseName);
        }

        TypographicCharacterRules.normalise(person.getBiography());
        TypographicCharacterRules.normalise(person.getKeyword());

        var personalInfo = person.getPersonalInfo();

        if (Objects.nonNull(personalInfo)) {
            TypographicCharacterRules.normalise(personalInfo.getDisplayTitle());
            personalInfo.setPlaceOfBirth(
                TypographicCharacterRules.normalise(personalInfo.getPlaceOfBirth()));
        }
    }

    @Override
    public void applyFix(OrganisationUnitDTO organisationUnit) {
        TypographicCharacterRules.normalise(organisationUnit.getName());
        TypographicCharacterRules.normalise(organisationUnit.getNameAbbreviation());
        TypographicCharacterRules.normalise(organisationUnit.getDescription());
        TypographicCharacterRules.normalise(organisationUnit.getKeyword());
    }

    @Override
    public void applyFix(EventDTO event) {
        TypographicCharacterRules.normalise(event.getName());
        TypographicCharacterRules.normalise(event.getNameAbbreviation());
        TypographicCharacterRules.normalise(event.getDescription());
        TypographicCharacterRules.normalise(event.getKeywords());
        TypographicCharacterRules.normalise(event.getPlace());
        TypographicCharacterRules.normalise(event.getDisplayOrganizer());
    }

    @Override
    public void applyFix(PublicationSeriesDTO publicationSeries) {
        TypographicCharacterRules.normalise(publicationSeries.getTitle());
        TypographicCharacterRules.normalise(publicationSeries.getNameAbbreviation());
        TypographicCharacterRules.normalise(publicationSeries.getSubtitle());
    }

    @Override
    public void applyFix(PublisherDTO publisher) {
        TypographicCharacterRules.normalise(publisher.getName());
        TypographicCharacterRules.normalise(publisher.getPlace());
        TypographicCharacterRules.normalise(publisher.getState());
    }

    private void normaliseName(PersonNameDTO personName) {
        if (Objects.isNull(personName)) {
            return;
        }

        personName.setFirstname(TypographicCharacterRules.normalise(personName.getFirstname()));
        personName.setOtherName(TypographicCharacterRules.normalise(personName.getOtherName()));
        personName.setLastname(TypographicCharacterRules.normalise(personName.getLastname()));
    }
}
