package rs.teslaris.core.converter.document;

import java.util.Objects;
import java.util.Set;
import org.apache.logging.log4j.util.Strings;
import org.jbibtex.BibTeXEntry;
import org.jbibtex.Key;
import org.jbibtex.StringValue;
import org.springframework.transaction.annotation.Transactional;
import rs.teslaris.core.converter.commontypes.MultilingualContentConverter;
import rs.teslaris.core.converter.person.PersonContributionConverter;
import rs.teslaris.core.dto.document.DocumentDTO;
import rs.teslaris.core.model.commontypes.MultiLingualContent;
import rs.teslaris.core.model.document.Dataset;
import rs.teslaris.core.model.document.Document;
import rs.teslaris.core.model.document.JournalPublication;
import rs.teslaris.core.model.document.Monograph;
import rs.teslaris.core.model.document.MonographPublication;
import rs.teslaris.core.model.document.Patent;
import rs.teslaris.core.model.document.Proceedings;
import rs.teslaris.core.model.document.ProceedingsPublication;
import rs.teslaris.core.model.document.Software;
import rs.teslaris.core.model.document.Thesis;
import rs.teslaris.core.util.language.LanguageAbbreviations;

@Transactional
public class DocumentPublicationConverter {

    public static DocumentDTO toDTO(Document document) {
        var dto = new DocumentDTO();
        setCommonFields(document, dto);
        return dto;
    }

    protected static void setCommonFields(Document publication, DocumentDTO publicationDTO) {
        publicationDTO.setId(publication.getId());
        publicationDTO.setTitle(
            MultilingualContentConverter.getMultilingualContentDTO(publication.getTitle()));
        publicationDTO.setSubTitle(
            MultilingualContentConverter.getMultilingualContentDTO(publication.getSubTitle()));
        publicationDTO.setDescription(
            MultilingualContentConverter.getMultilingualContentDTO(publication.getDescription()));
        publicationDTO.setKeywords(
            MultilingualContentConverter.getMultilingualContentDTO(publication.getKeywords()));

        publicationDTO.setContributions(
            PersonContributionConverter.documentContributionToDTO(publication.getContributors()));

        publicationDTO.setUris(publication.getUris());
        publicationDTO.setDocumentDate(publication.getDocumentDate());
        publicationDTO.setDoi(publication.getDoi());
        publicationDTO.setScopusId(publication.getScopusId());
        publicationDTO.setOpenAlexId(publication.getOpenAlexId());
        publicationDTO.setWebOfScienceId(publication.getWebOfScienceId());

        publicationDTO.setIsMetadataValid(publication.getIsMetadataValid());
        publicationDTO.setAreFilesValid(publication.getAreFilesValid());
        publicationDTO.setIsArchived(publication.getIsArchived());

        if (Objects.nonNull(publication.getEvent())) {
            publicationDTO.setEventId(publication.getEvent().getId());
        }

        publication.getFileItems().forEach(fileItem -> {
            publicationDTO.getFileItems().add(DocumentFileConverter.toDTO(fileItem));
        });

        publication.getProofs().forEach(proof -> {
            publicationDTO.getProofs().add(DocumentFileConverter.toDTO(proof));
        });
    }

    public static void setCommonFields(Document publication, BibTeXEntry entry) {
        setMCBibTexField(publication.getTitle(), entry, BibTeXEntry.KEY_TITLE);
        setMCBibTexField(publication.getSubTitle(), entry, new Key("subtitle"));
        setMCBibTexField(publication.getDescription(), entry, new Key("abstract"));
        setMCBibTexField(publication.getKeywords(), entry, new Key("keywords"));

        if (Objects.nonNull(publication.getContributors()) &&
            !publication.getContributors().isEmpty()) {
            PersonContributionConverter.toBibTexAuthors(publication.getContributors(), entry);
        }

        if (Objects.nonNull(publication.getDocumentDate()) &&
            !publication.getDocumentDate().isBlank()) {
            entry.addField(BibTeXEntry.KEY_YEAR,
                new StringValue(publication.getDocumentDate().split("-")[0],
                    StringValue.Style.BRACED));
        }

        if (valueExists(publication.getDoi())) {
            entry.addField(BibTeXEntry.KEY_DOI,
                new StringValue(publication.getDoi(), StringValue.Style.BRACED));
        }

        if (valueExists(publication.getScopusId())) {
            entry.addField(new Key("scopusId"),
                new StringValue(publication.getScopusId(), StringValue.Style.BRACED));
        }

        if (valueExists(publication.getOpenAlexId())) {
            entry.addField(new Key("openAlexId"),
                new StringValue(publication.getOpenAlexId(), StringValue.Style.BRACED));
        }

        if (valueExists(publication.getWebOfScienceId())) {
            entry.addField(new Key("wosId"),
                new StringValue(publication.getWebOfScienceId(), StringValue.Style.BRACED));
        }

        if (Objects.nonNull(publication.getEvent())) {
            setMCBibTexField(publication.getEvent().getName(), entry, new Key("event"));
        }

        if (Objects.nonNull(publication.getUris()) && !publication.getUris().isEmpty()) {
            entry.addField(BibTeXEntry.KEY_URL,
                new StringValue(Strings.join(publication.getUris(), ' '),
                    StringValue.Style.BRACED));
        }
    }

