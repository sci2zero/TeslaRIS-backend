package rs.teslaris.core.converter.document;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.function.BiConsumer;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.stream.Collectors;
import org.jbibtex.BibTeXEntry;
import org.jbibtex.Key;
import org.jbibtex.StringValue;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import rs.teslaris.core.converter.commontypes.MultilingualContentConverter;
import rs.teslaris.core.dto.document.ThesisResponseDTO;
import rs.teslaris.core.model.commontypes.LanguageTag;
import rs.teslaris.core.model.commontypes.MultiLingualContent;
import rs.teslaris.core.model.document.AccessRights;
import rs.teslaris.core.model.document.DocumentContributionType;
import rs.teslaris.core.model.document.DocumentFile;
import rs.teslaris.core.model.document.Thesis;
import rs.teslaris.core.model.oaipmh.dublincore.Contributor;
import rs.teslaris.core.model.oaipmh.dublincore.DC;
import rs.teslaris.core.model.oaipmh.dublincore.DCMultilingualContent;
import rs.teslaris.core.model.oaipmh.etdms.Degree;
import rs.teslaris.core.model.oaipmh.etdms.ETDMSThesis;
import rs.teslaris.core.model.oaipmh.etdms.LevelType;
import rs.teslaris.core.model.oaipmh.etdms.ThesisType;
import rs.teslaris.core.model.oaipmh.marc21.ControlField;
import rs.teslaris.core.model.oaipmh.marc21.DataField;
import rs.teslaris.core.model.oaipmh.marc21.Marc21;
import rs.teslaris.core.model.oaipmh.marc21.SubField;
import rs.teslaris.core.model.person.PersonName;
import rs.teslaris.core.util.configuration.PublicReviewConfigurationLoader;
import rs.teslaris.core.util.functional.Pair;
import rs.teslaris.core.util.persistence.IdentifierUtil;
import rs.teslaris.core.util.search.StringUtil;


@Component
public class ThesisConverter extends DocumentPublicationConverter {

    private static String repositoryName;

    private static String baseFrontendUrl;

    private static List<String> clientLanguages = new ArrayList<>();


    public static ThesisResponseDTO toDTO(Thesis thesis) {
        var thesisDTO = new ThesisResponseDTO();

        setCommonFields(thesis, thesisDTO);
        setThesisRelatedFields(thesis, thesisDTO);

        return thesisDTO;
    }

    public static BibTeXEntry toBibTexEntry(Thesis thesis, String defaultLanguageTag) {
        var type = new Key("thesis");
        if (thesis.getThesisType().equals(rs.teslaris.core.model.document.ThesisType.PHD) ||
            thesis.getThesisType()
                .equals(rs.teslaris.core.model.document.ThesisType.PHD_ART_PROJECT)) {
            type = BibTeXEntry.TYPE_PHDTHESIS;
        } else if (thesis.getThesisType()
            .equals(rs.teslaris.core.model.document.ThesisType.MASTER)) {
            type = BibTeXEntry.TYPE_MASTERSTHESIS;
        }

        var entry = new BibTeXEntry(type,
            new Key(IdentifierUtil.identifierPrefix + thesis.getId().toString()));

        setCommonFields(thesis, entry, defaultLanguageTag);

        if (Objects.nonNull(thesis.getAlternateTitle())) {
            setMCBibTexField(thesis.getAlternateTitle(), entry, new Key("alternateTitle"),
                defaultLanguageTag);
        }

        if (Objects.nonNull(thesis.getPublisher())) {
            setMCBibTexField(thesis.getPublisher().getName(), entry, BibTeXEntry.KEY_PUBLISHER,
                defaultLanguageTag);
        } else if (Objects.nonNull(thesis.getAuthorReprint()) && thesis.getAuthorReprint()) {
            entry.addField(BibTeXEntry.KEY_PUBLISHER,
                new StringValue(getAuthorReprintString(defaultLanguageTag),
                    StringValue.Style.BRACED));
        }

        if (Objects.nonNull(thesis.getOrganisationUnit())) {
            setMCBibTexField(thesis.getOrganisationUnit().getName(), entry,
                BibTeXEntry.KEY_INSTITUTION, defaultLanguageTag);
        } else if (Objects.nonNull(thesis.getExternalOrganisationUnitName())) {
            setMCBibTexField(thesis.getExternalOrganisationUnitName(), entry,
                BibTeXEntry.KEY_INSTITUTION, defaultLanguageTag);
        }

        if (StringUtil.valueExists(thesis.getEISBN())) {
            entry.addField(new Key("eIsbn"),
                new StringValue(thesis.getEISBN(), StringValue.Style.BRACED));
        }

        if (StringUtil.valueExists(thesis.getPrintISBN())) {
            entry.addField(new Key("printIsbn"),
                new StringValue(thesis.getPrintISBN(), StringValue.Style.BRACED));
        }

        return entry;
    }

