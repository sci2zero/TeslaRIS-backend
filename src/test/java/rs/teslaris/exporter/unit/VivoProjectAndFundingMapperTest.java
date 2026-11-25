package rs.teslaris.exporter.unit;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Set;
import org.apache.jena.datatypes.xsd.XSDDatatype;
import org.apache.jena.rdf.model.Model;
import org.apache.jena.vocabulary.RDF;
import org.apache.jena.vocabulary.RDFS;
import org.junit.jupiter.api.Test;
import org.springframework.context.support.StaticMessageSource;
import rs.teslaris.core.model.funding.FundingType;
import rs.teslaris.core.model.project.OrganisationUnitProjectContributionType;
import rs.teslaris.core.model.project.PersonProjectContributionType;
import rs.teslaris.core.model.project.PersonProjectInvestigationRole;
import rs.teslaris.exporter.model.common.ExportFunding;
import rs.teslaris.exporter.model.common.ExportMonetaryAmount;
import rs.teslaris.exporter.model.common.ExportMultilingualContent;
import rs.teslaris.exporter.model.common.ExportOrganisationUnit;
import rs.teslaris.exporter.model.common.ExportPerson;
import rs.teslaris.exporter.model.common.ExportProject;
import rs.teslaris.exporter.model.common.ExportProjectDocument;
import rs.teslaris.exporter.model.common.ExportProjectOrganisationContribution;
import rs.teslaris.exporter.model.common.ExportProjectPersonContribution;
import rs.teslaris.exporter.model.converter.push.vivo.VivoFundingMapper;
import rs.teslaris.exporter.model.converter.push.vivo.VivoProjectMapper;
import rs.teslaris.exporter.model.converter.push.vivo.VivoTeslarisLinks;
import rs.teslaris.exporter.model.push.PushEntityType;
import rs.teslaris.exporter.util.PushTargetsConfigurationLoader.Auth;
import rs.teslaris.exporter.util.PushTargetsConfigurationLoader.AuthType;
import rs.teslaris.exporter.util.PushTargetsConfigurationLoader.PushTarget;
import rs.teslaris.exporter.util.push.EntityReference;
import rs.teslaris.exporter.util.push.NTriplesSerializer;

public class VivoProjectAndFundingMapperTest {

    private static final String NS = "http://vivo.example/individual/";

    private static final String VIVO = "http://vivoweb.org/ontology/core#";

    private static final String OBO = "http://purl.obolibrary.org/obo/";

    private static final String VCARD = "http://www.w3.org/2006/vcard/ns#";

    private static final String PROJECT = NS + "teslaris-project-4";

    private static final String FUNDING = NS + "teslaris-funding-9";

    private static final VivoTeslarisLinks LINKS =
        new VivoTeslarisLinks("http://localhost:5173/", "sr,en", "CRIS UNS");

    private final VivoProjectMapper projectMapper =
        new VivoProjectMapper(LINKS, messageSource());

    private final VivoFundingMapper fundingMapper = new VivoFundingMapper(LINKS);


    private static StaticMessageSource messageSource() {
        var messageSource = new StaticMessageSource();
        var serbian = Locale.forLanguageTag("sr");
        messageSource.addMessage("personProjectContributionType.TEAM_MEMBER", Locale.ENGLISH,
            "Team member");
        messageSource.addMessage("personProjectContributionType.TEAM_MEMBER", serbian,
            "Član tima");
        messageSource.addMessage("personProjectInvestigationRole.PHD_STUDENT_FELLOW",
            Locale.ENGLISH, "PhD student fellow");
        messageSource.addMessage("personProjectInvestigationRole.PHD_STUDENT_FELLOW", serbian,
            "Doktorand");
        return messageSource;
    }

    // Strict: outputs without funding parts only when the project has one funding.
    private static PushTarget target() {
        return target(false);
    }

    private static PushTarget creditAllTarget() {
        return target(true);
    }

    private static PushTarget target(Boolean creditAllFundingsWithoutFundingParts) {
        return new PushTarget("vivo-local", "vivo", true, "http://vivo.example/api/sparqlUpdate",
            "http://graph.example", NS, new Auth(AuthType.NONE, null, null), 100, null,
            List.of("EN", "SR"), List.of("projects", "fundings"),
            creditAllFundingsWithoutFundingParts);
    }

