package rs.teslaris.core.harvester.publication;

import java.util.Date;
import java.util.List;
import javax.xml.bind.annotation.XmlAccessType;
import javax.xml.bind.annotation.XmlAccessorType;
import javax.xml.bind.annotation.XmlAttribute;
import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlElementWrapper;
import javax.xml.bind.annotation.XmlRootElement;
import javax.xml.bind.annotation.XmlType;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.ToString;
import rs.teslaris.core.harvester.common.MultilingualContent;

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

    @XmlAttribute(name = "id")
    private String id;

    @XmlElement(name = "Type", namespace = "https://www.openaire.eu/cerif-profile/vocab/COAR_Publication_Types")
    private String type;

    @XmlElement(name = "Language")
    private String language;

    @XmlElement(name = "Title")
    private MultilingualContent title;

    @XmlElement(name = "Subtitle")
    private MultilingualContent subtitle;

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
    private MultilingualContent url;

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
    private List<Author> authors;

    @XmlElementWrapper(name = "Editors")
    @XmlElement(name = "Editor")
    private List<Editor> editors;

    @XmlElementWrapper(name = "Publishers")
    @XmlElement(name = "Publisher")
    private List<Publisher> publishers;

    @XmlElementWrapper(name = "PublishedIn")
    @XmlElement(name = "Publication", namespace = "https://www.openaire.eu/cerif-profile/1.1/")
    private Publication publication;

    @XmlElement(name = "PartOf")
    private PartOf partOf;
}