    public static String toTaggedFormat(Thesis thesis, String defaultLanguageTag, boolean refMan) {
        var sb = new StringBuilder();
        sb.append(refMan ? "TY  - " : "%0 ").append(refMan ? "THES" : "Thesis").append("\n");

        setCommonTaggedFields(thesis, sb, defaultLanguageTag, refMan);

        if (Objects.nonNull(thesis.getAlternateTitle())) {
            setMCTaggedField(thesis.getAlternateTitle(), sb, refMan ? "T2" : "%0T",
                defaultLanguageTag);
        }

        if (Objects.nonNull(thesis.getPublisher())) {
            setMCTaggedField(thesis.getPublisher().getName(), sb, refMan ? "PB" : "%I",
                defaultLanguageTag);
        } else if (Objects.nonNull(thesis.getAuthorReprint()) && thesis.getAuthorReprint()) {
            sb.append(refMan ? "PB  - " : "%I ").append(getAuthorReprintString(defaultLanguageTag))
                .append("\n");
        }

        if (Objects.nonNull(thesis.getOrganisationUnit())) {
            setMCTaggedField(thesis.getOrganisationUnit().getName(), sb, refMan ? "A2" : "%C",
                defaultLanguageTag);
        } else if (Objects.nonNull(thesis.getExternalOrganisationUnitName())) {
            setMCTaggedField(thesis.getExternalOrganisationUnitName(), sb, refMan ? "A2" : "%C",
                defaultLanguageTag);
        }

        if (StringUtil.valueExists(thesis.getEISBN())) {
            sb.append(refMan ? "SN  - e:" : "%@ ").append(thesis.getEISBN()).append("\n");
        }

        if (StringUtil.valueExists(thesis.getPrintISBN())) {
            sb.append(refMan ? "SN  - print:" : "%@ ").append(thesis.getPrintISBN()).append("\n");
        }

        if (refMan) {
            sb.append("ER  -\n");
        }

        return sb.toString();
    }

