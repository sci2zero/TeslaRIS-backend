package rs.teslaris.core.importer.model.organisationunit;

import java.util.List;
import javax.xml.bind.annotation.XmlAccessType;
import javax.xml.bind.annotation.XmlAccessorType;
import javax.xml.bind.annotation.XmlAttribute;
import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlRootElement;
import javax.xml.bind.annotation.XmlType;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.ToString;
import rs.teslaris.core.importer.model.common.MultilingualContent;

@XmlType(name = "TOrgUnit", namespace = "https://www.openaire.eu/cerif-profile/1.1/")
@XmlAccessorType(XmlAccessType.FIELD)
@XmlRootElement(name = "OrgUnit")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@ToString
public class OrgUnit {

    @XmlAttribute(name = "id")
    private String id;

    @XmlElement(name = "Name")
    private List<MultilingualContent> multilingualContent;

    @XmlElement(name = "PartOf")
    private PartOf partOf;

    private Integer importUserId;
}
