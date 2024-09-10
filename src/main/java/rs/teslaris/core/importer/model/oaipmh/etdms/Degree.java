package rs.teslaris.core.importer.model.oaipmh.etdms;

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

@XmlAccessorType(XmlAccessType.FIELD)
@XmlType(name = "TDegree", namespace = "http://www.ndltd.org/standards/metadata/etdms/1.1/")
@XmlRootElement(name = "degree", namespace = "http://www.ndltd.org/standards/metadata/etdms/1.1/")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class Degree {

    @XmlElement(name = "name", namespace = "http://purl.org/dc/elements/1.1/")
    private List<String> name = new ArrayList<>();

    @XmlElement(name = "levelType", namespace = "http://www.ndltd.org/standards/metadata/etdms/1.1/")
    private LevelType level;
    //
    @XmlElement(name = "discipline", namespace = "http://purl.org/dc/elements/1.1/")
    private List<String> discipline = new ArrayList<>();

    @XmlElement(name = "grantor", namespace = "http://purl.org/dc/elements/1.1/")
    private List<String> grantor = new ArrayList<>();
}
