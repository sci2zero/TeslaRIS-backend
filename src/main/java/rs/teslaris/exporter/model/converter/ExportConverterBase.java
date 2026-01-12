package rs.teslaris.exporter.model.converter;

import jakarta.annotation.PostConstruct;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Date;
import java.util.List;
import java.util.Objects;
import java.util.function.BiConsumer;
import java.util.function.Consumer;
import java.util.function.Function;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.env.Environment;
import org.springframework.stereotype.Component;
import rs.teslaris.core.indexmodel.DocumentPublicationType;
import rs.teslaris.core.model.commontypes.BaseEntity;
import rs.teslaris.core.model.document.JournalPublicationType;
import rs.teslaris.core.model.document.ProceedingsPublicationType;
import rs.teslaris.core.model.document.ThesisType;
import rs.teslaris.core.model.oaipmh.dspaceinternal.Dim;
import rs.teslaris.core.model.oaipmh.dspaceinternal.DimField;
import rs.teslaris.core.model.oaipmh.dublincore.DC;
import rs.teslaris.core.model.oaipmh.dublincore.DCType;
import rs.teslaris.core.model.oaipmh.publication.PublicationType;
import rs.teslaris.core.service.interfaces.institution.OrganisationUnitService;
import rs.teslaris.core.util.exceptionhandling.exception.NotFoundException;
import rs.teslaris.core.util.search.StringUtil;
import rs.teslaris.exporter.model.common.BaseExportEntity;
import rs.teslaris.exporter.model.common.ExportDocument;
import rs.teslaris.exporter.model.common.ExportPublicationType;
import rs.teslaris.exporter.util.ExportDataFormat;
import rs.teslaris.exporter.util.ExportHandlersConfigurationLoader;

@Component
public class ExportConverterBase {

    // Inherited classes should include
    // these static methods:
    // static T toCommonExportModel(D modelEntity)
    // static R toOpenaireModel(T commonExportEntity, boolean supportLegacyIdentifiers,
    //      List<String> supportedLanguages, Map<String, String> typeToIdentifierSuffixMapping);
    // static D toDCModel(T commonExportEntity, boolean supportLegacyIdentifiers,
    //      List<String> supportedLanguages, Map<String, String> typeToIdentifierSuffixMapping);
    // static E toETDMSModel(T commonExportEntity, boolean supportLegacyIdentifiers,
    //     List<String> supportedLanguages, Map<String, String> typeToIdentifierSuffixMapping); // where applicable
    // static S toDIMSModel(T commonExportEntity, boolean supportLegacyIdentifiers,
    //     List<String> supportedLanguages, Map<String, String> typeToIdentifierSuffixMapping); // where applicable
    // static M toMARC21Model(T commonExportEntity, boolean supportLegacyIdentifiers,
    //     List<String> supportedLanguages, Map<String, String> typeToIdentifierSuffixMapping); // where applicable

    protected static String repositoryName;

    protected static String baseFrontendUrl;

    protected static List<String> clientLanguages = new ArrayList<>();

    protected static String legacyIdentifierPrefix;

    protected static String identifierPrefix;

    @Autowired
    private Environment environment;


