package rs.teslaris.core.model.oaipmh.marc21;

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

@XmlType(name = "TMarc21", namespace = "http://www.loc.gov/MARC21/slim")
@XmlAccessorType(XmlAccessType.FIELD)
@XmlRootElement(name = "record", namespace = "http://www.loc.gov/MARC21/slim")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@ToString
public class Marc21 {

    @XmlElement(name = "leader", namespace = "http://www.loc.gov/MARC21/slim")
    private String leader;

    @XmlElement(name = "controlfield", namespace = "http://www.loc.gov/MARC21/slim")
    private List<ControlField> controlFields = new ArrayList<>();

    @XmlElement(name = "datafield", namespace = "http://www.loc.gov/MARC21/slim")
    private List<DataField> dataFields = new ArrayList<>();

}
