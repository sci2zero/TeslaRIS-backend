package rs.teslaris.core.model.person;

import java.util.Set;
import javax.persistence.*;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import rs.teslaris.core.model.commontypes.BaseEntity;
import rs.teslaris.core.model.commontypes.Country;
import rs.teslaris.core.model.commontypes.MultiLingualContent;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Embeddable
public class PostalAddress {

    @ManyToOne(fetch = FetchType.EAGER)
    @JoinColumn(name = "country_id")
    Country country;

    @OneToMany(fetch = FetchType.EAGER, cascade = CascadeType.ALL)
    Set<MultiLingualContent> streetAndNumber;

    @OneToMany(fetch = FetchType.EAGER, cascade = CascadeType.ALL)
    Set<MultiLingualContent> city;
}