    private static void setThesisRelatedFields(Thesis thesis, ThesisResponseDTO thesisDTO) {
        if (Objects.nonNull(thesis.getOrganisationUnit())) {
            thesisDTO.setOrganisationUnitId(thesis.getOrganisationUnit().getId());
        } else {
            thesisDTO.setExternalOrganisationUnitName(
                MultilingualContentConverter.getMultilingualContentDTO(
                    thesis.getExternalOrganisationUnitName()));
        }

        thesisDTO.setThesisType(thesis.getThesisType());
        thesisDTO.setTopicAcceptanceDate(thesis.getTopicAcceptanceDate());
        thesisDTO.setThesisDefenceDate(thesis.getThesisDefenceDate());
        thesisDTO.setIsOnPublicReview(thesis.getIsOnPublicReview());
        thesisDTO.setIsOnPublicReviewPause(thesis.getIsOnPublicReviewPause());
        thesisDTO.setPublicReviewDates(
            thesis.getPublicReviewStartDates().stream().sorted().toList());

        thesisDTO.setAlternateTitle(
            MultilingualContentConverter.getMultilingualContentDTO(thesis.getAlternateTitle()));
        thesisDTO.setExtendedAbstract(
            MultilingualContentConverter.getMultilingualContentDTO(thesis.getExtendedAbstract()));

        if (Objects.nonNull(thesis.getPhysicalDescription())) {
            var physicalDescription = thesis.getPhysicalDescription();
            thesisDTO.setNumberOfPages(physicalDescription.getNumberOfPages());
            thesisDTO.setNumberOfReferences(physicalDescription.getNumberOfReferences());
            thesisDTO.setNumberOfChapters(physicalDescription.getNumberOfChapters());
            thesisDTO.setNumberOfGraphs(physicalDescription.getNumberOfGraphs());
            thesisDTO.setNumberOfTables(physicalDescription.getNumberOfTables());
            thesisDTO.setNumberOfIllustrations(physicalDescription.getNumberOfIllustrations());
            thesisDTO.setNumberOfAppendices(physicalDescription.getNumberOfAppendices());
        }

        if (thesisDTO.getIsOnPublicReview() && Objects.nonNull(thesisDTO.getPublicReviewDates()) &&
            !thesisDTO.getPublicReviewDates().isEmpty()) {
            thesisDTO.setPublicReviewEnd(
                thesisDTO.getPublicReviewDates().getLast()
                    .plusDays(PublicReviewConfigurationLoader.getLengthInDays(
                        Objects.requireNonNullElse(thesis.getIsShortenedReview(), false)))
            );
        }

        if (Objects.nonNull(thesis.getLanguage())) {
            thesisDTO.setLanguageCode(thesis.getLanguage().getLanguageCode());
            thesisDTO.setLanguageId(thesis.getLanguage().getId());
        }

        if (Objects.nonNull(thesis.getWritingLanguage())) {
            thesisDTO.setWritingLanguageTagId(thesis.getWritingLanguage().getId());
        }

        thesisDTO.setScientificArea(
            MultilingualContentConverter.getMultilingualContentDTO(thesis.getScientificArea()));
        thesisDTO.setScientificSubArea(
            MultilingualContentConverter.getMultilingualContentDTO(thesis.getScientificSubArea()));
        thesisDTO.setEisbn(thesis.getEISBN());
        thesisDTO.setPrintISBN(thesis.getPrintISBN());
        thesisDTO.setPlaceOfKeep(
            MultilingualContentConverter.getMultilingualContentDTO(thesis.getPlaceOfKeeping()));
        thesisDTO.setUdc(thesis.getUdc());
        thesisDTO.setTypeOfTitle(
            MultilingualContentConverter.getMultilingualContentDTO(thesis.getTypeOfTitle()));
        thesisDTO.setPublicReviewCompleted(thesis.getPublicReviewCompleted());
        thesisDTO.setIsShortenedReview(
            Objects.requireNonNullElse(thesis.getIsShortenedReview(), false));

        if (Objects.nonNull(thesis.getPublisher())) {
            thesisDTO.setPublisherId(thesis.getPublisher().getId());
        } else {
            thesisDTO.setAuthorReprint(thesis.getAuthorReprint());
        }

        thesis.getPreliminaryFiles().forEach(file -> {
            thesisDTO.getPreliminaryFiles().add(DocumentFileConverter.toDTO(file));
        });

        thesis.getPreliminarySupplements().forEach(supplement -> {
            thesisDTO.getPreliminarySupplements().add(DocumentFileConverter.toDTO(supplement));
        });

        thesis.getCommissionReports().forEach(commissionReport -> {
            thesisDTO.getCommissionReports().add(DocumentFileConverter.toDTO(commissionReport));
        });
    }

    public static DC toDCModel(Thesis thesis) {
        var dcThesis = new DC();
        setDCCommonFields(thesis, dcThesis);

        return dcThesis;
    }

