package rs.teslaris.core.importer.model.common;

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

@XmlType(name = "TRecord")
@XmlAccessorType(XmlAccessType.FIELD)
@XmlRootElement(name = "Record")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@ToString
public class Record {

    @XmlElement(name = "header")
    private Header header;

    @XmlElement(name = "metadata")
    private Metadata metadata;
}
