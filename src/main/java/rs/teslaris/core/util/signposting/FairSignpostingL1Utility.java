package rs.teslaris.core.util.signposting;

import java.net.URI;
import java.net.URISyntaxException;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.Collection;
import java.util.Objects;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpHeaders;
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
import rs.teslaris.core.model.document.Dataset;
import rs.teslaris.core.model.document.Document;
import rs.teslaris.core.model.document.DocumentContributionType;
import rs.teslaris.core.model.document.DocumentFile;
import rs.teslaris.core.model.document.JournalPublication;
import rs.teslaris.core.model.document.LibraryFormat;
import rs.teslaris.core.model.document.Monograph;
import rs.teslaris.core.model.document.MonographPublication;
import rs.teslaris.core.model.document.Patent;
import rs.teslaris.core.model.document.Proceedings;
import rs.teslaris.core.model.document.ProceedingsPublication;
import rs.teslaris.core.model.document.ResourceType;
import rs.teslaris.core.model.document.Software;
import rs.teslaris.core.model.document.Thesis;
import rs.teslaris.core.model.institution.OrganisationUnitsRelation;

@Component
public class FairSignpostingL1Utility {

    private static String baseUrl;

    private static String frontendUrl;

    private static String defaultLocale;


    private FairSignpostingL1Utility() {
        // utility class
    }

    public static void addHeadersForPerson(HttpHeaders headers, PersonResponseDTO person,
                                           String selfUrl) {
        headers.add(HttpHeaders.LINK,
            "<https://schema.org/Person>; rel=\"type\"");
        headers.add(HttpHeaders.LINK,
            "<" + frontendUrl + defaultLocale + "/persons/" + person.getId() +
                "> ; rel=\"collection\" ; type=\"text/html\"");
        headers.add(HttpHeaders.LINK,
            "<" + baseUrl + selfUrl + "/" + person.getId() +
                ">; rel=\"describedby\"; type=\"application/json\"");

        if (valuePresent(person.getPersonalInfo().getOrcid())) {
            headers.add(HttpHeaders.LINK,
                "<" + buildSafeUri("https://orcid.org/" + person.getPersonalInfo().getOrcid()) +
                    ">; rel=\"identifier\"; type=\"text/html\"");
        }

        if (valuePresent(person.getPersonalInfo().getScopusAuthorId())) {
            headers.add(HttpHeaders.LINK, "<https://www.scopus.com/authid/detail.uri?authorId=" +
                person.getPersonalInfo().getScopusAuthorId() +
                ">; rel=\"identifier\"; type=\"text/html\"");
        }

        if (valuePresent(person.getPersonalInfo().getOpenAlexId())) {
            headers.add(HttpHeaders.LINK,
                "<https://openalex.org/" + person.getPersonalInfo().getOpenAlexId() +
                    ">; rel=\"identifier\"; type=\"text/html\"");
        }

        if (valuePresent(person.getPersonalInfo().getWebOfScienceResearcherId())) {
            headers.add(HttpHeaders.LINK, "http://www.researcherid.com/rid/" +
                person.getPersonalInfo().getWebOfScienceResearcherId() +
                ">; rel=\"identifier\"; type=\"text/html\"");
        }

        if (valuePresent(person.getPersonalInfo().getECrisId())) {
            headers.add(HttpHeaders.LINK,
                "<https://bib.cobiss.net/biblioweb/biblio/sr/scr/cris/" +
                    person.getPersonalInfo().getECrisId() + ">; rel=\"identifier\"");
        }


        addLinksetReferences(headers, selfUrl, String.valueOf(person.getId()));
    }


    public static void addHeadersForOrganisationUnit(HttpHeaders headers,
                                                     OrganisationUnitDTO organisationUnit,
                                                     OrganisationUnitsRelation superRelation,
                                                     String selfUrl) {
        headers.add(HttpHeaders.LINK,
            "<https://schema.org/Organization>; rel=\"type\"");
        headers.add(HttpHeaders.LINK,
            "<" + frontendUrl + defaultLocale + "/organisation-units/" + organisationUnit.getId() +
                "> ; rel=\"collection\" ; type=\"text/html\"");
        headers.add(HttpHeaders.LINK,
            "<" + baseUrl + selfUrl + "/" + organisationUnit.getId() +
                ">; rel=\"describedby\"; type=\"application/json\"");

        if (Objects.nonNull(superRelation)) {
            headers.add(HttpHeaders.LINK,
                "<" + frontendUrl + defaultLocale + "/organisation-unit/" +
                    superRelation.getTargetOrganisationUnit().getId() +
                    "> ; rel=\"collection\" ; type=\"https://schema.org/parentOrganization\"");
        }

        addLinksetReferences(headers, selfUrl, String.valueOf(organisationUnit.getId()));
    }