    public static ETDMSThesis toETDMSModel(Thesis thesis) {
        var thesisType = new ThesisType();

        setDCCommonFields(thesis, thesisType);

        var degree = new Degree();
        addContentToList(
            thesis.getOrganisationUnit().getName(),
            MultiLingualContent::getContent,
            content -> degree.getGrantor().add(content)
        );
        degree.setLevel(
            new LevelType(String.valueOf(thesis.getThesisType().ordinal() % 3)));
        degree.getName().add(thesis.getThesisType().name());
        thesisType.setDegree(degree);

        var etdmsThesis = new ETDMSThesis();
        etdmsThesis.setThesisType(thesisType);
        return etdmsThesis;
    }

    private static void setDCCommonFields(Thesis exportDocument, DC dcPublication) {
        dcPublication.getDate().add(exportDocument.getDocumentDate());
        dcPublication.getSource().add(repositoryName);

        addIdentifiers(exportDocument, dcPublication);
        addTitles(exportDocument, dcPublication);
        addContributors(exportDocument, dcPublication);
        addDescriptions(exportDocument, dcPublication);
        addKeywords(exportDocument, dcPublication);
        addLanguage(exportDocument, dcPublication);
        addPublisher(exportDocument, dcPublication);
        addFormats(exportDocument, dcPublication);
        addRelations(exportDocument, dcPublication);
        addRights(exportDocument, dcPublication);
    }

    private static void addIdentifiers(Thesis exportDocument, DC dcPublication) {
        dcPublication.getIdentifier().add(exportDocument.getDoi());

        clientLanguages.forEach(lang ->
            dcPublication.getIdentifier().add(
                baseFrontendUrl + lang + "/scientific-results/thesis/" + exportDocument.getId()
            )
        );
    }

    private static void addTitles(Thesis exportDocument, DC dcPublication) {
        addContentToList(
            exportDocument.getTitle(),
            MultiLingualContent::getContent,
            MultiLingualContent::getLanguage,
            (content, languageTag) -> dcPublication.getTitle()
                .add(new DCMultilingualContent(content, languageTag))
        );
    }

    private static void addContributors(Thesis exportDocument, DC dcPublication) {
        var roleMap = Map.of(
            DocumentContributionType.AUTHOR, "creator",
            DocumentContributionType.EDITOR, "editor",
            DocumentContributionType.ADVISOR, "advisor",
            DocumentContributionType.BOARD_MEMBER, "board_member"
        );

        roleMap.forEach((type, role) -> {
                getContributorNames(exportDocument, type).forEach(
                    nameAndIdentifier ->
                        addContributor(
                            dcPublication,
                            nameAndIdentifier.a.toString(),
                            role,
                            nameAndIdentifier.b));
            }
        );
    }

    private static Set<Pair<PersonName, String>> getContributorNames(Thesis exportDocument,
                                                                     DocumentContributionType type) {
        return exportDocument.getContributors().stream()
            .filter(contribution -> contribution.getContributionType().equals(type))
            .map(contribution -> new Pair<>(
                contribution.getAffiliationStatement().getDisplayPersonName(),
                Objects.nonNull(contribution.getPerson()) ?
                    Objects.requireNonNullElse(contribution.getPerson().getOrcid(), "") : ""))
            .collect(Collectors.toSet());
    }

    private static void addContributor(DC dcPublication, String name, String role, String orcid) {
        if ("creator".equals(role)) {
            dcPublication.getCreator().add(name);
        } else {
            dcPublication.getContributor().add(
                new Contributor(name, role, orcid.isBlank() ? "" : "https://orcid.org/" + orcid));
        }
    }

    private static void addDescriptions(Thesis exportDocument, DC dcPublication) {
        addContentToList(
            exportDocument.getDescription(),
            MultiLingualContent::getContent,
            MultiLingualContent::getLanguage,
            (content, languageTag) -> dcPublication.getDescription()
                .add(new DCMultilingualContent(content, languageTag))
        );
    }

