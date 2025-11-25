package rs.teslaris.core.model.oaipmh.dublincore;

import com.fasterxml.jackson.annotation.JsonInclude;
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
import rs.teslaris.core.model.oaipmh.event.EventConvertable;
import rs.teslaris.core.model.oaipmh.organisationunit.OrgUnitConvertable;
import rs.teslaris.core.model.oaipmh.patent.PatentConvertable;
import rs.teslaris.core.model.oaipmh.person.PersonConvertable;
import rs.teslaris.core.model.oaipmh.product.ProductConvertable;
import rs.teslaris.core.model.oaipmh.publication.PublicationConvertable;

@XmlType(name = "TOai_dc", namespace = "http://www.openarchives.org/OAI/2.0/oai_dc/")
@XmlAccessorType(XmlAccessType.FIELD)
@XmlRootElement(name = "dc", namespace = "http://www.openarchives.org/OAI/2.0/oai_dc/")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@ToString
@JsonInclude(JsonInclude.Include.NON_EMPTY)
public class DC implements PublicationConvertable, EventConvertable, OrgUnitConvertable,
    PersonConvertable,
    PatentConvertable, ProductConvertable {

    @XmlElement(name = "title", namespace = "http://purl.org/dc/elements/1.1/")
    private List<DCMultilingualContent> title = new ArrayList<>();

    @XmlElement(name = "creator", namespace = "http://purl.org/dc/elements/1.1/")
    private List<String> creator = new ArrayList<>();

    @XmlElement(name = "subject", namespace = "http://purl.org/dc/elements/1.1/")
    private List<DCMultilingualContent> subject = new ArrayList<>();

    @XmlElement(name = "description", namespace = "http://purl.org/dc/elements/1.1/")
    private List<DCMultilingualContent> description = new ArrayList<>();

    @XmlElement(name = "publisher", namespace = "http://purl.org/dc/elements/1.1/")
    private List<DCMultilingualContent> publisher = new ArrayList<>();

    @XmlElement(name = "contributor", namespace = "http://purl.org/dc/elements/1.1/")
    private List<Contributor> contributor = new ArrayList<>();

    @XmlElement(name = "date", namespace = "http://purl.org/dc/elements/1.1/")
    private List<String> date = new ArrayList<>();

    @XmlElement(name = "type", namespace = "http://purl.org/dc/elements/1.1/")
    private List<DCType> type = new ArrayList<>();

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
