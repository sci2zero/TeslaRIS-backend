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
import rs.teslaris.core.model.oaipmh.publication.PublicationConvertable;

@XmlAccessorType(XmlAccessType.FIELD)
@XmlType(name = "TThesis", namespace = "http://www.ndltd.org/standards/metadata/etdms/1.1/")
@XmlRootElement(name = "thesis", namespace = "http://www.ndltd.org/standards/metadata/etdms/1.1/")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class ETDMSThesis implements PublicationConvertable {

    @XmlElement(name = "thesisType", namespace = "http://www.ndltd.org/standards/metadata/etdms/1.1/")
    private ThesisType thesisType;


}
