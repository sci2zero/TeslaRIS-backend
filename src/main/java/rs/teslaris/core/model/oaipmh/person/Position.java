package rs.teslaris.core.model.oaipmh.person;

import jakarta.xml.bind.annotation.XmlAccessType;
import jakarta.xml.bind.annotation.XmlAccessorType;
import jakarta.xml.bind.annotation.XmlElement;
import jakarta.xml.bind.annotation.XmlRootElement;
import jakarta.xml.bind.annotation.XmlType;
import java.util.Date;
import java.util.List;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.ToString;

@XmlType(name = "TPosition", namespace = "https://www.openaire.eu/cerif-profile/1.1/")
@XmlAccessorType(XmlAccessType.FIELD)
@XmlRootElement(name = "Position")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@ToString
public class Position {

    @XmlElement(name = "Name")
    private String name;

    @XmlElement(name = "StartDate")
    private Date startDate;

    @XmlElement(name = "EndDate")
    private Date endDate;

    @XmlElement(name = "ResearchArea")
    private List<String> researchArea;
}

