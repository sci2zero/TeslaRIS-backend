package rs.teslaris.core.converter.document;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.stream.Collectors;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import rs.teslaris.core.converter.commontypes.MultilingualContentConverter;
import rs.teslaris.core.dto.document.ThesisResponseDTO;
import rs.teslaris.core.model.commontypes.MultiLingualContent;
import rs.teslaris.core.model.document.AccessRights;
import rs.teslaris.core.model.document.DocumentContributionType;
import rs.teslaris.core.model.document.DocumentFile;
import rs.teslaris.core.model.document.Thesis;
import rs.teslaris.core.model.oaipmh.dublincore.Contributor;
import rs.teslaris.core.model.oaipmh.dublincore.DC;
import rs.teslaris.core.model.oaipmh.etdms.Degree;
import rs.teslaris.core.model.oaipmh.etdms.ETDMSThesis;
import rs.teslaris.core.model.oaipmh.etdms.LevelType;
import rs.teslaris.core.model.oaipmh.etdms.ThesisType;
import rs.teslaris.core.model.oaipmh.marc21.ControlField;
import rs.teslaris.core.model.oaipmh.marc21.DataField;
import rs.teslaris.core.model.oaipmh.marc21.Marc21;
import rs.teslaris.core.model.oaipmh.marc21.SubField;
import rs.teslaris.core.model.person.PersonName;


@Component
public class ThesisConverter extends DocumentPublicationConverter {

    private static Integer daysOnPublicReview;

    private static String repositoryName;

    private static String baseFrontendUrl;

    private static List<String> clientLanguages = new ArrayList<>();


    public static ThesisResponseDTO toDTO(Thesis thesis) {
        var thesisDTO = new ThesisResponseDTO();

        setCommonFields(thesis, thesisDTO);
        setThesisRelatedFields(thesis, thesisDTO);

        return thesisDTO;
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
        thesisDTO.setRemark(
            MultilingualContentConverter.getMultilingualContentDTO(thesis.getRemark()));

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

        if (thesisDTO.getIsOnPublicReview()) {
            thesisDTO.setPublicReviewEnd(
                thesisDTO.getPublicReviewDates().getLast().plusDays(daysOnPublicReview));
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

        if (Objects.nonNull(thesis.getPublisher())) {
            thesisDTO.setPublisherId(thesis.getPublisher().getId());
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
            dcPublication.getTitle()::add
        );
    }

    private static void addContributors(Thesis exportDocument, DC dcPublication) {
        var roleMap = Map.of(
            DocumentContributionType.AUTHOR, "creator",
            DocumentContributionType.EDITOR, "editor",
            DocumentContributionType.ADVISOR, "advisor",
            DocumentContributionType.BOARD_MEMBER, "board_member"
        );

        roleMap.forEach((type, role) ->
            addContentToList(
                getContributorNames(exportDocument, type),
                PersonName::toString,
                content -> addContributor(dcPublication, content, role)
            )
        );
    }

    private static Set<PersonName> getContributorNames(Thesis exportDocument,
                                                       DocumentContributionType type) {
        return exportDocument.getContributors().stream()
            .filter(contribution -> contribution.getContributionType().equals(type))
            .map(contribution -> contribution.getAffiliationStatement().getDisplayPersonName())
            .collect(Collectors.toSet());
    }

    private static void addContributor(DC dcPublication, String name, String role) {
        if ("creator".equals(role)) {
            dcPublication.getCreator().add(name);
        } else {
            dcPublication.getContributor().add(new Contributor(name, role));
        }
    }

    private static void addDescriptions(Thesis exportDocument, DC dcPublication) {
        addContentToList(
            exportDocument.getDescription(),
            MultiLingualContent::getContent,
            dcPublication.getDescription()::add
        );
    }

    private static void addKeywords(Thesis exportDocument, DC dcPublication) {
        addContentToList(
            exportDocument.getKeywords(),
            MultiLingualContent::getContent,
            content -> dcPublication.getSubject().add(content.replace("\n", "; "))
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
                dcPublication.getPublisher()::add
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

    @Value("${thesis.public-review.duration-days}")
    public void setConfigValue(Integer value) {
        ThesisConverter.daysOnPublicReview = value;
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
