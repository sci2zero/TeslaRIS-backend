package rs.teslaris.exporter.model.converter.skgif;

import java.time.format.DateTimeFormatter;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import rs.teslaris.core.model.document.AccessRights;
import rs.teslaris.core.model.document.ResourceType;
import rs.teslaris.core.model.skgif.common.SKGIFAccessRights;
import rs.teslaris.core.model.skgif.researchproduct.BibliographicInfo;
import rs.teslaris.core.model.skgif.researchproduct.Manifestation;
import rs.teslaris.core.model.skgif.researchproduct.ManifestationDates;
import rs.teslaris.core.model.skgif.researchproduct.PageRange;
import rs.teslaris.core.model.skgif.researchproduct.RelatedProducts;
import rs.teslaris.core.model.skgif.researchproduct.ResearchProduct;
import rs.teslaris.core.model.skgif.researchproduct.SKGIFContribution;
import rs.teslaris.core.model.skgif.researchproduct.TypeInfo;
import rs.teslaris.core.util.persistence.IdentifierUtil;
import rs.teslaris.core.util.search.StringUtil;
import rs.teslaris.exporter.model.common.ExportContribution;
import rs.teslaris.exporter.model.common.ExportDocument;
import rs.teslaris.exporter.model.common.ExportMultilingualContent;
import rs.teslaris.exporter.model.common.ExportPublicationType;

public class ResearchProductConverter extends BaseConverter {

    public static List<ResearchProduct> toSKGIF(ExportDocument document) {
        var researchProduct = new ResearchProduct();
        researchProduct.setLocalIdentifier(
            IdentifierUtil.identifierPrefix + document.getDatabaseId());

        populateIdentifiers(researchProduct.getIdentifiers(), document);

        researchProduct.setEntityType("product");
        researchProduct.setProductType(getProductType(document.getType()));

        if (StringUtil.valueExists(document.getDocumentDate())) {
            researchProduct.setCreationDate(document.getDocumentDate() +
                (document.getDocumentDate().length() == 4 ? "-01-01T00:00:00" : ""));
        }

        researchProduct.setTitles(getMultilingualContent(document.getTitle()));
        researchProduct.setAbstracts(getMultilingualContent(document.getDescription()));

        populateContributorInformation(researchProduct, document);

        researchProduct.setRelevantOrganisations(
            document.getActivelyRelatedInstitutionIds().stream()
                .map(id -> IdentifierUtil.identifierPrefix + id).toList());

        populateDocumentManifestations(researchProduct, document);

        researchProduct.setRelatedProducts(RelatedProducts.builder().isSupplementedBy(
            document.getResearchOutput()
                .stream().map(id -> IdentifierUtil.identifierPrefix + id).toList()).build());

        return List.of(researchProduct);
    }

    private static void populateContributorInformation(ResearchProduct researchProduct,
                                                       ExportDocument document) {
        addToContributions(researchProduct, document.getAuthors(), "author",
            List.of("writing – original draft", "conceptualization"));
        addToContributions(researchProduct, document.getEditors(), "editor",
            List.of("writing – review & editing"));
        addToContributions(researchProduct, document.getAdvisors(), "advisor",
            List.of("supervision", "validation"));
    }

    private static void addToContributions(ResearchProduct researchProduct,
                                           List<ExportContribution> contributions, String role,
                                           List<String> contributionTypes) {
        contributions.forEach(author -> {
            var contribution = new SKGIFContribution();

            contribution.setBy(Objects.nonNull(author.getPerson()) ?
                (IdentifierUtil.identifierPrefix + author.getPerson().getDatabaseId()) :
                author.getDisplayName());
            contribution.setRole(role);
            contribution.setContributionTypes(contributionTypes);
            contribution.setRank(author.getOrderNumber());

            contribution.setDeclaredAffiliations(author.getDeclaredContributions().stream()
                .map(id -> IdentifierUtil.identifierPrefix + id).toList());

            researchProduct.getContributions().add(contribution);
        });
    }

