package rs.teslaris.core.importer.model.organisationunit;

import jakarta.xml.bind.annotation.XmlAccessType;
import jakarta.xml.bind.annotation.XmlAccessorType;
import jakarta.xml.bind.annotation.XmlAttribute;
import jakarta.xml.bind.annotation.XmlElement;
import jakarta.xml.bind.annotation.XmlRootElement;
import jakarta.xml.bind.annotation.XmlType;
import java.util.List;
import lombok.AllArgsConstructor;
import lombok.EqualsAndHashCode;
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
@EqualsAndHashCode
public class OrgUnit {

    private String id;

    @XmlAttribute(name = "id")
    private String oldId;

    @XmlElement(name = "Name")
    private List<MultilingualContent> multilingualContent;

    @XmlElement(name = "PartOf")
    private PartOf partOf;

    private List<Integer> importUserId;

    private Boolean loaded;
}
