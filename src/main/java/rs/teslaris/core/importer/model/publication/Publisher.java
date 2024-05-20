package rs.teslaris.core.importer.model.publication;

import jakarta.xml.bind.annotation.XmlAccessType;
import jakarta.xml.bind.annotation.XmlAccessorType;
import jakarta.xml.bind.annotation.XmlElement;
import jakarta.xml.bind.annotation.XmlRootElement;
import jakarta.xml.bind.annotation.XmlType;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.ToString;
import rs.teslaris.core.importer.model.organisationunit.OrgUnit;

@XmlType(name = "TPublisher", namespace = "https://www.openaire.eu/cerif-profile/1.1/")
@XmlAccessorType(XmlAccessType.FIELD)
@XmlRootElement(name = "Publisher")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@ToString
public class Publisher {

    @XmlElement(name = "DisplayName")
    private String displayName;

    @XmlElement(name = "OrgUnit", namespace = "https://www.openaire.eu/cerif-profile/1.1/")
    private OrgUnit orgUnit;
}
