package rs.teslaris.core.model.oaipmh.common;

import jakarta.xml.bind.annotation.XmlAccessType;
import jakarta.xml.bind.annotation.XmlAccessorType;
import jakarta.xml.bind.annotation.XmlElement;
import jakarta.xml.bind.annotation.XmlRootElement;
import jakarta.xml.bind.annotation.XmlType;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.ToString;

@XmlType(name = "TGetRecord")
@XmlAccessorType(XmlAccessType.FIELD)
@XmlRootElement(name = "GetRecord")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@ToString
public class GetRecord {

    @XmlElement(name = "record")
    private Record record;
}
