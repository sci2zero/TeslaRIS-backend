package rs.teslaris.core.util.signposting;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Objects;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import rs.teslaris.core.dto.document.DatasetDTO;
import rs.teslaris.core.dto.document.DocumentDTO;
import rs.teslaris.core.dto.document.DocumentFileResponseDTO;
import rs.teslaris.core.dto.document.JournalPublicationResponseDTO;
import rs.teslaris.core.dto.document.JournalResponseDTO;
import rs.teslaris.core.dto.document.MonographDTO;
import rs.teslaris.core.dto.document.MonographPublicationDTO;
import rs.teslaris.core.dto.document.PatentDTO;
import rs.teslaris.core.dto.document.ProceedingsPublicationDTO;
import rs.teslaris.core.dto.document.ProceedingsResponseDTO;
import rs.teslaris.core.dto.document.PublicationSeriesDTO;
import rs.teslaris.core.dto.document.SoftwareDTO;
import rs.teslaris.core.dto.document.ThesisResponseDTO;
import rs.teslaris.core.dto.institution.OrganisationUnitDTO;
import rs.teslaris.core.dto.person.PersonResponseDTO;
import rs.teslaris.core.model.document.AccessRights;
import rs.teslaris.core.model.document.BibliographicFormat;
import rs.teslaris.core.model.document.DocumentContributionType;
import rs.teslaris.core.model.document.DocumentFile;
import rs.teslaris.core.model.document.LibraryFormat;
import rs.teslaris.core.model.document.ResourceType;
import rs.teslaris.core.model.institution.OrganisationUnitsRelation;
import rs.teslaris.core.service.impl.person.PersonServiceImpl;
import rs.teslaris.core.util.functional.Pair;

@Component
public class FairSignpostingL2Utility {

    private static String baseUrl;

    private static String frontendUrl;

    private static String defaultLocale;

    private static PersonServiceImpl personService;

    private FairSignpostingL2Utility() {
        // utility class
    }


    public static String createLinksetForOrganisationUnit(OrganisationUnitDTO organisationUnit,
                                                          OrganisationUnitsRelation superRelation,
                                                          LinksetFormat format) {
        var anchor =
            frontendUrl + defaultLocale + "/organisation-units/" + organisationUnit.getId();
        var linkEntries = new ArrayList<FairSignpostingLinksetUtility.LinkEntry>();

        linkEntries.add(
            new FairSignpostingLinksetUtility.LinkEntry("https://schema.org/AboutPage", "type",
                null, anchor)
        );
        linkEntries.add(
            new FairSignpostingLinksetUtility.LinkEntry("https://schema.org/Organization", "type",
                null, anchor)
        );

        if (Objects.nonNull(superRelation)) {
            linkEntries.add(
                new FairSignpostingLinksetUtility.LinkEntry(
                    frontendUrl + defaultLocale + "/organisation-unit/" +
                        superRelation.getTargetOrganisationUnit().getId(),
                    "collection", "https://schema.org/parentOrganization", anchor)
            );
        }

        return serializeToLinksetFormat(linkEntries, format);
    }

    public static String createLinksForPublicationSeries(PublicationSeriesDTO publicationSeries,
                                                         LinksetFormat format) {
        var anchor = frontendUrl + defaultLocale +
            ((publicationSeries instanceof JournalResponseDTO) ? "/journals/" : "/book-series/") +
            publicationSeries.getId();
        var linkEntries = new ArrayList<FairSignpostingLinksetUtility.LinkEntry>();

        linkEntries.add(
            new FairSignpostingLinksetUtility.LinkEntry("https://schema.org/AboutPage", "type",
                null, anchor)
        );
        linkEntries.add(
            new FairSignpostingLinksetUtility.LinkEntry("https://schema.org/Periodical", "type",
                null, anchor)
        );

        return serializeToLinksetFormat(linkEntries, format);
    }

    public static String addLinksForDocumentFileItems(DocumentFile file, LinksetFormat format) {
        var anchor =
            FairSignpostingL1Utility.buildSafeUri(baseUrl + "/api/file/" + file.getFilename());
        var linkEntries = new ArrayList<FairSignpostingLinksetUtility.LinkEntry>();

        linkEntries.add(
            new FairSignpostingLinksetUtility.LinkEntry("https://schema.org/item", "type",
                null, anchor)
        );

        return serializeToLinksetFormat(linkEntries, format);
    }

