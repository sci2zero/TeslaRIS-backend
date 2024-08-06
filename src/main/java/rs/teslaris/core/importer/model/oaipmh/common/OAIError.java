package rs.teslaris.core.importer.model.oaipmh.common;


import jakarta.xml.bind.annotation.XmlAccessType;
import jakarta.xml.bind.annotation.XmlAccessorType;
import jakarta.xml.bind.annotation.XmlAttribute;
import jakarta.xml.bind.annotation.XmlElement;
import jakarta.xml.bind.annotation.XmlRootElement;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@XmlAccessorType(XmlAccessType.FIELD)
@XmlRootElement(name = "error")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class OAIError {

    @XmlAttribute(name = "code")
    private String code;

    @XmlElement(name = "error")
    private String message;
}
