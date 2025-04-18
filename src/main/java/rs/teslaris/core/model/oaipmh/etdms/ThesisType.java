package rs.teslaris.core.model.oaipmh.etdms;

import jakarta.xml.bind.annotation.XmlAccessType;
import jakarta.xml.bind.annotation.XmlAccessorType;
import jakarta.xml.bind.annotation.XmlElement;
import jakarta.xml.bind.annotation.XmlRootElement;
import jakarta.xml.bind.annotation.XmlType;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import rs.teslaris.core.model.oaipmh.dublincore.DC;

@XmlAccessorType(XmlAccessType.FIELD)
@XmlType(name = "TThesisType", namespace = "http://www.ndltd.org/standards/metadata/etdms/1.1/")
@XmlRootElement(name = "thesisType", namespace = "http://www.ndltd.org/standards/metadata/etdms/1.1/")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class ThesisType extends DC {

    @XmlElement(name = "degree", namespace = "http://www.ndltd.org/standards/metadata/etdms/1.1/")
    private Degree degree;
}
