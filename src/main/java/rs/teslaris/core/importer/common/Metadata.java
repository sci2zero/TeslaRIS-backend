package rs.teslaris.core.importer.common;

import javax.xml.bind.annotation.XmlAccessType;
import javax.xml.bind.annotation.XmlAccessorType;
import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlRootElement;
import javax.xml.bind.annotation.XmlType;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.ToString;
import rs.teslaris.core.importer.event.Event;
import rs.teslaris.core.importer.organisationunit.OrgUnit;
import rs.teslaris.core.importer.patent.Patent;
import rs.teslaris.core.importer.person.Person;
import rs.teslaris.core.importer.product.Product;
import rs.teslaris.core.importer.publication.Publication;

@XmlType(name = "TMetadata")
@XmlAccessorType(XmlAccessType.FIELD)
@XmlRootElement(name = "metadata")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@ToString
public class Metadata {

    @XmlElement(name = "OrgUnit", namespace = "https://www.openaire.eu/cerif-profile/1.1/")
    private OrgUnit orgUnit;

    @XmlElement(name = "Person", namespace = "https://www.openaire.eu/cerif-profile/1.1/")
    private Person person;

    @XmlElement(name = "Event", namespace = "https://www.openaire.eu/cerif-profile/1.1/")
    private Event event;

    @XmlElement(name = "Publication", namespace = "https://www.openaire.eu/cerif-profile/1.1/")
    private Publication publication;

    @XmlElement(name = "Patent", namespace = "https://www.openaire.eu/cerif-profile/1.1/")
    private Patent patent;

    @XmlElement(name = "Product", namespace = "https://www.openaire.eu/cerif-profile/1.1/")
    private Product product;
}
