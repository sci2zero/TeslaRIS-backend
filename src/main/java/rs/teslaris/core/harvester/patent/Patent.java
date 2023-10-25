package rs.teslaris.core.harvester.patent;

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
import rs.teslaris.core.harvester.common.PersonAttributes;
import rs.teslaris.core.harvester.publication.Author;

@XmlType(name = "TPatent", namespace = "https://www.openaire.eu/cerif-profile/1.1/")
@XmlAccessorType(XmlAccessType.FIELD)
@XmlRootElement(name = "Patent")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@ToString
public class Patent {

    @XmlAttribute(name = "id")
    private String id;


    @XmlElement(name = "Type", namespace = "https://www.openaire.eu/cerif-profile/vocab/COAR_Patent_Types")
    private String type;


    @XmlElement(name = "Title")
    private MultilingualContent title;


    @XmlElement(name = "ApprovalDate")
    private Date approvalDate;


    @XmlElement(name = "PatentNumber")
    private String patentNumber;


    @XmlElementWrapper(name = "Inventors")
    @XmlElement(name = "Inventor")
    private List<PersonAttributes> inventor;
}
