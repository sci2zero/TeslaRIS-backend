package rs.teslaris.core.model.oaipmh.common;

import jakarta.xml.bind.annotation.XmlAccessType;
import jakarta.xml.bind.annotation.XmlAccessorType;
import jakarta.xml.bind.annotation.XmlElement;
import jakarta.xml.bind.annotation.XmlType;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.ToString;

@XmlAccessorType(XmlAccessType.FIELD)
@XmlType(name = "TSet", propOrder = {
    "setSpec",
    "setName"
})
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@ToString
public class Set {

    @XmlElement(required = true)
    private String setSpec;

    @XmlElement(required = true)
    private String setName;

}