    private static List<ExportMultilingualContent> english(String content) {
        return new ArrayList<>(List.of(new ExportMultilingualContent("EN", content, 1)));
    }

    private static ExportProjectPersonContribution person(Integer personId, String displayName,
                                                          int orderNumber,
                                                          PersonProjectContributionType type,
                                                          PersonProjectInvestigationRole role) {
        var contribution = new ExportProjectPersonContribution();
        contribution.setDisplayName(displayName);
        contribution.setOrderNumber(orderNumber);
        contribution.setContributionType(type);
        contribution.setInvestigationRole(role);
        if (personId != null) {
            var person = new ExportPerson();
            person.setDatabaseId(personId);
            contribution.setPerson(person);
        }
        return contribution;
    }

    private static ExportProjectOrganisationContribution organisation(
        Integer organisationUnitId, String displayName, int orderNumber,
        OrganisationUnitProjectContributionType type) {
        var contribution = new ExportProjectOrganisationContribution();
        contribution.setOrderNumber(orderNumber);
        contribution.setContributionType(type);
        if (organisationUnitId != null) {
            var organisationUnit = new ExportOrganisationUnit();
            organisationUnit.setDatabaseId(organisationUnitId);
            contribution.setOrganisationUnit(organisationUnit);
        } else {
            contribution.setDisplayName(english(displayName));
        }
        return contribution;
    }

    private static ExportProject project() {
        var project = new ExportProject();
        project.setDatabaseId(4);
        project.setName(english("Smart Grids"));
        project.setDescription(english("About grids."));
        project.setDateFrom(LocalDate.of(2023, 1, 1));
        project.setDateTo(LocalDate.of(2025, 12, 31));
        project.setRaid("10.80368/b1adfb3a");
        project.setPersons(new ArrayList<>(List.of(
            person(7, "Ivan Mrsulja", 1, PersonProjectContributionType.PRINCIPLE_INVESTIGATOR,
                null),
            person(8, "Ana Petrovic", 2, PersonProjectContributionType.TEAM_MEMBER,
                PersonProjectInvestigationRole.PHD_STUDENT_FELLOW),
            person(null, "Marko Markovic", 3,
                PersonProjectContributionType.PRINCIPLE_INVESTIGATOR, null),
            person(null, "Outside Member", 4, PersonProjectContributionType.TEAM_MEMBER,
                null))));
        project.setOrganisations(new ArrayList<>(List.of(
            organisation(2, null, 1, OrganisationUnitProjectContributionType.COORDINATOR),
            organisation(null, "Partner University", 2,
                OrganisationUnitProjectContributionType.PARTNER))));
        return project;
    }

    private static ExportFunding funding() {
        var funding = new ExportFunding();
        funding.setDatabaseId(9);
        funding.setName(english("Grant for Smart Grids"));
        funding.setGrantAgreementId("451-03-47/2023");
        funding.setAmount(new ExportMonetaryAmount(125000, "EUR"));
        funding.setFundingTypes(Set.of(FundingType.GRANT));
        var funder = new ExportOrganisationUnit();
        funder.setDatabaseId(30);
        funding.setFunder(funder);
        funding.setProject(project());
        return funding;
    }

    private static boolean has(Model model, String subject, String property, String object) {
        return model.contains(model.createResource(subject), model.createProperty(property),
            model.createResource(object));
    }

    private static ExportFunding fundingWithOutputs(Integer fundingCount,
                                                    ExportProjectDocument... outputs) {
        var funding = funding();
        funding.getProject().setFundingCount(fundingCount);
        funding.getProject().setDocuments(new ArrayList<>(List.of(outputs)));
        return funding;
    }

    private static boolean supports(Model model, Integer documentId) {
        var document = NS + "teslaris-document-" + documentId;
        return has(model, FUNDING, VIVO + "supportedInformationResource", document) &&
            has(model, document, VIVO + "informationResourceSupportedBy", FUNDING);
    }

