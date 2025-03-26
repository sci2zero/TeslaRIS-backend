package rs.teslaris.core.model.oaipmh.common;

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

@XmlAccessorType(XmlAccessType.FIELD)
@XmlType(name = "TListSets", propOrder = {
    "set"
})
@XmlRootElement(name = "ListSets")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@ToString
public class ListSets {

    @XmlElement(required = true)
    private List<Set> set = new ArrayList<>();

}