    public static String createLinksetForPerson(PersonResponseDTO person, LinksetFormat format) {
        if (Objects.isNull(person)) {
            return "";
        }

        var anchor = frontendUrl + defaultLocale + "/persons/" + person.getId();
        var linkEntries = new ArrayList<FairSignpostingLinksetUtility.LinkEntry>();

        linkEntries.add(
            new FairSignpostingLinksetUtility.LinkEntry("https://schema.org/AboutPage", "type",
                null, anchor)
        );
        linkEntries.add(
            new FairSignpostingLinksetUtility.LinkEntry("https://schema.org/Person", "type", null,
                anchor)
        );

        linkEntries.add(
            new FairSignpostingLinksetUtility.LinkEntry(
                frontendUrl + defaultLocale + "/persons/" + person.getId(),
                "collection", null, anchor)
        );

        if (FairSignpostingL1Utility.valuePresent(person.getPersonalInfo().getOrcid())) {
            var dataciteUri = FairSignpostingL1Utility.buildSafeUri(
                "https://orcid.org/" + person.getPersonalInfo().getOrcid());
            linkEntries.add(
                new FairSignpostingLinksetUtility.LinkEntry(dataciteUri, "cite-as",
                    "application/vnd.datacite.datacite+json", anchor)
            );
            linkEntries.add(
                new FairSignpostingLinksetUtility.LinkEntry(anchor, "describes", "text/html",
                    dataciteUri)
            );
        }

        if (FairSignpostingL1Utility.valuePresent(
            person.getPersonalInfo().getScopusAuthorId())) {
            var dataciteUri = "https://www.scopus.com/authid/detail.uri?authorId=" +
                person.getPersonalInfo().getScopusAuthorId();
            linkEntries.add(
                new FairSignpostingLinksetUtility.LinkEntry(dataciteUri, "describedby",
                    "application/vnd.datacite.datacite+json", anchor)
            );
            linkEntries.add(
                new FairSignpostingLinksetUtility.LinkEntry(anchor, "describes", "text/html",
                    dataciteUri)
            );
        }
        if (FairSignpostingL1Utility.valuePresent(
            person.getPersonalInfo().getOpenAlexId())) {
            var dataciteUri = "https://openalex.org/" + person.getPersonalInfo().getOpenAlexId();
            linkEntries.add(
                new FairSignpostingLinksetUtility.LinkEntry(dataciteUri, "describedby",
                    "application/vnd.datacite.datacite+json", anchor)
            );
            linkEntries.add(
                new FairSignpostingLinksetUtility.LinkEntry(anchor, "describes", "text/html",
                    dataciteUri)
            );
        }
        if (FairSignpostingL1Utility.valuePresent(
            person.getPersonalInfo().getWebOfScienceResearcherId())) {
            var dataciteUri = "http://www.researcherid.com/rid/" +
                person.getPersonalInfo().getWebOfScienceResearcherId();
            linkEntries.add(
                new FairSignpostingLinksetUtility.LinkEntry(dataciteUri, "describedby",
                    "application/vnd.datacite.datacite+json", anchor)
            );
            linkEntries.add(
                new FairSignpostingLinksetUtility.LinkEntry(anchor, "describes", "text/html",
                    dataciteUri)
            );
        }

        if (FairSignpostingL1Utility.valuePresent(person.getPersonalInfo().getECrisId())) {
            var dataciteUri = "https://bib.cobiss.net/biblioweb/biblio/sr/scr/cris/" +
                person.getPersonalInfo().getECrisId();
            linkEntries.add(
                new FairSignpostingLinksetUtility.LinkEntry(dataciteUri, "cite-as", "text/html",
                    anchor)
            );
            linkEntries.add(
                new FairSignpostingLinksetUtility.LinkEntry(anchor, "describes", "text/html",
                    dataciteUri)
            );
        }

        return serializeToLinksetFormat(linkEntries, format);
    }