    protected static <T> void addContentToList(List<T> sourceList,
                                               Function<T, String> preprocessingFunction,
                                               Consumer<String> consumer) {
        if (Objects.isNull(sourceList)) {
            return;
        }

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

    protected static <T> void addContentToList(List<T> sourceList,
                                               Function<T, String> contentExtractionFunction,
                                               Function<T, String> identifierExtractionFunction,
                                               BiConsumer<String, String> consumer) {
        if (Objects.isNull(sourceList)) {
            return;
        }

        sourceList.forEach(item -> {
            if (Objects.isNull(item)) {
                return;
            }

            if ((item instanceof String) && ((String) item).isBlank()) {
                return;
            }

            consumer.accept(contentExtractionFunction.apply(item),
                identifierExtractionFunction.apply(item).toLowerCase());
        });
    }

    protected static void setBaseFields(BaseExportEntity baseExportEntity, BaseEntity baseEntity) {
        baseExportEntity.setDatabaseId(baseEntity.getId());
        baseExportEntity.setLastUpdated(baseEntity.getLastModification());
        baseExportEntity.setDeleted(baseEntity.getDeleted());
    }

    protected static void setDocumentDate(String documentDate, Consumer<Date> setter) {
        if (Objects.nonNull(documentDate) && !documentDate.isBlank()) {
            SimpleDateFormat[] formatters = {
                new SimpleDateFormat("yyyy"),
                new SimpleDateFormat("yyyy-MM-dd"),
                new SimpleDateFormat("dd-MM-yyyy"),
                new SimpleDateFormat("dd/MM/yyyy"),
                new SimpleDateFormat("MM/dd/yyyy"),
                new SimpleDateFormat("dd.MM.yyyy"),
                new SimpleDateFormat("dd.MM.yyyy.")
            };

            for (var formatter : formatters) {
                try {
                    setter.accept(formatter.parse(documentDate));
                    break;
                } catch (ParseException e) {
                    // Parsing failed, try the next formatter
                }
            }
        }
    }

    protected static PublicationType inferPublicationCOARType(ExportDocument exportDocument) {
        var scheme = "https://www.openaire.eu/cerif-profile/vocab/COAR_Publication_Types";

        var coarType = switch (exportDocument.getType()) {
            case JOURNAL_PUBLICATION -> exportDocument.getJournalPublicationType().equals(
                JournalPublicationType.RESEARCH_ARTICLE) ?
                "http://purl.org/coar/resource_type/c_2df8fbb1" :
                "http://purl.org/coar/resource_type/c_3e5a";
            case PROCEEDINGS -> "http://purl.org/coar/resource_type/c_f744";
            case PROCEEDINGS_PUBLICATION -> exportDocument.getProceedingsPublicationType().equals(
                ProceedingsPublicationType.REGULAR_FULL_ARTICLE) ?
                "http://purl.org/coar/resource_type/c_5794" :
                "http://purl.org/coar/resource_type/c_c94f";
            case MONOGRAPH, BOOK_SERIES -> "http://purl.org/coar/resource_type/c_2f33"; // book
            case PATENT -> "http://purl.org/coar/resource_type/c_15cd";
            case SOFTWARE -> "http://purl.org/coar/resource_type/c_5ce6";
            case DATASET -> "http://purl.org/coar/resource_type/c_ddb1";
            case JOURNAL -> "http://purl.org/coar/resource_type/c_0640";
            case MONOGRAPH_PUBLICATION -> "http://purl.org/coar/resource_type/c_3248"; // book part
            case THESIS -> (exportDocument.getThesisType().equals(ThesisType.PHD) ||
                exportDocument.getThesisType().equals(ThesisType.PHD_ART_PROJECT)) ?
                "http://purl.org/coar/resource_type/c_db06" :
                "http://purl.org/coar/resource_type/c_46ec";
            case MATERIAL_PRODUCT -> "http://purl.org/coar/resource_type/JBNF-DYAD";
            case GENETIC_MATERIAL -> "http://purl.org/coar/resource_type/S7R1-K5P0";
        };

        return new PublicationType(coarType, null, scheme);
    }

    /**
     * Performs exceptional handling of converted entities for specific export data formats and sets.
     * <p>
     * This method modifies the {@code convertedEntity} directly by clearing and re-adding specific fields
     * using hard-coded values and other workarounds. It is a last resort and should be avoided unless absolutely necessary.
     * <p>
     * Every addition or modification to this method should be thoroughly discussed and reviewed,
     * as this is considered a "budževina" (Serbian term for a workaround or patch that is often
     * suboptimal or hacky in nature).
     * <p>
     * The method takes a handler parameter, which is used to access specific configuration or additional
     * logic that might be needed for exceptional handling based on the export data format or set.
     *
     * @param convertedEntity The entity that has been converted and may need exceptional handling.
     * @param format          The export data format being used (e.g., Dublin Core).
     * @param recordClass     Common export class of the requested resource.
     * @param handler         The handler used to manage export configurations and apply any additional
     *                        logic necessary for handling specific cases, based on the export data format or set.
     */
    public static void performExceptionalHandlingWhereAbsolutelyNecessary(Object convertedEntity,
                                                                          ExportDataFormat format,
                                                                          Class<?> recordClass,
                                                                          ExportHandlersConfigurationLoader.Handler handler) {
        if (format.equals(ExportDataFormat.DUBLIN_CORE) &&
            recordClass.equals(ExportDocument.class)) {
            ((DC) convertedEntity).getRights().clear();
            ((DC) convertedEntity).getRights().add("info:eu-repo/semantics/openAccess");
            ((DC) convertedEntity).getRights()
                .add("http://creativecommons.org/licenses/by-sa/2.0/uk/");
        }
    }

    public static void applyCustomMappings(Object convertedEntity, ExportDataFormat format,
                                           OrganisationUnitService organisationUnitService,
                                           ExportHandlersConfigurationLoader.Handler handler) {
        var sourceInstitutionId = Integer.parseInt(handler.internalInstitutionId());

        if ((format.equals(ExportDataFormat.DUBLIN_CORE) ||
            format.equals(ExportDataFormat.ETD_MS))) {
            var typeKey = ((DC) convertedEntity).getType().getFirst().getValue().toUpperCase();
            var concreteTypeKey = ((DC) convertedEntity).getType().getFirst().getScheme();

            ((DC) convertedEntity).getType().clear();
            ((DC) convertedEntity).getType()
                .addAll(constructDCTypeFields(handler, typeKey, concreteTypeKey));

            if (typeKey.equals(DocumentPublicationType.THESIS.name())) {
                organisationUnitService.findOne(sourceInstitutionId).getName()
                    .forEach(name ->
                        ((DC) convertedEntity).getSource().add(name.getContent()));
            }

            if (Objects.nonNull(handler.dateFormat())) {
                var formatter = DateTimeFormatter.ofPattern(handler.dateFormat());
                ((DC) convertedEntity).getDate().replaceAll(dateString -> {
                    if (Objects.isNull(dateString) || !dateString.contains("-")) {
                        return dateString;
                    }

                    return LocalDate.parse(dateString, DateTimeFormatter.ISO_LOCAL_DATE)
                        .format(formatter);
                });
            }
        } else if (format.equals(ExportDataFormat.DSPACE_INTERNAL_MODEL)) {
            var typeField = ((Dim) convertedEntity).getFields().stream()
                .filter(field -> field.getElement().equals("type")).findFirst();

            if (typeField.isEmpty()) {
                throw new NotFoundException("Fatal error. No type found for document.");
            }

            var typeKey = typeField.get().getValue();
            var concreteTypeKey = typeField.get().getQualifier();

            ((Dim) convertedEntity).getFields().remove(typeField.get());
            constructDCTypeFields(handler, typeKey, concreteTypeKey).forEach(
                dcType -> ((Dim) convertedEntity).getFields().add(
                    new DimField("dc", "type", dcType.getScheme(), dcType.getLang(), null, null,
                        dcType.getValue())));

            if (typeKey.equals(DocumentPublicationType.THESIS.name())) {
                organisationUnitService.findOne(sourceInstitutionId).getName()
                    .forEach(name -> {
                        var field = new DimField();
                        field.setMdschema("dc");
                        field.setElement("source");
                        field.setLanguage(name.getLanguage().getLanguageTag().toLowerCase());
                        field.setValue(name.getContent());
                        ((Dim) convertedEntity).getFields().add(field);
                    });
            }

            if (Objects.nonNull(handler.dateFormat())) {
                var formatter = DateTimeFormatter.ofPattern(handler.dateFormat());
                ((Dim) convertedEntity).getFields().stream()
                    .filter(field -> field.getElement().equals("date")).findFirst()
                    .ifPresent(dateField -> {
                        if (Objects.isNull(dateField.getValue()) ||
                            !dateField.getValue().contains("-")) {
                            return;
                        }

                        dateField.setValue(
                            LocalDate.parse(dateField.getValue(), DateTimeFormatter.ISO_LOCAL_DATE)
                                .format(formatter));
                    });
            }
        }
    }

    private static List<DCType> constructDCTypeFields(
        ExportHandlersConfigurationLoader.Handler handler, String typeKey, String concreteTypeKey) {
        var types = new ArrayList<DCType>();
        if (!handler.typeMappings().containsKey(typeKey)) {
            return types;
        }

        var typeConfiguration = Arrays.asList(handler.typeMappings().get(typeKey).split(";"));

        if (StringUtil.valueExists(concreteTypeKey) && typeConfiguration.stream()
            .anyMatch(entry -> entry.startsWith("(" + concreteTypeKey + ")"))) {
            typeConfiguration = typeConfiguration.stream()
                .filter(entry -> entry.startsWith("(" + concreteTypeKey + ")"))
                .map(entry -> entry.replace("(" + concreteTypeKey + ")", ""))
                .toList();
        } else {
            typeConfiguration = typeConfiguration.stream()
                .filter(entry -> entry.startsWith("(DEFAULT)"))
                .map(entry -> entry.replace("(DEFAULT)", "")).toList();
        }

        typeConfiguration.forEach(type -> {
            var typeTokens = type.split("§");
            var name = typeTokens[0];

            String scheme = null;
            if (typeTokens.length == 2) {
                scheme = typeTokens[1];
            }

            String lang = null;
            if (name.contains("@")) {
                var typeAndLang = typeTokens[0].split("@");
                name = typeAndLang[0];
                lang = typeAndLang[1];
            }

            types.add(new DCType(name, lang, scheme));
        });

        return types;
    }

    protected static String getConcreteEntityPath(ExportPublicationType type) {
        return switch (type) {
            case JOURNAL_PUBLICATION -> "journal-publication";
            case PROCEEDINGS -> "proceedings";
            case PROCEEDINGS_PUBLICATION -> "proceedings-publication";
            case MONOGRAPH -> "monograph";
            case PATENT -> "patent";
            case SOFTWARE -> "software";
            case DATASET -> "dataset";
            case JOURNAL -> "journal";
            case MONOGRAPH_PUBLICATION -> "monograph-publication";
            case THESIS -> "thesis";
            case MATERIAL_PRODUCT -> "material-product";
            case GENETIC_MATERIAL -> "genetic-material";
            case BOOK_SERIES -> "book-series";
        };
    }

    @PostConstruct
    public void init() {
        repositoryName = environment.getProperty("export.repo.name");
        baseFrontendUrl = environment.getProperty("frontend.application.address");
        clientLanguages.clear();
        clientLanguages.addAll(Arrays.asList(
            Objects.requireNonNull(environment.getProperty("client.localization.languages"))
                .split(",")));
        legacyIdentifierPrefix = environment.getProperty("legacy-identifier.prefix");
        identifierPrefix = environment.getProperty("export.internal-identifier.prefix");
    }
}
