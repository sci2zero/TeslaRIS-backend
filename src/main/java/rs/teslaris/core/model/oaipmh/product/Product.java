package rs.teslaris.core.model.oaipmh.product;

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
import rs.teslaris.core.model.oaipmh.publication.PublicationType;
import rs.teslaris.core.model.oaipmh.publication.Publisher;

@XmlType(name = "TProduct", namespace = "https://www.openaire.eu/cerif-profile/1.1/")
@XmlAccessorType(XmlAccessType.FIELD)
@XmlRootElement(name = "Product")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@ToString
public class Product implements ProductConvertable, HasOldId {

    private String id;

    @XmlElement(name = "Keyword")
    private List<MultilingualContent> keywords;

    @XmlAttribute(name = "id")
    private String oldId;

    @XmlElement(name = "Type", namespace = "https://www.openaire.eu/cerif-profile/vocab/COAR_Product_Types")
    private List<PublicationType> type;

    @XmlElement(name = "Language")
    private String language;

    @XmlElement(name = "Name")
    private List<MultilingualContent> name;

    @XmlElement(name = "URL")
    private List<String> url;

    @XmlElement(name = "Description")
    private List<MultilingualContent> description;

    @XmlElementWrapper(name = "Creators")
    @XmlElement(name = "Creator")
    private List<PersonAttributes> creators;

    @XmlElement(name = "Access", namespace = "http://purl.org/coar/access_right")
    private String access;

    private List<Integer> importUserId;

    private Boolean loaded;

    // Additional Migration fields - not part of the OAI-PMH specification

    @XmlElement(name = "PublicationDate")
    private Date publicationDate;

    @XmlElement(name = "DOI")
    private String doi;

    @XmlElement(name = "InternalNumber")
    private String internalNumber;

    @XmlElement(name = "Publisher")
    private Publisher publisher;
}