    public static void setCommonTaggedFields(Document publication, StringBuilder sb,
                                             boolean refMan) {
        setMCTaggedField(publication.getTitle(), sb, refMan ? "TI" : "%T");
        setMCTaggedField(publication.getSubTitle(), sb, refMan ? "ST" : "%Z");
        setMCTaggedField(publication.getDescription(), sb, refMan ? "AB" : "%X");
        setMCTaggedField(publication.getKeywords(), sb, refMan ? "KW" : "%K");

        if (Objects.nonNull(publication.getContributors())) {
            PersonContributionConverter.toTaggedAuthors(publication.getContributors(), sb, refMan);
        }

        if (valueExists(publication.getDoi())) {
            sb.append(refMan ? "DO  - " : "%R ").append(publication.getDoi()).append("\n");
        }

        if (valueExists(publication.getDocumentDate())) {
            sb.append(refMan ? "PY  - " : "%D ").append(publication.getDocumentDate().split("-")[0])
                .append("\n");
        }

        if (Objects.nonNull(publication.getUris()) && !publication.getUris().isEmpty()) {
            for (String uri : publication.getUris()) {
                sb.append(refMan ? "UR  - " : "%U ").append(uri).append("\n");
            }
        }
    }

    protected static void setMCBibTexField(Set<MultiLingualContent> content, BibTeXEntry entry,
                                           Key fieldKey) {
        if (Objects.isNull(content) || content.isEmpty()) {
            return;
        }

        var defaultLanguage = LanguageAbbreviations.ENGLISH;
        var defaultContent =
            MultilingualContentConverter.getLocalizedContent(content, defaultLanguage);
        if (defaultContent.isEmpty()) {
            var firstEntry = content.stream().findFirst();
            if (firstEntry.isEmpty()) {
                return; // should never happen
            }

            defaultContent = firstEntry.get().getContent();
            defaultLanguage = firstEntry.get().getLanguage().getLanguageTag();
        }

        entry.addField(fieldKey, new StringValue(defaultContent, StringValue.Style.BRACED));

        var finalDefaultLanguage = defaultLanguage;
        content.forEach(mc -> {
            if (Objects.nonNull(mc.getLanguage()) && Objects.nonNull(mc.getContent()) &&
                !mc.getLanguage().getLanguageTag().equalsIgnoreCase(finalDefaultLanguage)) {
                var langKey =
                    new Key(fieldKey.getValue() + "[" + mc.getLanguage().getLanguageTag() + "]");
                entry.addField(langKey, new StringValue(mc.getContent(), StringValue.Style.BRACED));
            }
        });
    }


    protected static void setMCTaggedField(Set<MultiLingualContent> content, StringBuilder sb,
                                           String fieldTag) {
        if (Objects.isNull(content) || content.isEmpty()) {
            return;
        }

        var defaultLanguage = LanguageAbbreviations.ENGLISH;
        var defaultContent =
            MultilingualContentConverter.getLocalizedContent(content, defaultLanguage);

        if (defaultContent.isEmpty()) {
            var firstEntry = content.stream().findFirst();
            if (firstEntry.isEmpty()) {
                return; // should never happen
            }
            defaultContent = firstEntry.get().getContent();
        }

        sb.append(fieldTag).append(fieldTag.startsWith("%") ? " " : "  - ").append(defaultContent)
            .append("\n");

        if (!fieldTag.equals("TI")) {
            return;
        }

        content.forEach(mc -> {
            if (Objects.nonNull(mc.getLanguage()) && Objects.nonNull(mc.getContent()) &&
                !mc.getLanguage().getLanguageTag().equalsIgnoreCase(defaultLanguage)) {
                sb.append("TT  - ").append(mc.getContent()).append("\n");
            }
        });
    }

    protected static boolean valueExists(String value) {
        return Objects.nonNull(value) && !value.isBlank();
    }

    public static BibTeXEntry toBibTeXEntry(Document document) {
        return switch (document) {
            case Thesis thesis -> ThesisConverter.toBibTexEntry(thesis);
            case Dataset dataset -> DatasetConverter.toBibTexEntry(dataset);
            case Software software -> SoftwareConverter.toBibTexEntry(software);
            case Patent patent -> PatentConverter.toBibTexEntry(patent);
            case JournalPublication journalPublication ->
                JournalPublicationConverter.toBibTexEntry(journalPublication);
            case Monograph monograph -> MonographConverter.toBibTexEntry(monograph);
            case MonographPublication monographPublication ->
                MonographPublicationConverter.toBibTexEntry(monographPublication);
            case Proceedings proceedings -> ProceedingsConverter.toBibTexEntry(proceedings);
            case ProceedingsPublication proceedingsPublication ->
                ProceedingsPublicationConverter.toBibTexEntry(proceedingsPublication);
            default -> throw new IllegalArgumentException(
                "Unsupported document type: " + document.getClass().getSimpleName());
        };
    }

    public static String toTaggedFormat(Document document, boolean refMan) {
        return switch (document) {
            case Thesis thesis -> ThesisConverter.toTaggedFormat(thesis, refMan);
            case Dataset dataset -> DatasetConverter.toTaggedFormat(dataset, refMan);
            case Software software -> SoftwareConverter.toTaggedFormat(software, refMan);
            case Patent patent -> PatentConverter.toTaggedFormat(patent, refMan);
            case JournalPublication journalPublication ->
                JournalPublicationConverter.toTaggedFormat(journalPublication, refMan);
            case Monograph monograph -> MonographConverter.toTaggedFormat(monograph, refMan);
            case MonographPublication monographPublication ->
                MonographPublicationConverter.toTaggedFormat(monographPublication, refMan);
            case Proceedings proceedings ->
                ProceedingsConverter.toTaggedFormat(proceedings, refMan);
            case ProceedingsPublication proceedingsPublication ->
                ProceedingsPublicationConverter.toTaggedFormat(proceedingsPublication, refMan);
            default -> throw new IllegalArgumentException(
                "Unsupported document type: " + document.getClass().getSimpleName());
        };
    }
}
