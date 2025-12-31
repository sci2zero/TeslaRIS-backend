package rs.teslaris.exporter.model.converter;

import com.google.common.base.Functions;
import java.util.ArrayList;
import java.util.List;
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

public class ExportPatentConverter extends ExportConverterBase {

    public static Patent toOpenaireModel(ExportDocument exportDocument,
                                         boolean supportLegacyIdentifiers,
                                         List<String> supportedLanguages) {
        var openairePatent = new Patent();

        if (supportLegacyIdentifiers && Objects.nonNull(exportDocument.getOldIds()) &&
            !exportDocument.getOldIds().isEmpty()) {
            openairePatent.setOldId("Patents/" + legacyIdentifierPrefix +
                exportDocument.getOldIds().stream().findFirst().get());
        } else {
            openairePatent.setOldId(
                "Patents/" + IdentifierUtil.identifierPrefix + exportDocument.getDatabaseId());
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
                               List<String> supportedLanguages) {
        var dcPatent = new DC();
        dcPatent.getType().add(new DCType("model", null, null));
        dcPatent.getSource().add(repositoryName);

        if (supportLegacyIdentifiers && Objects.nonNull(exportDocument.getOldIds()) &&
            !exportDocument.getOldIds().isEmpty()) {
            dcPatent.getIdentifier().add(legacyIdentifierPrefix +
                exportDocument.getOldIds().stream().findFirst().get());
        }

        dcPatent.getIdentifier().add(identifierPrefix + exportDocument.getDatabaseId());

        CollectionOperations.getIntersection(clientLanguages, supportedLanguages).forEach(lang -> {
            dcPatent.getIdentifier()
                .add(baseFrontendUrl + lang + "/scientific-results/patent/" +
                    exportDocument.getDatabaseId());
        });

        if (StringUtil.valueExists(exportDocument.getDoi())) {
            dcPatent.getIdentifier().add("doi:" + exportDocument.getDoi());
        }

        if (StringUtil.valueExists(exportDocument.getScopus())) {
            dcPatent.getIdentifier().add("scopus:" + exportDocument.getScopus());
        }

        if (StringUtil.valueExists(exportDocument.getOpenAlex())) {
            dcPatent.getIdentifier().add("openalex:" + exportDocument.getOpenAlex());
        }

        if (StringUtil.valueExists(exportDocument.getWebOfScience())) {
            dcPatent.getIdentifier().add("wos:" + exportDocument.getWebOfScience());
        }

        addContentToList(
            exportDocument.getTitle(),
            ExportMultilingualContent::getContent,
            ExportMultilingualContent::getLanguageTag,
            (content, languageTag) -> dcPatent.getTitle()
                .add(new DCMultilingualContent(content, languageTag))
        );

        addContentToList(
            exportDocument.getAuthors(),
            ExportContribution::getDisplayName,
            content -> dcPatent.getCreator().add(content)
        );

        addContentToList(
            exportDocument.getDescription(),
            ExportMultilingualContent::getContent,
            ExportMultilingualContent::getLanguageTag,
            (content, languageTag) -> dcPatent.getDescription()
                .add(new DCMultilingualContent(content, languageTag))
        );

        addContentToList(
            exportDocument.getKeywords(),
            ExportMultilingualContent::getContent,
            ExportMultilingualContent::getLanguageTag,
            (content, languageTag) -> dcPatent.getSubject()
                .add(new DCMultilingualContent(content.replace("\n", "; "), languageTag))
        );

        addContentToList(
            exportDocument.getFileFormats(),
            Functions.identity(),
            content -> dcPatent.getFormat().add(content)
        );

        dcPatent.getRights().add(
            (Objects.nonNull(exportDocument.getOpenAccess()) && exportDocument.getOpenAccess()) ?
                "info:eu-repo/semantics/openAccess" :
                "info:eu-repo/semantics/metadataOnlyAccess");
        dcPatent.getRights().add("http://creativecommons.org/publicdomain/zero/1.0/");

        return dcPatent;
    }
}