    public static void addHeadersForPublicationSeries(HttpHeaders headers,
                                                      PublicationSeriesDTO publicationSeries,
                                                      String selfUrl) {
        headers.add(HttpHeaders.LINK,
            "<https://schema.org/Periodical>; rel=\"type\"");
        headers.add(HttpHeaders.LINK,
            "<" + baseUrl + selfUrl + "/" + publicationSeries.getId() +
                ">; rel=\"describedby\"; type=\"application/json\"");

        if (publicationSeries instanceof JournalResponseDTO) {
            headers.add(HttpHeaders.LINK,
                "<" + frontendUrl + defaultLocale + "/journals/" + publicationSeries.getId() +
                    "> ; rel=\"collection\" ; type=\"text/html\"");
        } else {
            headers.add(HttpHeaders.LINK,
                "<" + frontendUrl + defaultLocale + "/book-series/" + publicationSeries.getId() +
                    "> ; rel=\"collection\" ; type=\"text/html\"");
        }

        if (valuePresent(publicationSeries.getEissn())) {
            headers.add(HttpHeaders.LINK,
                "<urn:issn:" + publicationSeries.getEissn() + ">; rel=\"cite-as\"");
        }

        if (valuePresent(publicationSeries.getPrintISSN())) {
            headers.add(HttpHeaders.LINK,
                "<urn:issn:" + publicationSeries.getPrintISSN() + ">; rel=\"cite-as\"");
        }

        addLinksetReferences(headers, selfUrl, String.valueOf(publicationSeries.getId()));
    }

    public static void addHeadersForDocumentFileItems(HttpHeaders headers,
                                                      DocumentFile documentFile) {
        headers.add(HttpHeaders.LINK, "<https://schema.org/item>; rel=\"type\"");
        if (Objects.nonNull(documentFile.getDocument())) {
            headers.add(HttpHeaders.LINK,
                "<" + frontendUrl + defaultLocale + "/scientific-results/" +
                    deduceDocumentType(documentFile.getDocument()) + "/" +
                    documentFile.getDocument().getId() +
                    "> ; rel=\"collection\" ; type=\"text/html\"");
        }

        addLinksetReferences(headers, "/api/file", documentFile.getServerFilename());
    }

    public static void addHeadersForMetadataFormats(HttpHeaders headers, Integer thesisId) {
        headers.add(HttpHeaders.LINK,
            "<" + frontendUrl + defaultLocale + "/scientific-results/thesis/" + thesisId +
                "> ; rel=\"collection\" ; type=\"text/html\"");
    }

    public static HttpHeaders constructHeaders(DocumentDTO dto, String selfUrl) {
        var headers = new HttpHeaders();

        deduceResourceType(headers, dto);
        setCommonFields(headers, dto, selfUrl);
        setCollectionRelatedFields(headers, dto);

        return headers;
    }

