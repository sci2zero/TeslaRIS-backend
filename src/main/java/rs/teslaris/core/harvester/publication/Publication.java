package rs.teslaris.core.harvester.publication;

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

    @XmlAttribute(name = "id")
    private String id;

    @XmlElement(name = "Type", namespace = "https://www.openaire.eu/cerif-profile/vocab/COAR_Publication_Types")
    private String type;

    @XmlElement(name = "Language")
    private String language;

    @XmlElement(name = "Title")
    private MultilingualContent title;

    @XmlElement(name = "PublicationDate")
    private String publicationDate;

    @XmlElement(name = "OutputFrom")
    private OutputFrom outputFrom;

    @XmlElement(name = "URL")
    private MultilingualContent url;

    @XmlElement(name = "Volume")
    private String volume;

    @XmlElement(name = "Issue")
    private String issue;

    @XmlElement(name = "StartPage")
    private String startPage;

    @XmlElement(name = "EndPage")
    private String endPage;

    @XmlElementWrapper(name = "Authors")
    @XmlElement(name = "Author")
    private List<Author> authors;
}
