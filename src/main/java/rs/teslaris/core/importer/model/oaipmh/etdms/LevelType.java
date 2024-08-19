package rs.teslaris.core.importer.model.oaipmh.etdms;

import jakarta.xml.bind.annotation.XmlAccessType;
import jakarta.xml.bind.annotation.XmlAccessorType;
import jakarta.xml.bind.annotation.XmlRootElement;
import jakarta.xml.bind.annotation.XmlType;
import jakarta.xml.bind.annotation.XmlValue;
import java.util.Arrays;
import java.util.List;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@XmlAccessorType(XmlAccessType.FIELD)
@XmlType(name = "TLevelType", namespace = "http://www.ndltd.org/standards/metadata/etdms/1.1/")
@XmlRootElement(name = "levelType", namespace = "http://www.ndltd.org/standards/metadata/etdms/1.1/")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class LevelType {

    // The allowed values, matching the pattern [012]
    private static final List<String> allowedValues = Arrays.asList("0", "1", "2");
    @XmlValue
    private String value;

    public void setValue(String value) {
        if (!allowedValues.contains(value)) {
            throw new IllegalArgumentException("Invalid value for LevelType: " + value);
        }
        this.value = value;
    }
}