    private static void setCommonFields(HttpHeaders headers, DocumentDTO dto, String selfUrl) {
        if (valuePresent(dto.getDoi())) {
            headers.add(HttpHeaders.LINK,
                "<" + buildSafeUri("https://doi.org/" + dto.getDoi()) + ">; rel=\"cite-as\"");
        }

        if (valuePresent(dto.getScopusId())) {
            headers.add(HttpHeaders.LINK,
                "<https://www.scopus.com/record/display.uri?eid=2-s2.0-" + dto.getScopusId() +
                    ">; rel=\"cite-as\"");
        }

        if (valuePresent(dto.getWebOfScienceId())) {
            headers.add(HttpHeaders.LINK,
                "<https://www.webofscience.com/api/gateway?GWVersion=2&SrcApp=teslaris&SrcAuth=WosAPI&DestLinkType=FullRecord&DestApp=WOS_CPL&KeyUT=WOS:" +
                    dto.getDoi() + ">; rel=\"cite-as\"");
        }

        if (valuePresent(dto.getOpenAlexId())) {
            headers.add(HttpHeaders.LINK,
                "<" + buildSafeUri("https://openalex.org/" + dto.getDoi()) + ">; rel=\"cite-as\"");
        }

        addMetadataLinks(headers, dto, selfUrl);
        addDocumentFileLinks(headers, dto);

        dto.getFileItems().stream()
            .filter(f -> f.getResourceType().equals(ResourceType.OFFICIAL_PUBLICATION)).findFirst()
            .ifPresent(file -> {
                if (Objects.isNull(file.getAccessRights()) || Objects.isNull(file.getLicense()) ||
                    !file.getAccessRights().equals(AccessRights.OPEN_ACCESS)) {
                    return;
                }
                headers.add(HttpHeaders.LINK,
                    "<" + buildSafeUri("https://spdx.org/licenses/CC-" +
                        file.getLicense().name().replace("_", "-") + "-4.0") +
                        ">; rel=\"license\"");
            });

        if (Objects.nonNull(dto.getContributions())) {
            dto.getContributions().stream()
                .filter(c -> c.getContributionType().equals(DocumentContributionType.AUTHOR))
                .forEach(author -> {
                    if (Objects.nonNull(author.getPersonId()) && author.getPersonId() > 0) {
                        headers.add(HttpHeaders.LINK,
                            "<" + frontendUrl + defaultLocale + "/persons/" + author.getPersonId() +
                                ">; rel=\"author\"");
                    }
                });
        }

        addLinksetReferences(headers, "/api/document", String.valueOf(dto.getId()));
    }

    private static void deduceResourceType(HttpHeaders headers, DocumentDTO dto) {
        switch (dto) {
            case JournalPublicationResponseDTO ignored -> headers.add(HttpHeaders.LINK,
                "<https://schema.org/ScholarlyArticle> ; rel=\"type\"");
            case SoftwareDTO ignored -> headers.add(HttpHeaders.LINK,
                "<https://schema.org/SoftwareApplication> ; rel=\"type\"");
            case DatasetDTO ignored ->
                headers.add(HttpHeaders.LINK, "<https://schema.org/Dataset> ; rel=\"type\"");
            case PatentDTO ignored ->
                headers.add(HttpHeaders.LINK, "<https://schema.org/result> ; rel=\"type\"");
            case ProceedingsResponseDTO ignored ->
                headers.add(HttpHeaders.LINK, "<https://schema.org/Collection> ; rel=\"type\"");
            case ProceedingsPublicationDTO ignored -> headers.add(HttpHeaders.LINK,
                "<https://schema.org/ScholarlyArticle> ; rel=\"type\"");
            case MonographDTO ignored ->
                headers.add(HttpHeaders.LINK, "<https://schema.org/Book> ; rel=\"type\"");
            case MonographPublicationDTO ignored ->
                headers.add(HttpHeaders.LINK, "<https://schema.org/Chapter> ; rel=\"type\"");
            case ThesisResponseDTO ignored ->
                headers.add(HttpHeaders.LINK, "<https://schema.org/Thesis> ; rel=\"type\"");
            default -> {
            }
        }
    }

    private static String deduceDocumentType(Document document) {
        return switch (document) {
            case JournalPublication ignored -> "journal-publication";
            case Software ignored -> "software";
            case Dataset ignored -> "dataset";
            case Patent ignored -> "patent";
            case Proceedings ignored -> "proceedings";
            case ProceedingsPublication ignored -> "proceedings-publication";
            case Monograph ignored -> "monograph";
            case MonographPublication ignored -> "monograph-publication";
            case Thesis ignored -> "thesis";
            default -> "";
        };
    }

    private static void setCollectionRelatedFields(HttpHeaders headers, DocumentDTO dto) {
        switch (dto) {
            case JournalPublicationResponseDTO journalPublicationResponseDTO ->
                headers.add(HttpHeaders.LINK, "<" + frontendUrl + defaultLocale + "/journals/" +
                    journalPublicationResponseDTO.getJournalId() +
                    "> ; rel=\"collection\" ; type=\"text/html\"");
            case ProceedingsPublicationDTO proceedingsPublicationDTO ->
                headers.add(HttpHeaders.LINK, "<" + frontendUrl + defaultLocale + "/journals/" +
                    proceedingsPublicationDTO.getProceedingsId() +
                    "> ; rel=\"collection\" ; type=\"text/html\"");
            case MonographPublicationDTO monographPublicationDTO ->
                headers.add(HttpHeaders.LINK, "<" + frontendUrl + defaultLocale + "/journals/" +
                    monographPublicationDTO.getMonographId() +
                    "> ; rel=\"collection\" ; type=\"text/html\"");
            default -> {
            }
        }
    }

