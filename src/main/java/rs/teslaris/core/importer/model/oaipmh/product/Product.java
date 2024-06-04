package rs.teslaris.core.importer.model.oaipmh.product;

import jakarta.xml.bind.annotation.XmlAccessType;
import jakarta.xml.bind.annotation.XmlAccessorType;
import jakarta.xml.bind.annotation.XmlAttribute;
import jakarta.xml.bind.annotation.XmlElement;
import jakarta.xml.bind.annotation.XmlElementWrapper;
import jakarta.xml.bind.annotation.XmlRootElement;
import jakarta.xml.bind.annotation.XmlType;
import java.util.List;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.ToString;
import rs.teslaris.core.importer.model.oaipmh.common.MultilingualContent;
import rs.teslaris.core.importer.model.oaipmh.common.PersonAttributes;

@XmlType(name = "TProduct", namespace = "https://www.openaire.eu/cerif-profile/1.1/")
@XmlAccessorType(XmlAccessType.FIELD)
@XmlRootElement(name = "Product")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@ToString
public class Product {

    @XmlElement(name = "Keyword")
    List<String> keywords;
    private String id;
    @XmlAttribute(name = "id")
    private String oldId;

    @XmlElement(name = "Type", namespace = "https://www.openaire.eu/cerif-profile/vocab/COAR_Product_Types")
    private String type;

    @XmlElement(name = "Language")
    private String language;

    @XmlElement(name = "Name")
    private List<MultilingualContent> name;

    @XmlElement(name = "URL")
    private List<String> url;

    @XmlElement(name = "Description")
    private List<MultilingualContent> description;

    @XmlElement(name = "Access")
    private String access;

    @XmlElementWrapper(name = "Creators")
    @XmlElement(name = "Creator")
    private List<PersonAttributes> creators;

    private List<Integer> importUserId;

    private Boolean loaded;
}