    public static String createLinksetForDocument(DocumentDTO dto, LinksetFormat format) {
        var resourceType = deduceResourceType(dto);
        var anchor =
            frontendUrl + defaultLocale + "/scientific-results" + resourceType.b + dto.getId();
        var linkEntries = new ArrayList<FairSignpostingLinksetUtility.LinkEntry>();

        addBaseLinks(linkEntries, resourceType, anchor);
        addIdentifierLinks(linkEntries, dto, anchor);
        addMetadataLinks(linkEntries, dto, resourceType.b, anchor);
        addDocumentFileLinks(linkEntries, dto, anchor);
        addLicenseLink(linkEntries, dto, anchor);
        addAuthorLinks(linkEntries, dto, anchor);

        return serializeToLinksetFormat(linkEntries, format);
    }

    private static void addBaseLinks(List<FairSignpostingLinksetUtility.LinkEntry> linkEntries,
                                     Pair<String, String> resourceType, String anchor) {
        linkEntries.add(new FairSignpostingLinksetUtility.LinkEntry(
            "https://schema.org/AboutPage", "type", null, anchor));
        linkEntries.add(new FairSignpostingLinksetUtility.LinkEntry(
            resourceType.a, "type", null, anchor));
    }

    private static void addIdentifierLinks(
        List<FairSignpostingLinksetUtility.LinkEntry> linkEntries,
        DocumentDTO dto, String anchor) {
        if (FairSignpostingL1Utility.valuePresent(dto.getDoi())) {
            var doiUri = "https://doi.org/" + dto.getDoi();
            linkEntries.add(new FairSignpostingLinksetUtility.LinkEntry(
                doiUri, "cite-as", "application/vnd.datacite.datacite+json", anchor));
            linkEntries.add(new FairSignpostingLinksetUtility.LinkEntry(
                doiUri, "describedby", "application/vnd.datacite.datacite+json", anchor));
            linkEntries.add(new FairSignpostingLinksetUtility.LinkEntry(
                anchor, "describes", "text/html", doiUri));
        }

        if (FairSignpostingL1Utility.valuePresent(dto.getScopusId())) {
            var scopusUri =
                "https://www.scopus.com/record/display.uri?eid=2-s2.0-" + dto.getScopusId();
            linkEntries.add(new FairSignpostingLinksetUtility.LinkEntry(
                scopusUri, "describedby", "application/vnd.datacite.datacite+json", anchor));
            linkEntries.add(new FairSignpostingLinksetUtility.LinkEntry(
                anchor, "describes", "text/html", scopusUri));
        }

        if (FairSignpostingL1Utility.valuePresent(dto.getWebOfScienceId())) {
            var wosUri = "https://www.webofscience.com/api/gateway?GWVersion=2&SrcApp=teslaris" +
                "&SrcAuth=WosAPI&DestLinkType=FullRecord&DestApp=WOS_CPL&KeyUT=WOS:" +
                dto.getWebOfScienceId();
            linkEntries.add(new FairSignpostingLinksetUtility.LinkEntry(
                wosUri, "describedby", "application/vnd.datacite.datacite+json", anchor));
            linkEntries.add(new FairSignpostingLinksetUtility.LinkEntry(
                anchor, "describes", "text/html", wosUri));
        }

        if (FairSignpostingL1Utility.valuePresent(dto.getOpenAlexId())) {
            var openAlexUri = "https://openalex.org/" + dto.getDoi();
            linkEntries.add(new FairSignpostingLinksetUtility.LinkEntry(
                openAlexUri, "describedby", "application/vnd.datacite.datacite+json", anchor));
            linkEntries.add(new FairSignpostingLinksetUtility.LinkEntry(
                anchor, "describes", "text/html", openAlexUri));
        }
    }

    private static void addLicenseLink(List<FairSignpostingLinksetUtility.LinkEntry> linkEntries,
                                       DocumentDTO dto, String anchor) {
        dto.getFileItems().stream()
            .filter(f -> f.getResourceType().equals(ResourceType.OFFICIAL_PUBLICATION))
            .findFirst()
            .ifPresent(file -> {
                if (Objects.nonNull(file.getAccessRights()) &&
                    Objects.nonNull(file.getLicense()) &&
                    file.getAccessRights().equals(AccessRights.OPEN_ACCESS)) {
                    var licenseUri = FairSignpostingL1Utility.buildSafeUri(
                        "https://spdx.org/licenses/CC-" +
                            file.getLicense().name().replace("_", "-") + "-4.0");
                    linkEntries.add(new FairSignpostingLinksetUtility.LinkEntry(
                        licenseUri, "license", null, anchor));
                }
            });
    }