    static boolean valuePresent(String value) {
        return Objects.nonNull(value) && !value.isBlank();
    }

    private static void addDocumentFileLinks(HttpHeaders headers, DocumentDTO dto) {
        addHeadersForDocumentFiles(headers, dto.getFileItems());
        addHeadersForDocumentFiles(headers, dto.getProofs());

        if (dto instanceof ThesisResponseDTO) {
            addHeadersForDocumentFiles(headers, ((ThesisResponseDTO) dto).getPreliminaryFiles());
            addHeadersForDocumentFiles(headers,
                ((ThesisResponseDTO) dto).getPreliminarySupplements());
            addHeadersForDocumentFiles(headers, ((ThesisResponseDTO) dto).getCommissionReports());
        }
    }

    private static void addHeadersForDocumentFiles(HttpHeaders headers,
                                                   Collection<DocumentFileResponseDTO> items) {
        if (Objects.isNull(items)) {
            return;
        }

        items.stream()
            .filter(df -> df.getAccessRights().equals(AccessRights.OPEN_ACCESS))
            .forEach(file ->
                headers.add(HttpHeaders.LINK,
                    "<" + buildSafeUri(baseUrl + "/api/file/" + file.getServerFilename()) +
                        ">; rel=\"item\" type=\"" + file.getMimeType() + "\"")
            );
    }

    private static void addMetadataLinks(HttpHeaders headers, DocumentDTO dto, String selfUrl) {
        headers.add(HttpHeaders.LINK,
            "<" + baseUrl + selfUrl + "/" + dto.getId() +
                ">; rel=\"describedby\"; type=\"application/json\"");
        headers.add(HttpHeaders.LINK,
            "<" + baseUrl + "/api/document/metadata/" + dto.getId() + "/BIBTEX" +
                ">; rel=\"describedby\"; type=\"" + BibliographicFormat.BIBTEX.getValue() + "\"");
        headers.add(HttpHeaders.LINK,
            "<" + baseUrl + "/api/document/metadata/" + dto.getId() + "/REFMAN" +
                ">; rel=\"describedby\"; type=\"" + BibliographicFormat.REFMAN.getValue() + "\"");
        headers.add(HttpHeaders.LINK,
            "<" + baseUrl + "/api/document/metadata/" + dto.getId() + "/ENDNOTE" +
                ">; rel=\"describedby\"; type=\"" + BibliographicFormat.ENDNOTE.getValue() + "\"");

        if (dto instanceof ThesisResponseDTO) {
            for (var libraryFormat : LibraryFormat.values()) {
                headers.add(HttpHeaders.LINK,
                    "<" + buildSafeUri(baseUrl + selfUrl + "/library-format/" + dto.getId() + "/" +
                        libraryFormat.name()) + ">; rel=\"describedby\"; type=\"" +
                        libraryFormat.getValue() + "\"");
            }
        }
    }

    private static void addLinksetReferences(HttpHeaders headers, String selfUrl, String entityId) {
        headers.add(HttpHeaders.LINK,
            "<" + buildSafeUri(baseUrl + selfUrl + "/linkset/" + entityId + "/JSON") +
                ">; rel=\"describedby\"; type=\"" + LinksetFormat.JSON.getValue() + "\"");
        headers.add(HttpHeaders.LINK,
            "<" + buildSafeUri(baseUrl + selfUrl + "/linkset/" + entityId + "/LINKSET") +
                ">; rel=\"describedby\"; type=\"" + LinksetFormat.LINKSET.getValue() + "\"");
    }

    public static String buildSafeUri(String uri) {
        try {
            return new URI(uri).toASCIIString();
        } catch (URISyntaxException e) {
            return URLEncoder.encode(uri, StandardCharsets.UTF_8)
                .replace("+", "%20"); // Simple fallback
        }
    }

    @Value("${export.base.url}")
    public void setBaseUrl(String baseUrl) {
        FairSignpostingL1Utility.baseUrl = baseUrl;
    }

    @Value("${frontend.application.address}")
    public void setFrontendUrl(String frontendUrl) {
        FairSignpostingL1Utility.frontendUrl = frontendUrl;
    }

    @Value("${default.locale}")
    public void setDefaultLocale(String defaultLocale) {
        FairSignpostingL1Utility.defaultLocale = defaultLocale;
    }
}

