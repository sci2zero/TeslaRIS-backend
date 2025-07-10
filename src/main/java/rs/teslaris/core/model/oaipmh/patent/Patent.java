package rs.teslaris.core.model.oaipmh.patent;

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
import rs.teslaris.core.model.oaipmh.common.HasOldId;
import rs.teslaris.core.model.oaipmh.common.MultilingualContent;
import rs.teslaris.core.model.oaipmh.common.PersonAttributes;

@XmlType(name = "TPatent", namespace = "https://www.openaire.eu/cerif-profile/1.1/")
@XmlAccessorType(XmlAccessType.FIELD)
@XmlRootElement(name = "Patent")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@ToString
public class Patent implements PatentConvertable, HasOldId {

    @XmlElement(name = "Keyword")
    List<String> keywords;
    private String id;
    @XmlAttribute(name = "id")
    private String oldId;

    @XmlElement(name = "Type", namespace = "https://www.openaire.eu/cerif-profile/vocab/COAR_Patent_Types")
    private String type;

    @XmlElement(name = "Title")
    private List<MultilingualContent> title;

    @XmlElement(name = "ApprovalDate")
    private Date approvalDate;

    @XmlElement(name = "PatentNumber")
    private String patentNumber;

    @XmlElement(name = "Abstract")
    private String _abstract;

    @XmlElement(name = "Access")
    private String access;

    @XmlElementWrapper(name = "Inventors")
    @XmlElement(name = "Inventor")
    private List<PersonAttributes> inventor;

    private List<Integer> importUserId;

    private Boolean loaded;
}