    private static void addAuthorLinks(List<FairSignpostingLinksetUtility.LinkEntry> linkEntries,
                                       DocumentDTO dto, String anchor) {
        if (Objects.isNull(dto.getContributions())) {
            return;
        }

        dto.getContributions().stream()
            .filter(c -> c.getContributionType().equals(DocumentContributionType.AUTHOR))
            .forEach(author -> {
                if (Objects.nonNull(author.getPersonId()) && author.getPersonId() > 0) {
                    var person = personService.readPersonWithBasicInfo(author.getPersonId());
                    var uri = resolveAuthorIdentifier(person);
                    if (Objects.nonNull(uri)) {
                        linkEntries.add(
                            new FairSignpostingLinksetUtility.LinkEntry(uri, "author", null,
                                anchor));
                    }
                }
            });
    }

    private static String resolveAuthorIdentifier(PersonResponseDTO person) {
        if (Objects.isNull(person)) {
            return null;
        }

        var info = person.getPersonalInfo();

        if (FairSignpostingL1Utility.valuePresent(info.getOrcid())) {
            return "https://orcid.org/" + info.getOrcid();
        }
        if (FairSignpostingL1Utility.valuePresent(info.getScopusAuthorId())) {
            return "https://www.scopus.com/authid/detail.uri?authorId=" + info.getScopusAuthorId();
        }
        if (FairSignpostingL1Utility.valuePresent(info.getOpenAlexId())) {
            return "https://openalex.org/" + info.getOpenAlexId();
        }
        if (FairSignpostingL1Utility.valuePresent(info.getWebOfScienceResearcherId())) {
            return "http://www.researcherid.com/rid/" + info.getWebOfScienceResearcherId();
        }
        if (FairSignpostingL1Utility.valuePresent(info.getECrisId())) {
            return "https://bib.cobiss.net/biblioweb/biblio/sr/scr/cris/" + info.getECrisId();
        }
        return null;
    }

    private static void addMetadataLinks(
        ArrayList<FairSignpostingLinksetUtility.LinkEntry> linkEntries, DocumentDTO dto,
        String selfUrl, String anchor) {
        var dataciteUri = baseUrl + "/api/document/metadata/" + dto.getId() + "/BIBTEX";
        linkEntries.add(
            new FairSignpostingLinksetUtility.LinkEntry(
                dataciteUri, "describedby",
                BibliographicFormat.BIBTEX.getValue(), anchor)
        );
        linkEntries.add(
            new FairSignpostingLinksetUtility.LinkEntry(anchor, "describes", "text/html",
                dataciteUri)
        );

        dataciteUri = baseUrl + "/api/document/" + dto.getId() + "/REFMAN";
        linkEntries.add(
            new FairSignpostingLinksetUtility.LinkEntry(
                dataciteUri, "describedby",
                BibliographicFormat.REFMAN.getValue(), anchor)
        );
        linkEntries.add(
            new FairSignpostingLinksetUtility.LinkEntry(anchor, "describes", "text/html",
                dataciteUri)
        );

        dataciteUri = baseUrl + "/api/document/metadata/" + dto.getId() + "/ENDNOTE";
        linkEntries.add(
            new FairSignpostingLinksetUtility.LinkEntry(
                dataciteUri, "describedby",
                BibliographicFormat.ENDNOTE.getValue(), anchor)
        );
        linkEntries.add(
            new FairSignpostingLinksetUtility.LinkEntry(anchor, "describes", "text/html",
                dataciteUri)
        );

        if (dto instanceof ThesisResponseDTO) {
            for (var libraryFormat : LibraryFormat.values()) {
                var libraryDataciteUri =
                    baseUrl + "/api" + selfUrl + "/library-format/" + dto.getId() + "/" +
                        libraryFormat.name();
                linkEntries.add(
                    new FairSignpostingLinksetUtility.LinkEntry(
                        libraryDataciteUri, "describedby", libraryFormat.getValue(), anchor)
                );
                linkEntries.add(
                    new FairSignpostingLinksetUtility.LinkEntry(anchor, "describes", "text/html",
                        libraryDataciteUri)
                );
            }
        }
    }