    private static void addKeywords(Thesis exportDocument, DC dcPublication) {
        addContentToList(
            exportDocument.getKeywords(),
            MultiLingualContent::getContent,
            MultiLingualContent::getLanguage,
            (content, languageTag) -> dcPublication.getSubject()
                .add(new DCMultilingualContent(content.replace("\n", "; "), languageTag))
        );
    }

    private static void addLanguage(Thesis exportDocument, DC dcPublication) {
        if (Objects.nonNull(exportDocument.getLanguage())) {
            addContentToList(
                Set.of(exportDocument.getLanguage().getLanguageCode()),
                Function.identity(),
                dcPublication.getLanguage()::add
            );
        }
    }

    private static void addPublisher(Thesis exportDocument, DC dcPublication) {
        if (Objects.nonNull(exportDocument.getPublisher())) {
            addContentToList(
                exportDocument.getPublisher().getName(),
                MultiLingualContent::getContent,
                MultiLingualContent::getLanguage,
                (content, languageTag) -> dcPublication.getPublisher()
                    .add(new DCMultilingualContent(content, languageTag))
            );
        }
    }

    private static void addFormats(Thesis exportDocument, DC dcPublication) {
        addContentToList(
            exportDocument.getFileItems().stream().map(DocumentFile::getMimeType)
                .collect(Collectors.toSet()),
            Function.identity(),
            dcPublication.getFormat()::add
        );
    }

    private static void addRelations(Thesis exportDocument, DC dcPublication) {
        if (Objects.nonNull(exportDocument.getDoi()) && !exportDocument.getDoi().isBlank()) {
            dcPublication.getRelation()
                .add("info:eu-repo/semantics/altIdentifier/doi/" + exportDocument.getDoi());
        }
    }

    private static void addRights(Thesis exportDocument, DC dcPublication) {
        boolean isOpenAccess = exportDocument.getFileItems().stream().anyMatch(
            fileItem -> fileItem.getAccessRights().equals(AccessRights.OPEN_ACCESS)
        );

        dcPublication.getRights().add(isOpenAccess ?
            "info:eu-repo/semantics/openAccess" :
            "info:eu-repo/semantics/metadataOnlyAccess"
        );
        dcPublication.getRights().add("http://creativecommons.org/publicdomain/zero/1.0/");
    }

    protected static <T> void addContentToList(Set<T> sourceList,
                                               Function<T, String> preprocessingFunction,
                                               Consumer<String> consumer) {
        sourceList.forEach(item -> {
            if (Objects.isNull(item)) {
                return;
            }

            if ((item instanceof String) && ((String) item).isBlank()) {
                return;
            }

            consumer.accept(preprocessingFunction.apply(item));
        });
    }

    protected static <T> void addContentToList(Set<T> sourceList,
                                               Function<T, String> contentExtractionFunction,
                                               Function<T, LanguageTag> languageTagExtractionFunction,
                                               BiConsumer<String, String> consumer) {
        sourceList.forEach(item -> {
            if (Objects.isNull(item)) {
                return;
            }

            if ((item instanceof String) && ((String) item).isBlank()) {
                return;
            }

            consumer.accept(contentExtractionFunction.apply(item),
                languageTagExtractionFunction.apply(item).getLanguageTag().toLowerCase());
        });
    }