    @Test
    public void shouldMapProjectWithResearcherRoles() {
        // When
        var mapping = projectMapper.map(project(), target());

        // Then
        var model = mapping.model();
        assertTrue(has(model, PROJECT, RDF.type.getURI(), VIVO + "Project"));
        assertTrue(model.contains(model.createResource(PROJECT), RDFS.label,
            model.createLiteral("Smart Grids", "en")));
        assertTrue(model.contains(model.createResource(PROJECT),
            model.createProperty(VIVO + "description"),
            model.createLiteral("About grids.", "en")));
        assertTrue(has(model, PROJECT, VIVO + "dateTimeInterval", PROJECT + "-interval"));

        var role = PROJECT + "-role-2";
        var person = NS + "teslaris-person-8";
        assertTrue(has(model, role, RDF.type.getURI(), VIVO + "ResearcherRole"));
        assertTrue(has(model, role, OBO + "RO_0000052", person));
        assertTrue(has(model, person, OBO + "RO_0000053", role));
        assertTrue(has(model, role, OBO + "BFO_0000054", PROJECT));
        assertTrue(has(model, PROJECT, OBO + "BFO_0000055", role));
        assertTrue(model.contains(model.createResource(role), RDFS.label,
            model.createLiteral("Team member (PhD student fellow)", "en")));
        assertTrue(model.contains(model.createResource(role), RDFS.label,
            model.createLiteral("Član tima (Doktorand)", "sr")));

        // No translation for PRINCIPLE_INVESTIGATOR in this message source: one readable name
        // without a language tag.
        assertTrue(model.contains(model.createResource(PROJECT + "-role-1"), RDFS.label,
            model.createLiteral("Principle investigator")));
        assertEquals(1, model.listObjectsOfProperty(model.createResource(PROJECT + "-role-1"),
            RDFS.label).toList().size());

        // Contributions without a TeslaRIS person are not sent on the project.
        assertFalse(model.containsResource(model.createResource(PROJECT + "-role-3")));
        assertFalse(model.containsResource(model.createResource(PROJECT + "-role-4")));
        assertTrue(mapping.references().contains(new EntityReference(PushEntityType.PERSONS, 8)));
    }

    @Test
    public void shouldLinkProjectOrganisationsAsParticipants() {
        // When
        var mapping = projectMapper.map(project(), target());

        // Then
        var model = mapping.model();
        var organisationUnit = NS + "teslaris-ou-2";
        assertTrue(has(model, PROJECT, OBO + "RO_0000057", organisationUnit));
        assertTrue(has(model, organisationUnit, OBO + "RO_0000056", PROJECT));

        assertEquals(1, mapping.sharedNodes().size());
        var external = mapping.sharedNodes().getFirst().uri();
        assertTrue(external.startsWith(NS + "teslaris-external-org-"));
        assertTrue(has(model, PROJECT, OBO + "RO_0000057", external));
        assertFalse(model.contains(model.createResource(external), RDFS.label));

        assertTrue(model.contains(model.createResource(PROJECT + "-url-raid"),
            model.createProperty(VCARD + "url"),
            model.createTypedLiteral("https://raid.org/10.80368/b1adfb3a",
                XSDDatatype.XSDanyURI)));
        assertTrue(model.contains(model.createResource(PROJECT + "-url-teslaris"),
            model.createProperty(VCARD + "url"),
            model.createTypedLiteral("http://localhost:5173/sr/project/4",
                XSDDatatype.XSDanyURI)));
    }

    @Test
    public void shouldMapFundingAsGrantWithFunderAndProject() {
        // When
        var mapping = fundingMapper.map(funding(), target());

        // Then
        var model = mapping.model();
        var grant = model.createResource(FUNDING);
        var funder = NS + "teslaris-ou-30";
        assertTrue(has(model, FUNDING, RDF.type.getURI(), VIVO + "Grant"));
        assertTrue(model.contains(grant, model.createProperty(VIVO + "sponsorAwardId"),
            "451-03-47/2023"));
        assertTrue(model.contains(grant, model.createProperty(VIVO + "totalAwardAmount"),
            "125000.00 EUR"));
        assertTrue(has(model, FUNDING, VIVO + "assignedBy", funder));
        assertTrue(has(model, funder, VIVO + "assigns", FUNDING));
        assertTrue(has(model, FUNDING, VIVO + "fundingVehicleFor", PROJECT));
        assertTrue(has(model, PROJECT, VIVO + "hasFundingVehicle", FUNDING));
        assertTrue(mapping.references().containsAll(List.of(
            new EntityReference(PushEntityType.PROJECTS, 4),
            new EntityReference(PushEntityType.ORGANISATION_UNITS, 30))));

        // The grant does not write the project's or funder's own data.
        assertFalse(model.contains(model.createResource(PROJECT), RDF.type));
        assertFalse(model.contains(model.createResource(funder), RDFS.label));
    }

