package rs.teslaris.revisioner.util.fixators.impl;

import org.springframework.stereotype.Component;
import rs.teslaris.core.dto.document.DocumentDTO;
import rs.teslaris.core.dto.document.EventDTO;
import rs.teslaris.core.dto.institution.OrganisationUnitDTO;
import rs.teslaris.core.dto.person.PersonSnapshotDTO;
import rs.teslaris.core.revision.DataQualityFixator;

/**
 * Replaces the typographic characters harvested metadata arrives with by their plain equivalents,
 * not implemented yet.
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
        // TODO: normalise the multilingual title, subtitle, description and keywords.
    }

    @Override
    public void applyFix(PersonSnapshotDTO person) {
        // TODO: normalise the person's names and biography.
    }

    @Override
    public void applyFix(OrganisationUnitDTO organisationUnit) {
        // TODO: normalise the unit's name and keywords.
    }

    @Override
    public void applyFix(EventDTO event) {
        // TODO: normalise the event's name, name abbreviation, place and description.
    }
}