    public static Marc21 convertToMarc21(Thesis exportDocument) {
        Marc21 marc21 = new Marc21();
        marc21.setLeader("ca a2 n");

        marc21.getControlFields().add(new ControlField("001", exportDocument.getId().toString()));

        if (Objects.nonNull(exportDocument.getDoi())) {
            marc21.getDataFields()
                .add(createDataField("024", "7", " ", "a", exportDocument.getDoi()));
        }

        clientLanguages.forEach(lang ->
            marc21.getDataFields().add(createDataField("856", "4", "1", "u",
                baseFrontendUrl + lang + "/scientific-results/thesis/" + exportDocument.getId()))
        );

        addContentToMarc21(marc21, "245", "1", "0", exportDocument.getTitle(),
            MultiLingualContent::getContent, "a");

        addContributorsToMarc21(marc21, exportDocument, DocumentContributionType.AUTHOR, "100", "1",
            " ");

        addContributorsToMarc21(marc21, exportDocument, DocumentContributionType.EDITOR, "700", "1",
            " ");
        addContributorsToMarc21(marc21, exportDocument, DocumentContributionType.ADVISOR, "700",
            "1", " ");
        addContributorsToMarc21(marc21, exportDocument, DocumentContributionType.BOARD_MEMBER,
            "700", "1", " ");

        addContentToMarc21(marc21, "520", " ", " ", exportDocument.getDescription(),
            MultiLingualContent::getContent, "a");

        addContentToMarc21(marc21, "650", " ", "7", exportDocument.getKeywords(),
            MultiLingualContent::getContent, "a");

        if (Objects.nonNull(exportDocument.getLanguage())) {
            marc21.getDataFields().add(createDataField("041", " ", " ", "a",
                exportDocument.getLanguage().getLanguageCode()));
        }

        if (Objects.nonNull(exportDocument.getPublisher())) {
            addContentToMarc21(marc21, "260", " ", " ", exportDocument.getPublisher().getName(),
                MultiLingualContent::getContent, "b");
        }

        addContentToMarc21(marc21, "856", "4", "1", exportDocument.getFileItems().stream()
            .map(DocumentFile::getMimeType).collect(Collectors.toSet()), Function.identity(), "q");

        var isOpenAccess = exportDocument.getFileItems().stream().anyMatch(
            fileItem -> fileItem.getAccessRights().equals(AccessRights.OPEN_ACCESS));

        String accessRights = isOpenAccess ?
            "info:eu-repo/semantics/openAccess" :
            "info:eu-repo/semantics/metadataOnlyAccess";

        marc21.getDataFields().add(createDataField("506", " ", " ", "a", accessRights));
        marc21.getDataFields().add(createDataField("540", " ", " ", "a",
            "http://creativecommons.org/publicdomain/zero/1.0/"));

        return marc21;
    }

    private static void addContributorsToMarc21(Marc21 marc21, Thesis exportDocument,
                                                DocumentContributionType type, String tag,
                                                String ind1, String ind2) {
        addContentToMarc21(
            marc21, tag, ind1, ind2,
            exportDocument.getContributors().stream()
                .filter(contribution -> contribution.getContributionType().equals(type))
                .map(contribution -> contribution.getAffiliationStatement().getDisplayPersonName())
                .collect(Collectors.toSet()),
            Object::toString, "a"
        );
    }

    private static <T> void addContentToMarc21(Marc21 marc21, String tag, String ind1, String ind2,
                                               Set<T> content, Function<T, String> extractor,
                                               String subfieldCode) {
        content.stream().map(extractor).forEach(value ->
            marc21.getDataFields().add(createDataField(tag, ind1, ind2, subfieldCode, value))
        );
    }

    private static DataField createDataField(String tag, String ind1, String ind2,
                                             String subfieldCode, String value) {
        var dataField = new DataField(tag, ind1, ind2, new ArrayList<>());
        dataField.getSubFields().add(new SubField(subfieldCode, value));
        return dataField;
    }

    @Value("${export.repo.name}")
    public void setRepositoryName(String repositoryName) {
        ThesisConverter.repositoryName = repositoryName;
    }

    @Value("${frontend.application.address}")
    public void setBaseFrontendUrl(String baseFrontendUrl) {
        ThesisConverter.baseFrontendUrl = baseFrontendUrl;
    }

    @Value("${client.localization.languages}")
    public void setClientLanguages(List<String> clientLanguages) {
        ThesisConverter.clientLanguages = clientLanguages;
    }
}
