package rs.teslaris.core.importer.model.oaipmh.dspaceinternal;

import jakarta.xml.bind.annotation.XmlAccessType;
import jakarta.xml.bind.annotation.XmlAccessorType;
import jakarta.xml.bind.annotation.XmlAttribute;
import jakarta.xml.bind.annotation.XmlValue;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.ToString;

@XmlAccessorType(XmlAccessType.FIELD)
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@ToString
public class DimField {

    @XmlAttribute(name = "mdschema")
    private String mdschema;

    @XmlAttribute(name = "element")
    private String element;

    @XmlAttribute(name = "qualifier")
    private String qualifier;

    @XmlAttribute(name = "lang")
    private String language;

    @XmlAttribute(name = "authority")
    private String authority;

    @XmlAttribute(name = "confidence")
    private String confidence;

    @XmlValue
    private String value;
}
