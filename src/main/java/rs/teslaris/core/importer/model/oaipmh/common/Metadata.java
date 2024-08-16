package rs.teslaris.core.importer.model.oaipmh.common;

import jakarta.xml.bind.annotation.XmlAccessType;
import jakarta.xml.bind.annotation.XmlAccessorType;
import jakarta.xml.bind.annotation.XmlElement;
import jakarta.xml.bind.annotation.XmlElements;
import jakarta.xml.bind.annotation.XmlRootElement;
import jakarta.xml.bind.annotation.XmlType;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.ToString;
import rs.teslaris.core.importer.model.oaipmh.dublincore.DC;
import rs.teslaris.core.importer.model.oaipmh.event.EventConvertable;
import rs.teslaris.core.importer.model.oaipmh.event.Event;
import rs.teslaris.core.importer.model.oaipmh.organisationunit.OrgUnitConvertable;
import rs.teslaris.core.importer.model.oaipmh.organisationunit.OrgUnit;
import rs.teslaris.core.importer.model.oaipmh.patent.PatentConvertable;
import rs.teslaris.core.importer.model.oaipmh.patent.Patent;
import rs.teslaris.core.importer.model.oaipmh.person.PersonConvertable;
import rs.teslaris.core.importer.model.oaipmh.person.Person;
import rs.teslaris.core.importer.model.oaipmh.product.ProductConvertable;
import rs.teslaris.core.importer.model.oaipmh.product.Product;
import rs.teslaris.core.importer.model.oaipmh.publication.PublicationConvertable;
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

    @XmlElements({
        @XmlElement(name = "OrgUnit", type = OrgUnit.class, namespace = "https://www.openaire.eu/cerif-profile/1.1/"),
        @XmlElement(name = "dc", type = DC.class, namespace = "http://www.openarchives.org/OAI/2.0/oai_dc/")
    })
    private OrgUnitConvertable orgUnit;

    @XmlElements({
        @XmlElement(name = "Person", type = Person.class, namespace = "https://www.openaire.eu/cerif-profile/1.1/"),
        @XmlElement(name = "dc", type = DC.class, namespace = "http://www.openarchives.org/OAI/2.0/oai_dc/")
    })
    private PersonConvertable person;

    @XmlElements({
        @XmlElement(name = "Event", type = Event.class, namespace = "https://www.openaire.eu/cerif-profile/1.1/"),
        @XmlElement(name = "dc", type = DC.class, namespace = "http://www.openarchives.org/OAI/2.0/oai_dc/")
    })
    private EventConvertable event;

    @XmlElements({
        @XmlElement(name = "Publication", type = Publication.class, namespace = "https://www.openaire.eu/cerif-profile/1.1/"),
        @XmlElement(name = "dc", type = DC.class, namespace = "http://www.openarchives.org/OAI/2.0/oai_dc/")
    })
    private PublicationConvertable publication;

    @XmlElements({
        @XmlElement(name = "Patent", type = Patent.class, namespace = "https://www.openaire.eu/cerif-profile/1.1/"),
        @XmlElement(name = "dc", type = DC.class, namespace = "http://www.openarchives.org/OAI/2.0/oai_dc/")
    })
    private PatentConvertable patent;

    @XmlElements({
        @XmlElement(name = "Product", type = Product.class, namespace = "https://www.openaire.eu/cerif-profile/1.1/"),
        @XmlElement(name = "dc", type = DC.class, namespace = "http://www.openarchives.org/OAI/2.0/oai_dc/")
    })
    private ProductConvertable product;
}
