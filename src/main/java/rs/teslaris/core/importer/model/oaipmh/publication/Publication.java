package rs.teslaris.core.importer.model.oaipmh.publication;

import jakarta.xml.bind.annotation.XmlAccessType;
import jakarta.xml.bind.annotation.XmlAccessorType;
import jakarta.xml.bind.annotation.XmlAttribute;
import jakarta.xml.bind.annotation.XmlElement;
import jakarta.xml.bind.annotation.XmlElementWrapper;
import jakarta.xml.bind.annotation.XmlRootElement;
import jakarta.xml.bind.annotation.XmlType;
import java.util.Date;
import java.util.List;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.ToString;
import rs.teslaris.core.importer.model.oaipmh.common.MultilingualContent;
import rs.teslaris.core.importer.model.oaipmh.common.PersonAttributes;

@XmlType(name = "TPublication", namespace = "https://www.openaire.eu/cerif-profile/1.1/")
@XmlAccessorType(XmlAccessType.FIELD)
@XmlRootElement(name = "Publication")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@ToString
public class Publication {

    @XmlElement(name = "Keyword")
    List<String> keywords;
    private String id;

    @XmlAttribute(name = "id")
    private String oldId;

    @XmlElement(name = "Type", namespace = "https://www.openaire.eu/cerif-profile/vocab/COAR_Publication_Types")
    private String type;

    @XmlElement(name = "Language")
    private String language;

    @XmlElement(name = "Title")
    private List<MultilingualContent> title;

    @XmlElement(name = "Subtitle")
    private List<MultilingualContent> subtitle;

    @XmlElement(name = "PublicationDate")
    private Date publicationDate;

    @XmlElement(name = "OutputFrom")
    private OutputFrom outputFrom;

    @XmlElement(name = "Number")
    private String number;

    @XmlElement(name = "Volume")
    private String volume;

    @XmlElement(name = "Issue")
    private String issue;

    @XmlElement(name = "Edition")
    private String edition;

    @XmlElement(name = "StartPage")
    private String startPage;

    @XmlElement(name = "EndPage")
    private String endPage;

    @XmlElement(name = "URL")
    private List<String> url;

    @XmlElement(name = "DOI")
    private String doi;

    @XmlElement(name = "SCP-Number")
    private String scpNumber;

    @XmlElement(name = "ISSN")
    private String issn;

    @XmlElement(name = "ISBN")
    private String isbn;

    @XmlElement(name = "Abstract")
    private String _abstract;

    @XmlElement(name = "Access")
    private String access;

    @XmlElementWrapper(name = "Authors")
    @XmlElement(name = "Author")
    private List<PersonAttributes> authors;

    @XmlElementWrapper(name = "Editors")
    @XmlElement(name = "Editor")
    private List<PersonAttributes> editors;

    @XmlElementWrapper(name = "Publishers")
    @XmlElement(name = "Publisher")
    private List<Publisher> publishers;

    @XmlElement(name = "PublishedIn")
    private PublishedIn publishedIn;

    @XmlElement(name = "PartOf")
    private PartOf partOf;

    private List<Integer> importUserId;

    private Boolean loaded;
}
