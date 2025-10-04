package rs.teslaris.core.model.oaipmh.organisationunit;

import jakarta.xml.bind.annotation.XmlAccessType;
import jakarta.xml.bind.annotation.XmlAccessorType;
import jakarta.xml.bind.annotation.XmlAttribute;
import jakarta.xml.bind.annotation.XmlElement;
import jakarta.xml.bind.annotation.XmlRootElement;
import jakarta.xml.bind.annotation.XmlType;
import java.util.List;
import lombok.AllArgsConstructor;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.ToString;
import rs.teslaris.core.model.oaipmh.common.HasOldId;
import rs.teslaris.core.model.oaipmh.common.MultilingualContent;

@XmlType(name = "TOrgUnit", namespace = "https://www.openaire.eu/cerif-profile/1.1/")
@XmlAccessorType(XmlAccessType.FIELD)
@XmlRootElement(name = "OrgUnit")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@ToString
@EqualsAndHashCode(callSuper = false)
public class OrgUnit implements OrgUnitConvertable, HasOldId {

    private String id;

    @XmlAttribute(name = "id")
    private String oldId;

    @XmlElement(name = "Name")
    private List<MultilingualContent> name;

    @XmlElement(name = "PartOf")
    private PartOf partOf;

    private List<Integer> importUserId;

    private Boolean loaded;

    // Additional Migration fields - not part of the OAI-PMH specification

    @XmlElement(name = "Acronym")
    private List<MultilingualContent> acronym;

    @XmlElement(name = "Place")
    private String place;

    @XmlElement(name = "Identifier")
    private String identifier;

    @XmlElement(name = "Keyword")
    private List<MultilingualContent> keywords;

    @XmlElement(name = "ResearchArea")
    private List<String> researchArea;

    @XmlElement(name = "IsInstitution")
    private Boolean isInstitution;
}
