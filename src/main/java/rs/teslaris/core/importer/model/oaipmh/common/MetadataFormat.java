package rs.teslaris.core.importer.model.oaipmh.common;

import jakarta.xml.bind.annotation.XmlAccessType;
import jakarta.xml.bind.annotation.XmlAccessorType;
import jakarta.xml.bind.annotation.XmlElement;
import jakarta.xml.bind.annotation.XmlType;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.ToString;

@XmlAccessorType(XmlAccessType.FIELD)
@XmlType(name = "TMetadataFormat", propOrder = {
    "metadataPrefix",
    "schema",
    "metadataNamespace"
})
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@ToString
public class MetadataFormat {

    @XmlElement(required = true)
    private String metadataPrefix;

    @XmlElement(required = true)
    private String schema;

    @XmlElement(required = true)
    private String metadataNamespace;

}
