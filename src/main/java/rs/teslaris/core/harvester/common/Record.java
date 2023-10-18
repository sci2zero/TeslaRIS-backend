package rs.teslaris.core.harvester.common;

import javax.xml.bind.annotation.XmlAccessType;
import javax.xml.bind.annotation.XmlAccessorType;
import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlRootElement;
import javax.xml.bind.annotation.XmlType;
import lombok.Getter;
import lombok.Setter;
import lombok.ToString;

@XmlType(name = "TRecord")
@XmlAccessorType(XmlAccessType.FIELD)
@XmlRootElement(name = "Record")
@Getter
@Setter
@ToString
public class Record {

    @XmlElement(name = "header")
    private Header header;

    @XmlElement(name = "metadata")
    private Metadata metadata;


    public Record() {
    }

    public Record(Header header, Metadata metadata) {
        this.header = header;
        this.metadata = metadata;
    }
}
