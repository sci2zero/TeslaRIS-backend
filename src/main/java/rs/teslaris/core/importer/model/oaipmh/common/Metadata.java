package rs.teslaris.core.importer.model.oaipmh.common;

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
import rs.teslaris.core.importer.model.oaipmh.event.Event;
import rs.teslaris.core.importer.model.oaipmh.organisationunit.OrgUnit;
import rs.teslaris.core.importer.model.oaipmh.patent.Patent;
import rs.teslaris.core.importer.model.oaipmh.person.Person;
import rs.teslaris.core.importer.model.oaipmh.product.Product;
import rs.teslaris.core.importer.model.oaipmh.publication.Publication;

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
