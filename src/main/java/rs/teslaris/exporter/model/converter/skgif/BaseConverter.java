package rs.teslaris.exporter.model.converter.skgif;

import java.util.List;
import rs.teslaris.core.model.skgif.common.SKGIFIdentifier;
import rs.teslaris.core.util.language.LanguageAbbreviations;
import rs.teslaris.core.util.search.StringUtil;
import rs.teslaris.exporter.model.common.ExportDocument;
import rs.teslaris.exporter.model.common.ExportMultilingualContent;

public class BaseConverter {

    protected static String extractTitleFromMC(List<ExportMultilingualContent> exportMC) {
        return exportMC.stream()
            .filter(
                mc -> mc.getLanguageTag().equalsIgnoreCase(LanguageAbbreviations.ENGLISH))
            .findFirst()
            .orElse(
                exportMC.stream().findFirst().orElse(
                    new ExportMultilingualContent(LanguageAbbreviations.ENGLISH, "",
                        1))).getContent();
    }

    protected static void populateIdentifiers(List<SKGIFIdentifier> identifiers,
                                              ExportDocument document) {
        if (StringUtil.valueExists(document.getDoi())) {
            identifiers.add(new SKGIFIdentifier("doi", document.getDoi()));
        }

        if (StringUtil.valueExists(document.getScopus())) {
            identifiers.add(new SKGIFIdentifier("url",
                "https://www.scopus.com/pages/publications/" + document.getScopus()));
        }

        if (StringUtil.valueExists(document.getOpenAlex())) {
            identifiers.add(new SKGIFIdentifier("openalex", document.getOpenAlex()));
        }

        if (StringUtil.valueExists(document.getEIssn())) {
            identifiers.add(new SKGIFIdentifier("eissn", document.getEIssn()));
        }

        if (StringUtil.valueExists(document.getPrintIssn())) {
            identifiers.add(new SKGIFIdentifier("issn", document.getPrintIssn()));
        }

        if (StringUtil.valueExists(document.getEIsbn())) {
            identifiers.add(new SKGIFIdentifier("isbn", document.getEIsbn()));
        } else if (StringUtil.valueExists(document.getPrintIsbn())) {
            identifiers.add(new SKGIFIdentifier("isbn", document.getPrintIsbn()));
        }
    }
}
