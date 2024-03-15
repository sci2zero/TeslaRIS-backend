package rs.teslaris.core.importer.model.product;

import java.util.List;
import javax.xml.bind.annotation.XmlAccessType;
import javax.xml.bind.annotation.XmlAccessorType;
import javax.xml.bind.annotation.XmlAttribute;
import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlElementWrapper;
import javax.xml.bind.annotation.XmlRootElement;
import javax.xml.bind.annotation.XmlType;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.ToString;
import rs.teslaris.core.importer.model.common.MultilingualContent;
import rs.teslaris.core.importer.model.common.PersonAttributes;

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

    @XmlAttribute(name = "id")
    private String id;

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