    @Test
    public void shouldAddPrincipalInvestigatorsAndAdministratorToGrant() {
        // When
        var model = fundingMapper.toRdf(funding(), target());

        // Then
        var firstPi = FUNDING + "-pi-1";
        var person = NS + "teslaris-person-7";
        assertTrue(has(model, firstPi, RDF.type.getURI(), VIVO + "PrincipalInvestigatorRole"));
        assertTrue(has(model, FUNDING, VIVO + "relates", firstPi));
        assertTrue(has(model, firstPi, VIVO + "relatedBy", FUNDING));
        assertTrue(has(model, firstPi, OBO + "RO_0000052", person));
        assertTrue(has(model, person, OBO + "RO_0000053", firstPi));
        assertTrue(has(model, FUNDING, VIVO + "relates", person));

        // External PI: vcard on the grant.
        var secondPi = FUNDING + "-pi-2";
        var vcard = secondPi + "-vcard";
        assertTrue(has(model, secondPi, OBO + "RO_0000052", vcard));
        assertTrue(has(model, vcard, RDF.type.getURI(), VCARD + "Individual"));
        assertTrue(model.contains(model.createResource(vcard + "-name"),
            model.createProperty(VCARD + "familyName"), "Markovic"));

        // Team members are not on the grant.
        assertFalse(has(model, FUNDING, VIVO + "relates", NS + "teslaris-person-8"));
        assertFalse(model.containsResource(model.createResource(FUNDING + "-pi-3")));

        var admin = FUNDING + "-admin-1";
        var coordinator = NS + "teslaris-ou-2";
        assertTrue(has(model, admin, RDF.type.getURI(), VIVO + "AdministratorRole"));
        assertTrue(has(model, admin, OBO + "RO_0000052", coordinator));
        assertTrue(has(model, coordinator, OBO + "RO_0000053", admin));
        assertFalse(model.containsResource(model.createResource(FUNDING + "-admin-2")));
        NTriplesSerializer.toLines(model);
    }

    @Test
    public void shouldMapContractAndExternalFunder() {
        // Given
        var funding = funding();
        funding.setFundingTypes(Set.of(FundingType.CONTRACT));
        funding.setFunder(null);
        funding.setDisplayFunder(english("Some Foundation"));

        // When
        var mapping = fundingMapper.map(funding, target());

        // Then
        var model = mapping.model();
        assertTrue(has(model, FUNDING, RDF.type.getURI(), VIVO + "Contract"));
        var funder = mapping.sharedNodes().stream()
            .filter(node -> node.model().contains(node.model().createResource(node.uri()),
                RDFS.label, node.model().createLiteral("Some Foundation", "en")))
            .findFirst().orElseThrow().uri();
        assertTrue(funder.startsWith(NS + "teslaris-external-org-"));
        assertTrue(has(model, FUNDING, VIVO + "assignedBy", funder));
    }

    @Test
    public void shouldLinkResultAndReportOutputsInBothDirections() {
        // Given
        var project = project();
        project.setDocuments(new ArrayList<>(List.of(
            new ExportProjectDocument(20, "RESULT", List.of()),
            new ExportProjectDocument(21, "REPORT", List.of()),
            new ExportProjectDocument(22, "USED", List.of()),
            new ExportProjectDocument(23, "INTERNAL", List.of()))));

        // When
        var mapping = projectMapper.map(project, target());

        // Then
        var model = mapping.model();
        for (var id : List.of(20, 21)) {
            var document = NS + "teslaris-document-" + id;
            assertTrue(has(model, PROJECT, OBO + "RO_0002234", document));
            assertTrue(has(model, document, OBO + "RO_0002353", PROJECT));
            assertTrue(mapping.references().contains(
                new EntityReference(PushEntityType.DOCUMENTS, id)));
        }
        assertFalse(model.containsResource(model.createResource(NS + "teslaris-document-22")));
        assertFalse(model.containsResource(model.createResource(NS + "teslaris-document-23")));

        // The project does not write the document's own data.
        assertFalse(model.contains(model.createResource(NS + "teslaris-document-20"), RDF.type));
    }

