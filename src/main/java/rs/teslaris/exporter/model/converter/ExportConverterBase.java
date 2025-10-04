package rs.teslaris.exporter.model.converter;

import jakarta.annotation.PostConstruct;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Date;
import java.util.List;
import java.util.Objects;
import java.util.function.Consumer;
import java.util.function.Function;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.env.Environment;
import org.springframework.stereotype.Component;
import rs.teslaris.core.model.commontypes.BaseEntity;
import rs.teslaris.core.model.oaipmh.dspaceinternal.Dim;
import rs.teslaris.core.model.oaipmh.dublincore.DC;
import rs.teslaris.core.util.exceptionhandling.exception.NotFoundException;
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
    // static R toOpenaireModel(T commonExportEntity);
    // static D toDCModel(T commonExportEntity);
    // static E toETDMSModel(T commonExportEntity); // where applicable
    // static S toDIMSModel(T commonExportEntity); // where applicable
    // static M toMARC21Model(T commonExportEntity); // where applicable

    protected static String repositoryName;

    protected static String baseFrontendUrl;

    protected static List<String> clientLanguages = new ArrayList<>();

    protected static String legacyIdentifierPrefix;

    @Autowired
    private Environment environment;


    protected static <T> void addContentToList(List<T> sourceList,
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

    protected static String inferPublicationCOARType(ExportPublicationType type) {
        return switch (type) {
            case JOURNAL_PUBLICATION -> "http://purl.org/coar/resource_type/c_2df8fbb1";
            case PROCEEDINGS -> "http://purl.org/coar/resource_type/c_f744";
            case PROCEEDINGS_PUBLICATION -> "http://purl.org/coar/resource_type/c_5794";
            case MONOGRAPH -> "http://purl.org/coar/resource_type/c_2f33"; // book
            case PATENT -> "http://purl.org/coar/resource_type/c_15cd";
            case SOFTWARE -> "http://purl.org/coar/resource_type/c_5ce6";
            case DATASET -> "http://purl.org/coar/resource_type/c_ddb1";
            case JOURNAL -> "http://purl.org/coar/resource_type/c_0640";
            case MONOGRAPH_PUBLICATION -> "http://purl.org/coar/resource_type/c_3248"; // book part
            case THESIS -> "http://purl.org/coar/resource_type/c_46ec";
        };
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
                                           Class<?> recordClass,
                                           ExportHandlersConfigurationLoader.Handler handler) {
        if (format.equals(ExportDataFormat.DUBLIN_CORE) &&
            recordClass.equals(ExportDocument.class)) {
            var typeKey = ((DC) convertedEntity).getType().getFirst();

            ((DC) convertedEntity).getType().clear();
            ((DC) convertedEntity).getType().add(handler.typeMappings().get(typeKey));

        } else if (format.equals(ExportDataFormat.DSPACE_INTERNAL_MODEL) &&
            recordClass.equals(ExportDocument.class)) {
            var typeElement = ((Dim) convertedEntity).getFields().stream()
                .filter(field -> field.getElement().equals("type")).findFirst();

            if (typeElement.isEmpty()) {
                throw new NotFoundException("Fatal error. No type found for document.");
            }

            typeElement.get().setValue(handler.typeMappings().get(typeElement.get().getValue()));
        }
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
    }
}