    private static void addDocumentFileLinks(
        ArrayList<FairSignpostingLinksetUtility.LinkEntry> linkEntries, DocumentDTO dto,
        String anchor) {
        addLinksForDocumentFiles(linkEntries, dto.getFileItems(), anchor);
        addLinksForDocumentFiles(linkEntries, dto.getProofs(), anchor);

        if (dto instanceof ThesisResponseDTO) {
            addLinksForDocumentFiles(linkEntries,
                ((ThesisResponseDTO) dto).getPreliminaryFiles(), anchor);
            addLinksForDocumentFiles(linkEntries,
                ((ThesisResponseDTO) dto).getPreliminarySupplements(), anchor);
            addLinksForDocumentFiles(linkEntries,
                ((ThesisResponseDTO) dto).getCommissionReports(), anchor);
        }
    }

    private static void addLinksForDocumentFiles(
        ArrayList<FairSignpostingLinksetUtility.LinkEntry> linkEntries,
        Collection<DocumentFileResponseDTO> items, String anchor) {
        if (Objects.isNull(items)) {
            return;
        }

        items.stream()
            .filter(df -> df.getAccessRights().equals(AccessRights.OPEN_ACCESS))
            .forEach(file -> {
                var fileUri = FairSignpostingL1Utility.buildSafeUri(
                    baseUrl + "/api/file/" + file.getFileName());
                linkEntries.add(
                    new FairSignpostingLinksetUtility.LinkEntry(fileUri, "item", file.getMimeType(),
                        anchor)
                );
                linkEntries.add(
                    new FairSignpostingLinksetUtility.LinkEntry(anchor, "collection", "text/html",
                        fileUri)
                );
            });
    }

    private static String serializeToLinksetFormat(
        ArrayList<FairSignpostingLinksetUtility.LinkEntry> links, LinksetFormat linksetFormat) {
        return switch (linksetFormat) {
            case JSON -> FairSignpostingLinksetUtility.toJson(links);
            case LINKSET -> FairSignpostingLinksetUtility.toLinksetFormat(links);
        };
    }

    private static Pair<String, String> deduceResourceType(DocumentDTO dto) {
        return switch (dto) {
            case JournalPublicationResponseDTO ignored ->
                new Pair<>("https://schema.org/ScholarlyArticle", "/journal-publication");
            case SoftwareDTO ignored ->
                new Pair<>("https://schema.org/SoftwareApplication", "/software");
            case DatasetDTO ignored -> new Pair<>("https://schema.org/Dataset", "/dataset");
            case PatentDTO ignored -> new Pair<>("https://schema.org/result", "/patent");
            case ProceedingsResponseDTO ignored ->
                new Pair<>("https://schema.org/Collection", "/proceedings");
            case ProceedingsPublicationDTO ignored ->
                new Pair<>("https://schema.org/ScholarlyArticle", "/proceedings-publication");
            case MonographDTO ignored -> new Pair<>("https://schema.org/Book", "/monograph");
            case MonographPublicationDTO ignored ->
                new Pair<>("https://schema.org/Chapter", "/monograph-publication");
            case ThesisResponseDTO ignored -> new Pair<>("https://schema.org/Thesis", "/thesis");
            default -> new Pair<>("https://schema.org/Item", "/");
        };
    }

    @Value("${export.base.url}")
    public void setBaseUrl(String baseUrl) {
        FairSignpostingL2Utility.baseUrl = baseUrl;
    }

    @Value("${frontend.application.address}")
    public void setFrontendUrl(String frontendUrl) {
        FairSignpostingL2Utility.frontendUrl = frontendUrl;
    }

    @Value("${default.locale}")
    public void setDefaultLocale(String defaultLocale) {
        FairSignpostingL2Utility.defaultLocale = defaultLocale;
    }

    @Autowired
    public void setPersonService(PersonServiceImpl personService) {
        FairSignpostingL2Utility.personService = personService;
    }
}
