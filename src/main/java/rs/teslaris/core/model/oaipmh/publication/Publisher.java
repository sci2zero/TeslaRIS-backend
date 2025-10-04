package rs.teslaris.core.model.oaipmh.publication;

import jakarta.xml.bind.annotation.XmlAccessType;
import jakarta.xml.bind.annotation.XmlAccessorType;
import jakarta.xml.bind.annotation.XmlElement;
import jakarta.xml.bind.annotation.XmlRootElement;
import jakarta.xml.bind.annotation.XmlType;
import java.util.List;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.ToString;
import rs.teslaris.core.model.oaipmh.common.MultilingualContent;

@XmlType(name = "TPublisher", namespace = "https://www.openaire.eu/cerif-profile/1.1/")
@XmlAccessorType(XmlAccessType.FIELD)
@XmlRootElement(name = "Publisher")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@ToString
public class Publisher {

    @XmlElement(name = "Name")
    private List<MultilingualContent> name;

    @XmlElement(name = "Place")
    private List<MultilingualContent> place;

    @XmlElement(name = "State")
    private List<MultilingualContent> state;
}
