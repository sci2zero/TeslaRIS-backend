package rs.teslaris.core.importer.model.oaipmh.common;

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

@XmlAccessorType(XmlAccessType.FIELD)
@XmlType(name = "TListMetadataFormats", propOrder = {
    "metadataFormat"
})
@XmlRootElement(name = "ListMetadataFormats")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@ToString
public class ListMetadataFormats {

    @XmlElement(required = true)
    private List<MetadataFormat> metadataFormat;

}
