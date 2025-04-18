package rs.teslaris.core.model.oaipmh.marc21;

import jakarta.xml.bind.annotation.XmlAccessType;
import jakarta.xml.bind.annotation.XmlAccessorType;
import jakarta.xml.bind.annotation.XmlAttribute;
import jakarta.xml.bind.annotation.XmlElement;
import java.util.ArrayList;
import java.util.List;
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
public class DataField {

    @XmlAttribute(name = "tag")
    private String tag;

    @XmlAttribute(name = "ind1")
    private String ind1;

    @XmlAttribute(name = "ind2")
    private String ind2;

    @XmlElement(name = "subfield", namespace = "http://www.loc.gov/MARC21/slim")
    private List<SubField> subFields = new ArrayList<>();
}
