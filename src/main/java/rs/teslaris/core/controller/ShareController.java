package rs.teslaris.core.controller;

import java.util.Set;
import lombok.RequiredArgsConstructor;
import org.apache.commons.text.StringEscapeUtils;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import rs.teslaris.core.indexmodel.DocumentPublicationType;
import rs.teslaris.core.model.commontypes.MultiLingualContent;
import rs.teslaris.core.service.interfaces.document.DocumentPublicationService;

@RestController
@RequestMapping("/api/share")
@RequiredArgsConstructor
public class ShareController {

    private final DocumentPublicationService documentPublicationService;

    @Value("${frontend.application.address}")
    private String frontendUrl;


    @GetMapping("/document/{documentType}/{id}/{lang}")
    public ResponseEntity<String> shareDocumentPage(@PathVariable Integer id,
                                                    @PathVariable
                                                    DocumentPublicationType documentType,
                                                    @PathVariable String lang) {
        var document = documentPublicationService.findOne(id);

        var title = StringEscapeUtils.escapeHtml4(getContentForLanguage(document.getTitle(), lang));
        var description =
            StringEscapeUtils.escapeHtml4(getContentForLanguage(document.getDescription(), lang));
        var url = StringEscapeUtils.escapeHtml4(
            frontendUrl + StringEscapeUtils.escapeHtml4(lang) + "/scientific-results/" +
                getPageNameFromDocumentType(documentType) + "/" + id);

        var html = """
            <!DOCTYPE html>
            <html lang="en">
            <head>
                <meta charset="UTF-8">
                <meta name="viewport" content="width=device-width, initial-scale=1.0">
                <meta property="og:title" content="%s" />
                <meta property="og:description" content="%s" />
                <meta property="og:url" content="%s" />
                <meta property="og:type" content="website" />
                <meta http-equiv="refresh" content="0; url=%s">
                <title>%s</title>
            </head>
            <body>
                <script defer>
                    document.addEventListener("DOMContentLoaded", function () {
                        window.location.href = "%s";
                    });
                </script>
            </body>
            </html>
            """.formatted(title, description, url, url, title, url);

        return ResponseEntity.ok()
            .contentType(MediaType.TEXT_HTML)
            .body(html);
    }

    private String getContentForLanguage(Set<MultiLingualContent> mcSet, String lang) {
        for (var mc : mcSet) {
            if (mc.getLanguage().getLanguageTag().equalsIgnoreCase(lang)) {
                return mc.getContent();
            }
        }

        var defaultContent = mcSet.stream().findFirst();

        return defaultContent.isPresent() ? defaultContent.get().getContent() : "";
    }

    private String getPageNameFromDocumentType(DocumentPublicationType documentPublicationType) {
        return switch (documentPublicationType) {
            case JOURNAL_PUBLICATION -> "journal-publication";
            case PROCEEDINGS -> "proceedings";
            case PROCEEDINGS_PUBLICATION -> "proceedings-publication";
            case MONOGRAPH -> "monograph";
            case PATENT -> "patent";
            case SOFTWARE -> "software";
            case DATASET -> "dataset";
            case MONOGRAPH_PUBLICATION -> "monograph-publication";
            case THESIS -> "thesis";
        };
    }
}