    @Test
    public void shouldCreditOutputsByFundingParts() {
        // Given: the project has two fundings (9 and 10).
        var funding = fundingWithOutputs(2,
            new ExportProjectDocument(20, "RESULT", List.of(9)),
            new ExportProjectDocument(21, "RESULT", List.of(10)),
            new ExportProjectDocument(22, "REPORT", List.of(9, 10)),
            new ExportProjectDocument(23, "RESULT", List.of()),
            new ExportProjectDocument(24, "USED", List.of(9)));

        // When
        var mapping = fundingMapper.map(funding, target());

        // Then
        var model = mapping.model();
        assertTrue(supports(model, 20));
        assertFalse(supports(model, 21));
        assertTrue(supports(model, 22));
        // No funding parts and two fundings: it is not known which one paid.
        assertFalse(supports(model, 23));
        // USED is not an output.
        assertFalse(model.containsResource(model.createResource(NS + "teslaris-document-24")));
        assertTrue(mapping.references().contains(
            new EntityReference(PushEntityType.DOCUMENTS, 20)));
    }

    @Test
    public void shouldCreditOutputWithoutFundingPartsWhenProjectHasOneFunding() {
        // Given
        var funding = fundingWithOutputs(1, new ExportProjectDocument(30, "RESULT", List.of()));

        // When
        var model = fundingMapper.toRdf(funding, target());

        // Then
        assertTrue(supports(model, 30));
        // The grant does not write the document's own data.
        assertFalse(model.contains(model.createResource(NS + "teslaris-document-30"), RDF.type));
    }

    @Test
    public void shouldNotCreditOutputWithoutFundingPartsInOldSnapshot() {
        // Given: snapshots made before the funding count was added.
        var funding =
            fundingWithOutputs(null, new ExportProjectDocument(40, "RESULT", List.of()));

        // When
        var model = fundingMapper.toRdf(funding, target());

        // Then
        assertFalse(supports(model, 40));
    }

    @Test
    public void shouldCreditEveryFundingWhenEnabledAndOutputHasNoFundingParts() {
        // Given: the project has two fundings and the output has no funding parts.
        var funding = fundingWithOutputs(2,
            new ExportProjectDocument(50, "RESULT", List.of()),
            new ExportProjectDocument(51, "RESULT", List.of(10)));

        // When
        var model = fundingMapper.toRdf(funding, creditAllTarget());

        // Then
        assertTrue(supports(model, 50));
        // Funding parts still decide when they exist.
        assertFalse(supports(model, 51));
    }

    @Test
    public void shouldCreditOldSnapshotWithoutCountWhenEnabled() {
        // Given
        var funding =
            fundingWithOutputs(null, new ExportProjectDocument(52, "REPORT", List.of()));

        // When
        var model = fundingMapper.toRdf(funding, creditAllTarget());

        // Then
        assertTrue(supports(model, 52));
    }

    @Test
    public void shouldCreditEveryFundingWhenOptionIsMissing() {
        // Given: a target without "creditAllFundingsWithoutFundingParts" in its configuration.
        var funding = fundingWithOutputs(2, new ExportProjectDocument(53, "RESULT", List.of()));

        // When
        var model = fundingMapper.toRdf(funding, target(null));

        // Then
        assertTrue(supports(model, 53));
    }

