package rs.teslaris.core.importer.model.oaipmh.common;

import jakarta.xml.bind.annotation.XmlAccessType;
import jakarta.xml.bind.annotation.XmlAccessorType;
import jakarta.xml.bind.annotation.XmlElement;
import jakarta.xml.bind.annotation.XmlRootElement;
import jakarta.xml.bind.annotation.XmlType;
import java.util.ArrayList;
import java.util.List;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.ToString;
import rs.teslaris.core.importer.model.oaipmh.event.AbstractEvent;
import rs.teslaris.core.importer.model.oaipmh.organisationunit.AbstractOrgUnit;
import rs.teslaris.core.importer.model.oaipmh.patent.AbstractPatent;
import rs.teslaris.core.importer.model.oaipmh.person.AbstractPerson;
import rs.teslaris.core.importer.model.oaipmh.product.AbstractProduct;
import rs.teslaris.core.importer.model.oaipmh.publication.AbstractPublication;

@XmlType(name = "oai_dcType", namespace = "http://www.openarchives.org/OAI/2.0/oai_dc/")
@XmlAccessorType(XmlAccessType.FIELD)
@XmlRootElement(name = "dc", namespace = "http://www.openarchives.org/OAI/2.0/oai_dc/")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@ToString
public class DC implements AbstractPublication, AbstractEvent, AbstractOrgUnit, AbstractPerson,
    AbstractPatent, AbstractProduct {

    @XmlElement(name = "title", namespace = "http://purl.org/dc/elements/1.1/")
    private List<String> title = new ArrayList<>();

    @XmlElement(name = "creator", namespace = "http://purl.org/dc/elements/1.1/")
    private List<String> creator = new ArrayList<>();

    @XmlElement(name = "subject", namespace = "http://purl.org/dc/elements/1.1/")
    private List<String> subject = new ArrayList<>();

    @XmlElement(name = "description", namespace = "http://purl.org/dc/elements/1.1/")
    private List<String> description = new ArrayList<>();

    @XmlElement(name = "publisher", namespace = "http://purl.org/dc/elements/1.1/")
    private List<String> publisher = new ArrayList<>();

    @XmlElement(name = "contributor", namespace = "http://purl.org/dc/elements/1.1/")
    private List<String> contributor = new ArrayList<>();

    @XmlElement(name = "date", namespace = "http://purl.org/dc/elements/1.1/")
    private List<String> date = new ArrayList<>();

    @XmlElement(name = "type", namespace = "http://purl.org/dc/elements/1.1/")
    private List<String> type = new ArrayList<>();

    @XmlElement(name = "format", namespace = "http://purl.org/dc/elements/1.1/")
    private List<String> format = new ArrayList<>();

    @XmlElement(name = "identifier", namespace = "http://purl.org/dc/elements/1.1/")
    private List<String> identifier = new ArrayList<>();

    @XmlElement(name = "source", namespace = "http://purl.org/dc/elements/1.1/")
    private List<String> source = new ArrayList<>();

    @XmlElement(name = "language", namespace = "http://purl.org/dc/elements/1.1/")
    private List<String> language = new ArrayList<>();

    @XmlElement(name = "relation", namespace = "http://purl.org/dc/elements/1.1/")
    private List<String> relation = new ArrayList<>();

    @XmlElement(name = "coverage", namespace = "http://purl.org/dc/elements/1.1/")
    private List<String> coverage = new ArrayList<>();

    @XmlElement(name = "rights", namespace = "http://purl.org/dc/elements/1.1/")
    private List<String> rights = new ArrayList<>();
}
