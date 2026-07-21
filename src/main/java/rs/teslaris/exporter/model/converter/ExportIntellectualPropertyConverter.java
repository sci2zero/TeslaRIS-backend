package rs.teslaris.exporter.model.converter;

import com.google.common.base.Functions;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.function.Function;
import rs.teslaris.core.model.oaipmh.common.PersonAttributes;
import rs.teslaris.core.model.oaipmh.dublincore.DC;
import rs.teslaris.core.model.oaipmh.dublincore.DCMultilingualContent;
import rs.teslaris.core.model.oaipmh.dublincore.DCType;
import rs.teslaris.core.model.oaipmh.patent.Patent;
import rs.teslaris.core.util.persistence.IdentifierUtil;
import rs.teslaris.core.util.search.CollectionOperations;
import rs.teslaris.core.util.search.StringUtil;
import rs.teslaris.exporter.model.common.ExportContribution;
import rs.teslaris.exporter.model.common.ExportDocument;
import rs.teslaris.exporter.model.common.ExportMultilingualContent;

public class ExportIntellectualPropertyConverter extends ExportConverterBase {

    public static Patent toOpenaireModel(ExportDocument exportDocument,
                                         boolean supportLegacyIdentifiers,
                                         List<String> supportedLanguages,
                                         Map<String, String> typeToIdentifierSuffixMapping) {
        var openairePatent = new Patent();

        var identifierTypeSuffix =
            typeToIdentifierSuffixMapping.getOrDefault(exportDocument.getType().name(), "");
        if (supportLegacyIdentifiers && Objects.nonNull(exportDocument.getOldIds()) &&
            !exportDocument.getOldIds().isEmpty()) {
            openairePatent.setOldId("Intellectual-property/" + legacyIdentifierPrefix +
                exportDocument.getOldIds().stream().findFirst().get());
        } else {
            openairePatent.setOldId(
                "Intellectual-property/" + IdentifierUtil.identifierPrefix +
                    exportDocument.getDatabaseId() +
                    identifierTypeSuffix);
        }

        openairePatent.setTitle(
            ExportMultilingualContentConverter.toOpenaireModel(exportDocument.getTitle()));

        openairePatent.setType(
            new ArrayList<>(List.of(inferPublicationCOARType(exportDocument))));

        ExportMultilingualContentConverter.setFieldFromPriorityContent(
            exportDocument.getDescription().stream(),
            Function.identity(),
            openairePatent::set_abstract
        );

        setDocumentDate(exportDocument.getDocumentDate(), openairePatent::setApprovalDate);
        openairePatent.setPatentNumber(exportDocument.getNumber());
        openairePatent.setAccess(
            (Objects.nonNull(exportDocument.getOpenAccess()) && exportDocument.getOpenAccess()) ?
                "http://purl.org/coar/access_right/c_abf2" :
                "http://purl.org/coar/access_right/c_14cb");

        openairePatent.setInventor(new ArrayList<>());
        exportDocument.getAuthors()
            .forEach(contribution -> {
                var personAttributes = new PersonAttributes();
                personAttributes.setDisplayName(contribution.getDisplayName());

                if (Objects.nonNull(contribution.getPerson())) {
                    personAttributes.setPerson(
                        ExportPersonConverter.toOpenaireModel(contribution.getPerson(),
                            supportLegacyIdentifiers, supportedLanguages));
                }

                openairePatent.getInventor().add(personAttributes);
            });

        return openairePatent;
    }

    public static DC toDCModel(ExportDocument exportDocument, boolean supportLegacyIdentifiers,
                               List<String> supportedLanguages,
                               Map<String, String> typeToIdentifierSuffixMapping) {
        var dcIntellectualProperty = new DC();
        dcIntellectualProperty.getType().add(new DCType("model", null, null));
        dcIntellectualProperty.getSource().add(repositoryName);

        if (supportLegacyIdentifiers && Objects.nonNull(exportDocument.getOldIds()) &&
            !exportDocument.getOldIds().isEmpty()) {
            dcIntellectualProperty.getIdentifier().add(legacyIdentifierPrefix +
                exportDocument.getOldIds().stream().findFirst().get());
        }

        var identifierTypeSuffix =
            typeToIdentifierSuffixMapping.getOrDefault(exportDocument.getType().name(), "");
        dcIntellectualProperty.getIdentifier()
            .add(identifierPrefix + exportDocument.getDatabaseId() + identifierTypeSuffix);

        CollectionOperations.getIntersection(clientLanguages, supportedLanguages).forEach(lang -> {
            dcIntellectualProperty.getIdentifier()
                .add(baseFrontendUrl + lang + "/scientific-results/intellectual-property/" +
                    exportDocument.getDatabaseId());
        });

        if (StringUtil.valueExists(exportDocument.getDoi())) {
            dcIntellectualProperty.getIdentifier().add("doi:" + exportDocument.getDoi());
        }

        if (StringUtil.valueExists(exportDocument.getScopus())) {
            dcIntellectualProperty.getIdentifier().add("scopus:" + exportDocument.getScopus());
        }

        if (StringUtil.valueExists(exportDocument.getOpenAlex())) {
            dcIntellectualProperty.getIdentifier().add("openalex:" + exportDocument.getOpenAlex());
        }

        if (StringUtil.valueExists(exportDocument.getWebOfScience())) {
            dcIntellectualProperty.getIdentifier().add("wos:" + exportDocument.getWebOfScience());
        }

        addContentToList(
            exportDocument.getTitle(),
            ExportMultilingualContent::getContent,
            ExportMultilingualContent::getLanguageTag,
            (content, languageTag) -> dcIntellectualProperty.getTitle()
                .add(new DCMultilingualContent(content, languageTag))
        );

        addContentToList(
            exportDocument.getAuthors(),
            ExportContribution::getDisplayName,
            content -> dcIntellectualProperty.getCreator().add(content)
        );

        addContentToList(
            exportDocument.getDescription(),
            ExportMultilingualContent::getContent,
            ExportMultilingualContent::getLanguageTag,
            (content, languageTag) -> dcIntellectualProperty.getDescription()
                .add(new DCMultilingualContent(content, languageTag))
        );

        addContentToList(
            exportDocument.getKeywords(),
            ExportMultilingualContent::getContent,
            ExportMultilingualContent::getLanguageTag,
            (content, languageTag) -> dcIntellectualProperty.getSubject()
                .add(new DCMultilingualContent(content.replace("\n", "; "), languageTag))
        );

        addContentToList(
            exportDocument.getFileFormats(),
            Functions.identity(),
            content -> dcIntellectualProperty.getFormat().add(content)
        );

        dcIntellectualProperty.getRights().add(
            (Objects.nonNull(exportDocument.getOpenAccess()) && exportDocument.getOpenAccess()) ?
                "info:eu-repo/semantics/openAccess" :
                "info:eu-repo/semantics/metadataOnlyAccess");
        dcIntellectualProperty.getRights().add("http://creativecommons.org/publicdomain/zero/1.0/");

        return dcIntellectualProperty;
    }
}