    @Test
    public void shouldLinkProjectSubjectAreas() {
        // Given
        var project = project();
        project.setResearchAreaIds(new ArrayList<>(List.of(9, 4)));
        // When
        var mapping = projectMapper.map(project, target());

        // Then
        var model = mapping.model();
        var subject = model.createResource(NS + "teslaris-project-" + project.getDatabaseId());
        var area = model.createResource(NS + "teslaris-research-area-4");
        assertTrue(model.contains(subject,
            model.createProperty("http://vivoweb.org/ontology/core#hasSubjectArea"), area));
        assertTrue(model.contains(area,
            model.createProperty("http://vivoweb.org/ontology/core#subjectAreaOf"), subject));
        assertTrue(model.contains(subject,
            model.createProperty("http://vivoweb.org/ontology/core#hasSubjectArea"),
            model.createResource(NS + "teslaris-research-area-9")));
        assertTrue(mapping.references().containsAll(List.of(
            new EntityReference(PushEntityType.RESEARCH_AREAS, 4),
            new EntityReference(PushEntityType.RESEARCH_AREAS, 9))));
    }

    @Test
    public void shouldLinkFundingSubjectAreas() {
        // Given
        var funding = funding();
        funding.setResearchAreaIds(new ArrayList<>(List.of(9, 4)));
        // When
        var mapping = fundingMapper.map(funding, target());

        // Then
        var model = mapping.model();
        var subject = model.createResource(NS + "teslaris-funding-" + funding.getDatabaseId());
        var area = model.createResource(NS + "teslaris-research-area-4");
        assertTrue(model.contains(subject,
            model.createProperty("http://vivoweb.org/ontology/core#hasSubjectArea"), area));
        assertTrue(model.contains(area,
            model.createProperty("http://vivoweb.org/ontology/core#subjectAreaOf"), subject));
        assertTrue(model.contains(subject,
            model.createProperty("http://vivoweb.org/ontology/core#hasSubjectArea"),
            model.createResource(NS + "teslaris-research-area-9")));
        assertTrue(mapping.references().containsAll(List.of(
            new EntityReference(PushEntityType.RESEARCH_AREAS, 4),
            new EntityReference(PushEntityType.RESEARCH_AREAS, 9))));
    }

    @Test
    public void shouldFallBackToUnsupportedLanguageWithoutLanguageTag() {
        // Given
        var funding = funding();
        funding.setName(new ArrayList<>(List.of(
            new ExportMultilingualContent("PT", "F. Estratégico UID - 2013/2015", 1))));
        funding.setDescription(new ArrayList<>(List.of(
            new ExportMultilingualContent("HU", "Leírás", 2),
            new ExportMultilingualContent("PT", "Descrição", 1))));
        funding.setKeywords(new ArrayList<>(List.of(
            new ExportMultilingualContent("PT", "energia\nredes", 1))));

        // When
        var model = fundingMapper.toRdf(funding, target());

        // Then
        var grant = model.createResource(FUNDING);
        var description = model.createProperty(VIVO + "description");
        var keyword = model.createProperty(VIVO + "freetextKeyword");
        assertTrue(model.contains(grant, RDFS.label,
            model.createLiteral("F. Estratégico UID - 2013/2015")));
        assertEquals(1, model.listObjectsOfProperty(grant, RDFS.label).toList().size());

        // Only the values of the language with the lowest priority number.
        assertTrue(model.contains(grant, description, model.createLiteral("Descrição")));
        assertEquals(1, model.listObjectsOfProperty(grant, description).toList().size());

        // Every keyword of that language is kept.
        assertTrue(model.contains(grant, keyword, model.createLiteral("energia")));
        assertTrue(model.contains(grant, keyword, model.createLiteral("redes")));
        assertEquals(2, model.listObjectsOfProperty(grant, keyword).toList().size());
    }

    @Test
    public void shouldNotFallBackWhenASupportedLanguageExists() {
        // Given
        var funding = funding();
        funding.setName(new ArrayList<>(List.of(
            new ExportMultilingualContent("PT", "Nome", 1),
            new ExportMultilingualContent("EN", "Name", 2))));

        // When
        var model = fundingMapper.toRdf(funding, target());

        // Then
        var grant = model.createResource(FUNDING);
        assertTrue(model.contains(grant, RDFS.label, model.createLiteral("Name", "en")));
        assertFalse(model.contains(grant, RDFS.label, model.createLiteral("Nome")));
        assertEquals(1, model.listObjectsOfProperty(grant, RDFS.label).toList().size());
    }
}