    private static String getProductType(ExportPublicationType type) {
        return switch (type) {
            case JOURNAL_PUBLICATION, PROCEEDINGS_PUBLICATION, MONOGRAPH_PUBLICATION, THESIS ->
                "literature";
            case SOFTWARE -> "research software";
            case DATASET -> "research data";
            default ->
                throw new IllegalArgumentException("No RP entity type type for: " + type.name());
        };
    }

    private static Map<String, List<String>> getMultilingualContent(
        List<ExportMultilingualContent> multilingualContent) {
        var mc = new HashMap<String, List<String>>();
        multilingualContent.forEach(
            title -> mc.put("@" + title.getLanguageTag(), List.of(title.getContent())));

        return mc;
    }

    public static String getFabioUri(ResourceType resourceType) {
        return switch (resourceType) {
            case PREPRINT -> "http://purl.org/spar/fabio/Preprint";
            case OFFICIAL_PUBLICATION -> "http://purl.org/spar/fabio/Article";
            case SUPPLEMENT -> "http://purl.org/spar/fabio/SupplementaryFile";
            case PROOF -> "http://purl.org/spar/fabio/Proof";
            case IMAGE -> "http://purl.org/spar/fabio/Image";
            case STATEMENT -> "http://purl.org/spar/fabio/Statement";
            case ADVISOR_CONFLICT_OF_INTEREST ->
                "http://purl.org/spar/fabio/ConflictOfInterestStatement";
        };
    }

    public static SKGIFAccessRights getAccessRights(AccessRights accessRights) {
        return switch (accessRights) {
            case ALL_RIGHTS_RESERVED, RESTRICTED_ACCESS, COMMISSION_ONLY ->
                new SKGIFAccessRights("closed",
                    "Only administrators can access the manifestation.");
            case EMBARGOED_ACCESS ->
                new SKGIFAccessRights("embargoed", "Access is forbidden in your country.");
            case OPEN_ACCESS ->
                new SKGIFAccessRights("open", "Evenryone can access the manifestation");
        };
    }

    private static void populateDocumentManifestations(ResearchProduct researchProduct,
                                                       ExportDocument document) {
        document.getDocumentFiles().forEach(documentFile -> {
            var manifestation = new Manifestation();
            manifestation.setType(new TypeInfo(
                getFabioUri(documentFile.getType()),
                Map.of("en", documentFile.getType().name().replace("_", " ").toLowerCase()),
                "http://purl.org/spar/fabio"
            ));
            if (documentFile.getAccessRights().equals(AccessRights.OPEN_ACCESS) &&
                Objects.nonNull(documentFile.getLicense())) {
                manifestation.setLicense("https://creativecommons.org/licenses/" +
                    documentFile.getLicense().name().replace("_", "-").toLowerCase() +
                    "/4.0/legalcode.en");
            }

            manifestation.setDates(ManifestationDates.builder()
                .modified(List.of(
                    DateTimeFormatter.ISO_INSTANT.format(
                        documentFile.getLastUpdated().toInstant())))
                .distribution(DateTimeFormatter.ISO_INSTANT.format(
                    documentFile.getCreationDate().toInstant()))
                .build());

            manifestation.setAccessRights(getAccessRights(documentFile.getAccessRights()));
            manifestation.setVersion("1.0.0");
            var bibliographicInfo = BibliographicInfo.builder()
                .in(getPublishedIn(document))
                .volume(document.getVolume())
                .issue(document.getIssue())
                .pages(new PageRange(document.getStartPage(), document.getEndPage()))
                .build();
            manifestation.setBiblio(bibliographicInfo);

            researchProduct.getManifestations().add(manifestation);
        });
    }

    private static String getPublishedIn(ExportDocument document) {
        return switch (document.getType()) {
            case JOURNAL_PUBLICATION -> Objects.nonNull(document.getJournal()) ?
                IdentifierUtil.identifierPrefix + document.getJournal().getDatabaseId() : "";
            case PROCEEDINGS_PUBLICATION -> Objects.nonNull(document.getProceedings()) ?
                IdentifierUtil.identifierPrefix + document.getProceedings().getDatabaseId() : "";
            case MONOGRAPH_PUBLICATION -> Objects.nonNull(document.getMonograph()) ?
                IdentifierUtil.identifierPrefix + document.getMonograph().getDatabaseId() : "";
            default -> "";
        };
    }
}
