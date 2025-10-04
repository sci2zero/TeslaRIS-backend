package rs.teslaris.core.model.oaipmh.publication;

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

@XmlType(name = "TBookSeries", namespace = "https://www.openaire.eu/cerif-profile/1.1/")
@XmlAccessorType(XmlAccessType.FIELD)
@XmlRootElement(name = "BookSeries")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@ToString
public class BookSeries {

    @XmlElement(name = "Title")
    private String title;

    @XmlElement(name = "ISSN")
    private String issn;
}
