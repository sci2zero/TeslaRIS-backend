package rs.teslaris.core.util.signposting;

import java.util.Collection;
import java.util.Objects;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpHeaders;
import org.springframework.stereotype.Component;
import rs.teslaris.core.dto.document.DatasetDTO;
import rs.teslaris.core.dto.document.DocumentDTO;
import rs.teslaris.core.dto.document.DocumentFileResponseDTO;
import rs.teslaris.core.dto.document.JournalPublicationResponseDTO;
import rs.teslaris.core.dto.document.MonographDTO;
import rs.teslaris.core.dto.document.MonographPublicationDTO;
import rs.teslaris.core.dto.document.PatentDTO;
import rs.teslaris.core.dto.document.ProceedingsPublicationDTO;
import rs.teslaris.core.dto.document.ProceedingsResponseDTO;
import rs.teslaris.core.dto.document.SoftwareDTO;
import rs.teslaris.core.dto.document.ThesisResponseDTO;
import rs.teslaris.core.model.document.AccessRights;
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

@Component
public class FairSignposting {

    private static String baseUrl;

    private static String frontendUrl;

    private static String defaultLocale;


    private FairSignposting() {
        // utility class
    }

    public static void addHeadersForDocumentFileItems(HttpHeaders headers,
                                                      DocumentFile documentFile) {
        if (Objects.nonNull(documentFile.getDocument())) {
            headers.add(HttpHeaders.LINK,
                "<" + frontendUrl + defaultLocale + "/scientific-results/" +
                    deduceDocumentType(documentFile.getDocument()) + "/" +
                    documentFile.getDocument().getId() +
                    "> ; rel=\"collection\" ; type=\"text/html\"");
        }
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
                "<https://doi.org/" + dto.getDoi() + ">; rel=\"cite-as\"");
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
                "<https://openalex.org/" + dto.getDoi() + ">; rel=\"cite-as\"");
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
                    "<https://spdx.org/licenses/CC-" + file.getLicense().name().replace("_", "-") +
                        "-4.0>; rel=\"license\"");
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

    private static boolean valuePresent(String value) {
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
                    "<" + baseUrl + "/api/file/" + file.getFileName() + ">; rel=\"item\" type=\"" +
                        file.getMimeType() + "\"")
            );
    }

    private static void addMetadataLinks(HttpHeaders headers, DocumentDTO dto, String selfUrl) {
        headers.add(HttpHeaders.LINK,
            "<" + baseUrl + selfUrl + "/" + dto.getId() +
                ">; rel=\"describedby\"; type=\"application/json\"");

        if (dto instanceof ThesisResponseDTO) {
            for (var libraryFormat : LibraryFormat.values()) {
                headers.add(HttpHeaders.LINK,
                    "<" + baseUrl + selfUrl + "/library-format/" + dto.getId() + "/" +
                        libraryFormat.name() + ">; rel=\"describedby\"; type=\"" +
                        libraryFormat.getValue() + "\"");
            }
        }
    }

    @Value("${export.base.url}")
    public void setBaseUrl(String baseUrl) {
        FairSignposting.baseUrl = baseUrl;
    }

    @Value("${frontend.application.address}")
    public void setFrontendUrl(String frontendUrl) {
        FairSignposting.frontendUrl = frontendUrl;
    }

    @Value("${default.locale}")
    public void setDefaultLocale(String defaultLocale) {
        FairSignposting.defaultLocale = defaultLocale;
    }
}

