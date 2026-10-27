package rs.teslaris.revisioner.unit;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

import java.util.List;
import java.util.Set;
import org.junit.jupiter.api.Test;
import rs.teslaris.core.dto.commontypes.MultilingualContentDTO;
import rs.teslaris.core.dto.document.ConferenceDTO;
import rs.teslaris.core.dto.document.JournalPublicationDTO;
import rs.teslaris.core.dto.document.PublicationSeriesDTO;
import rs.teslaris.core.dto.institution.OrganisationUnitDTO;
import rs.teslaris.core.dto.person.PersonSnapshotDTO;
import rs.teslaris.core.dto.person.PersonalInfoDTO;
import rs.teslaris.revisioner.util.fixators.impl.IdentifierAffixFixator;

public class IdentifierAffixFixatorTest {

    private final IdentifierAffixFixator identifierAffixFixator = new IdentifierAffixFixator();

    private static MultilingualContentDTO content(String text) {
        var multilingualContent = new MultilingualContentDTO();
        multilingualContent.setContent(text);
        return multilingualContent;
    }

    @Test
    public void shouldStripResolverPrefixAndTrailingPunctuationFromDoi() {
        var document = new JournalPublicationDTO();
        document.setDoi("  https://doi.org/10.1234/abcd-123, ");

        identifierAffixFixator.applyFix(document);

        assertEquals("10.1234/abcd-123", document.getDoi());
    }

    @Test
    public void shouldLeaveIdentifierUntouchedWhenCleaningCannotProduceValidValue() {
        var document = new JournalPublicationDTO();
        document.setDoi("not an identifier at all");

        identifierAffixFixator.applyFix(document);

        assertEquals("not an identifier at all", document.getDoi());
    }

    @Test
    public void shouldHarvestDocumentIdentifiersFromUris() {
        var document = new JournalPublicationDTO();
        document.setUris(
            Set.of("https://doi.org/10.1234/abcd", "https://openalex.org/w1234567890"));

        identifierAffixFixator.applyFix(document);

        assertEquals("10.1234/abcd", document.getDoi());
        assertEquals("W1234567890", document.getOpenAlexId());
    }

    @Test
    public void shouldHarvestDocumentIdentifierFromDescription() {
        var document = new JournalPublicationDTO();
        document.setDescription(
            List.of(content("Available at https://hdl.handle.net/20.500.12345/abc, see also.")));

        identifierAffixFixator.applyFix(document);

        assertEquals("20.500.12345/abc", document.getHandleId());
    }

    @Test
    public void shouldNotHarvestWhenIdentifierIsAlreadySet() {
        var document = new JournalPublicationDTO();
        document.setDoi("10.5555/kept");
        document.setUris(Set.of("https://doi.org/10.1234/harvested"));

        identifierAffixFixator.applyFix(document);

        assertEquals("10.5555/kept", document.getDoi());
    }

    @Test
    public void shouldNotHarvestIdentifierWithoutResolverPrefix() {
        var document = new JournalPublicationDTO();
        document.setDescription(List.of(content("The record number is 123456789.")));

        identifierAffixFixator.applyFix(document);

        assertNull(document.getScopusId());
    }

    @Test
    public void shouldNormalisePersonIdentifiers() {
        var personalInfo = new PersonalInfoDTO();
        personalInfo.setOrcid("https://orcid.org/0000-0002-1825-009x");
        personalInfo.setUris(
            Set.of("https://www.scopus.com/authid/detail.uri?authorId=7004212771"));

        var person = new PersonSnapshotDTO();
        person.setPersonalInfo(personalInfo);

        identifierAffixFixator.applyFix(person);

        assertEquals("0000-0002-1825-009X", personalInfo.getOrcid());
        assertEquals("7004212771", personalInfo.getScopusAuthorId());
    }

    @Test
    public void shouldNormaliseOrganisationUnitIdentifiers() {
        var organisationUnit = new OrganisationUnitDTO();
        organisationUnit.setRor("https://ror.org/02MHBDP94");
        organisationUnit.setDescription(
            List.of(content("Listed as https://www.wikidata.org/wiki/Q1234 in Wikidata.")));

        identifierAffixFixator.applyFix(organisationUnit);

        assertEquals("02mhbdp94", organisationUnit.getRor());
        assertEquals("Q1234", organisationUnit.getWikidata());
    }

    @Test
    public void shouldCleanPublicationSeriesIssnWithoutHarvestingIt() {
        var publicationSeries = new PublicationSeriesDTO();
        publicationSeries.setEissn("https://portal.issn.org/resource/ISSN/0317-847x");
        publicationSeries.setUris(Set.of("https://portal.issn.org/resource/ISSN/1234-5678"));

        identifierAffixFixator.applyFix(publicationSeries);

        assertEquals("0317-847X", publicationSeries.getEissn());
        assertNull(publicationSeries.getPrintISSN());
    }

    @Test
    public void shouldNormaliseConferenceIdentifiers() {
        var conference = new ConferenceDTO();
        conference.setConfId("A1B2C3D4-1111-2222-3333-444455556666");
        conference.setUris(Set.of("https://openalex.org/S4210176314"));

        identifierAffixFixator.applyFix(conference);

        assertEquals("a1b2c3d4-1111-2222-3333-444455556666", conference.getConfId());
        assertEquals("S4210176314", conference.getOpenAlexId());
    }
}
